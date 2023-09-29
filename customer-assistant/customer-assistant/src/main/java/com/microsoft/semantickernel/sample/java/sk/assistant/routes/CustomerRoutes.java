package com.microsoft.semantickernel.sample.java.sk.assistant.routes;

import com.azure.core.annotation.BodyParam;
import com.microsoft.semantickernel.chatcompletion.ChatCompletion;
import com.microsoft.semantickernel.chatcompletion.ChatHistory;
import com.microsoft.semantickernel.orchestration.SKContext;
import com.microsoft.semantickernel.orchestration.SKFunction;
import com.microsoft.semantickernel.sample.java.sk.assistant.SemanticKernelProvider;
import com.microsoft.semantickernel.sample.java.sk.assistant.controllers.CustomerController;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Customer;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.CustomerNotFoundException;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Customers;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Notes;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.QueryResult;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.StatementType;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseBroadcaster;
import jakarta.ws.rs.sse.SseEventSink;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Endpoints for interacting with customers
 */
@Path("/api/customer")
@Produces(MediaType.APPLICATION_JSON)
//Uncomment to enable authentication
//@Authenticated
public class CustomerRoutes {
    private final CustomerController customerController;
    private final Customers customers;
    private final SemanticKernelProvider provider;

    private static final Map<String, Map<String, SSEChatSession>> chatSessions = new ConcurrentHashMap<>();

    @Inject
    CustomerRoutes(
            CustomerController customerController,
            Customers customers,
            SemanticKernelProvider provider
    ) {
        this.customerController = customerController;
        this.customers = customers;
        this.provider = provider;
    }

    @GET
    @Path("/info/{customerId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Customer> queryPlayerFact(
            @PathParam("customerId")
            String customerId
    ) throws CustomerNotFoundException {
        return Uni.createFrom().item(customers.getCustomer(customerId));
    }

    @GET
    @Path("names")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Map<String, String>> getNames() {
        Map<String, String> ids = customers
                .getCustomers()
                .stream()
                .map(customer -> Map.of(customer.getUid(), customer.getName()))
                .reduce(new HashMap<>(), (accumulated, newData) -> {
                    accumulated.putAll(newData);
                    return accumulated;
                });

        return Uni.createFrom().item(ids);
    }

    private SSEChatSession getChat(String customerId, String chatId) {
        Map<String, SSEChatSession> chatMap = chatSessions.computeIfAbsent(customerId, k -> new HashMap<>());
        return chatMap.get(chatId);
    }

    private void addChatSession(String customerId, String chatId, SSEChatSession chatSession) {
        Map<String, SSEChatSession> chatMap = chatSessions.computeIfAbsent(customerId, k -> new HashMap<>());
        chatMap.put(chatId, chatSession);
    }

    @POST
    @Path("/sse/message/{customerId}")
    public void getServerSentEvents(
            @PathParam("customerId") String customerId,
            @HeaderParam("chatId") String chatId,
            @BodyParam("body") String statement
    ) throws CustomerNotFoundException {
        SSEChatSession chatSession = getChat(customerId, chatId);
        if (chatSession == null) {
            throw new NotFoundException("Session not found");
        }

        Customer customer = customers.getCustomer(customerId);
        if (customer == null) {
            throw new NotFoundException("Customer not found");
        }

        String cleaned = statement.replaceAll("^[^a-zA-Z0-9]+", "").trim();

        if (chatSession.getChat().getLastMessage().isPresent() && chatSession.getChat().getMessages().size() > 2) {
            chatSession.getChat().addMessage(ChatHistory.AuthorRoles.User, statement);

            chatSession
                    .getCompletion()
                    .generateMessageStream(chatSession.getChat(), null)
                    .reduce("", (accumulation, message) -> {
                        chatSession.getBroadcaster().broadcast(chatSession.getSse().newEvent(message));
                        return accumulation + message;
                    })
                    .subscribe(result -> {
                        chatSession.getChat().addMessage(ChatHistory.AuthorRoles.Assistant, result);
                    });
        } else {
            executeQuery(cleaned, customer, chatSession);
        }
    }

    private void executeQuery(String query, Customer customer, SSEChatSession session) {
        classifyStatement(query)
                .flatMap(type -> {
                    Mono<QueryResult> stream;

                    return executeRequest(query, customer, query, type);
                })
                .subscribe(message -> {
                    broadcastSSEResponse(query, session, message);
                });
    }

    private static void broadcastSSEResponse(String query, SSEChatSession session, QueryResult message) {
        session.getChat().addMessage(ChatHistory.AuthorRoles.User, query);
        session.getChat().addMessage(ChatHistory.AuthorRoles.Assistant, message.getResult());

        session.getBroadcaster().broadcast(session.getSse().newEventBuilder()
                .mediaType(MediaType.TEXT_PLAIN_TYPE)
                .data(message.getResult())
                .build());

        if (message.getDocuments() != null && !message.getDocuments().isEmpty()) {
            session
                    .getBroadcaster()
                    .broadcast(session.getSse().newEventBuilder()
                            .mediaType(MediaType.TEXT_HTML_TYPE)
                            .name("documents")
                            .data(message.getDocuments())
                            .build());
        }
    }

    @GET
    @Path("/sse/connect/{customerId}")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Response getServerSentEvents(
            @PathParam("customerId") String customerId,
            @QueryParam("chatId") String chatId,
            @Context SseEventSink eventSink,
            @Context Sse sse) {

        SSEChatSession chatSession = getChat(customerId, chatId);

        SseBroadcaster broadcaster;
        if (chatSession != null) {
            broadcaster = chatSession.getBroadcaster();
            broadcaster.register(eventSink);
            return Response.ok().build();
        }

        ChatCompletion<ChatHistory> completion = provider.getChatCompletion();

        ChatHistory chat = completion.createNewChat("You are a bot that helps a customer support agent manage customers");

        chatSession = new SSEChatSession(
                sse.newBroadcaster(),
                completion,
                chat,
                sse
        );

        addChatSession(customerId, chatId, chatSession);
        chatSession.getBroadcaster().register(eventSink);
        return Response.ok().build();
    }


    @POST
    @Path("perform/{customerId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<QueryResult> perform(
            @PathParam("customerId")
            String customerId,

            @BodyParam("statement")
            String statement
    ) throws CustomerNotFoundException {
        Customer customer = customers.getCustomer(customerId);
        if (customer == null) {
            return Uni.createFrom().failure(new NotFoundException("Customer not found"));
        }

        String cleaned = statement.replaceAll("^[^a-zA-Z0-9]+", "").trim();

        CompletableFuture<QueryResult> future = classifyStatement(cleaned)
                .flatMap(type -> executeRequest(statement, customer, cleaned, type))
                .toFuture();

        return Uni.createFrom().future(future);
    }

    @POST
    @Path("setNotes/{customerId}")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> setNotes(
            @PathParam("customerId")
            String customerId,

            @BodyParam("notes")
            String notes
    ) throws CustomerNotFoundException {
        Customer customer = customers.getCustomer(customerId);
        if (customer == null) {
            return Uni.createFrom().failure(new NotFoundException("Customer not found"));
        }

        customer.setNotes(new Notes(Arrays.asList(notes.split("\\n"))));

        CompletableFuture<String> future = customerController.saveCustomerFacts(customer)
                .then(Mono.just("Recorded note"))
                .toFuture();

        return Uni.createFrom().future(future);
    }

    @GET
    @Path("/download/{customerId}/{documentId}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Uni<Response> downloadDocument(
            @PathParam("customerId")
            String customerId,

            @PathParam("documentId")
            String documentId) throws CustomerNotFoundException {
        Customer customer = customers.getCustomer(customerId);
        if (customer == null) {
            return Uni.createFrom().failure(new NotFoundException("Customer not found"));
        }

        CompletableFuture<byte[]> future = customerController.getDocument(customer, documentId)
                .flatMap(document -> Mono.just(document.getBytes(StandardCharsets.UTF_8)))
                .toFuture();

        return Uni.createFrom().future(future)
                .map(contentBytes -> {
                    Response.ResponseBuilder response = Response.ok(contentBytes);
                    response.header("Content-Disposition", "attachment; filename=" + documentId + ".txt");
                    return response.build();
                });
    }

    private Mono<QueryResult> executeRequest(String statement, Customer customer, String request, StatementType type) {
        switch (type) {
            case REQUEST -> {
                boolean externalResourcePlan = statement.startsWith("/");

                return customerController.buildPlan(customer, request, externalResourcePlan);
            }
            case QUESTION -> {
                return customerController.answerQuestion(customer, request);
            }
            case FACT -> {
                return customerController.addCustomerFact(customer, request)
                        .then(Mono.just(new QueryResult("Recorded note: statement", "")));
            }
            case EVENT -> {
                return customerController.addLogEvent(customer.getUid(), request)
                        .then(Mono.just(new QueryResult("Recorded event", "")));
            }
            default -> {
                return Mono.just(new QueryResult("Unknown statement type", ""));
            }
        }
    }

    public Mono<StatementType> classifyStatement(String statement) {
        return provider.getKernel()
                .<SKFunction<?>>map(kernel -> kernel.getFunction("Language", "StatementType"))
                .flatMap(skFunction -> skFunction.invokeAsync(statement))
                .map(SKContext::getResult)
                .map(String::toUpperCase)
                .map(String::strip)
                .map(StatementType::valueOf);
    }
}
