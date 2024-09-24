package com.microsoft.semantickernel.sample.java.sk.assistant.routes;

import com.azure.core.annotation.BodyParam;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.orchestration.FunctionResult;
import com.microsoft.semantickernel.orchestration.InvocationContext;
import com.microsoft.semantickernel.orchestration.InvocationReturnMode;
import com.microsoft.semantickernel.sample.java.sk.assistant.SemanticKernelProvider;
import com.microsoft.semantickernel.sample.java.sk.assistant.controllers.CustomerController;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Customer;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.CustomerNotFoundException;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Customers;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Notes;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.QueryResult;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.StatementType;
import com.microsoft.semantickernel.semanticfunctions.KernelFunctionArguments;
import com.microsoft.semantickernel.services.ServiceNotFoundException;
import com.microsoft.semantickernel.services.chatcompletion.AuthorRole;
import com.microsoft.semantickernel.services.chatcompletion.ChatCompletionService;
import com.microsoft.semantickernel.services.chatcompletion.ChatHistory;
import com.microsoft.semantickernel.services.chatcompletion.ChatMessageContent;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
    private final Customers customers;
    private final SemanticKernelProvider provider;
    private final CustomerController customerController;
    private final Map<String, ChatHistory> sessions = new ConcurrentHashMap<>();

    @Inject
    CustomerRoutes(
            Customers customers,
            SemanticKernelProvider provider
    ) {
        this.customers = customers;
        this.provider = provider;
        this.customerController = provider.getCustomerController();
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

    @POST
    @Path("/message/{customerId}")
    public Uni<String> receiveMessage(
            @PathParam("customerId") String customerId,
            @HeaderParam("chatId") String chatId,
            @BodyParam("body") String statement
    ) throws CustomerNotFoundException, ServiceNotFoundException {

        ChatHistory chat = getChat(customerId, chatId);
        if (chat == null) {
            throw new NotFoundException("Session not found");
        }

        Customer customer = customers.getCustomer(customerId);
        if (customer == null) {
            throw new NotFoundException("Customer not found");
        }

        String cleaned = statement.replaceAll("^[^a-zA-Z0-9]+", "").trim();

        if (chat.getLastMessage().isPresent() && chat.getMessages().size() > 2) {
            chat.addMessage(AuthorRole.USER, statement);

            Kernel kernel = provider.getKernel();

            Mono<String> response = kernel
                    .getService(ChatCompletionService.class)
                    .getChatMessageContentsAsync(chat, kernel,
                            InvocationContext.builder()
                                    .withReturnMode(InvocationReturnMode.NEW_MESSAGES_ONLY)
                                    .build())
                    .flatMapIterable(it -> {
                        chat.addAll(it);
                        return it;
                    })
                    .mapNotNull(ChatMessageContent::getContent)
                    .collectList()
                    .map(it -> {
                        return String.join("\n", it);
                    });

            return Uni.createFrom().future(response.toFuture());
        } else {
            return Uni.createFrom().future(executeQuery(cleaned, customer, chat).toFuture());
        }
    }

    private ChatHistory getChat(String customerId, String chatId) {
        String sessionId = customerId + "::" + chatId;

        if (!sessions.containsKey(sessionId)) {
            sessions.put(sessionId, new ChatHistory("You are a bot that helps a customer support agent manage customers"));
        }

        return sessions.get(sessionId);
    }

    private Mono<String> executeQuery(String query, Customer customer, ChatHistory chatHistory) {
        chatHistory.addUserMessage(query);
        return classifyStatement(query)
                .flatMap(type -> executeRequest(customer, query, type, chatHistory))
                .map(it -> {
                    chatHistory.addAssistantMessage(it.getResult());
                    return it.getResult();
                });
    }

    @POST
    @Path("perform/{customerId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<QueryResult> perform(
            @PathParam("customerId")
            String customerId,

            @HeaderParam("chatId")
            String chatId,

            @BodyParam("statement")
            String statement
    ) throws CustomerNotFoundException {
        ChatHistory chat = getChat(customerId, chatId);
        if (chat == null) {
            throw new NotFoundException("Session not found");
        }

        Customer customer = customers.getCustomer(customerId);
        if (customer == null) {
            return Uni.createFrom().failure(new NotFoundException("Customer not found"));
        }

        String cleaned = statement.replaceAll("^[^a-zA-Z0-9]+", "").trim();

        CompletableFuture<QueryResult> future = classifyStatement(cleaned)
                .flatMap(type -> executeRequest(customer, cleaned, type, chat))
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

        CompletableFuture<String> future = provider.getCustomerController().saveCustomerFacts(customer)
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

    private Mono<QueryResult> executeRequest(Customer customer, String request, StatementType type, ChatHistory chatHistory) {
        switch (type) {
            case REQUEST -> {
                return customerController.buildPlan(customer, chatHistory);
            }
            case QUESTION -> {
                return customerController.answerQuestion(customer, request);
            }
            case FACT -> {
                return customerController.addCustomerFact(customer, request)
                        .then(Mono.just(new QueryResult("Recorded note: statement", "")));
            }
            case EVENT -> {
                return customerController.addLogEvent(customer, request)
                        .then(Mono.just(new QueryResult("Recorded event", "")));
            }
            default -> {
                return Mono.just(new QueryResult("Unknown statement type", ""));
            }
        }
    }

    public Mono<StatementType> classifyStatement(String statement) {
        Kernel kernel = provider.getKernel();
        return kernel.<String>getFunction("Language", "StatementType")
                .invokeAsync(kernel)
                .withArguments(
                        KernelFunctionArguments.builder()
                                .withVariable("input", statement)
                                .build()
                )
                .map(FunctionResult::getResult)
                .map(String::toUpperCase)
                .map(String::strip)
                .map(StatementType::valueOf);
    }
}
