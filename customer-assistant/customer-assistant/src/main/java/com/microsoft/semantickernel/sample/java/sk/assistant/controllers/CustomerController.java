package com.microsoft.semantickernel.sample.java.sk.assistant.controllers;

import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.connectors.data.jdbc.JDBCVectorStoreRecordCollectionOptions;
import com.microsoft.semantickernel.contextvariables.converters.ContextVariableJacksonConverter;
import com.microsoft.semantickernel.orchestration.FunctionResult;
import com.microsoft.semantickernel.orchestration.InvocationContext;
import com.microsoft.semantickernel.orchestration.InvocationReturnMode;
import com.microsoft.semantickernel.orchestration.ToolCallBehavior;
import com.microsoft.semantickernel.sample.java.sk.assistant.SemanticKernelProvider;
import com.microsoft.semantickernel.sample.java.sk.assistant.datastore.CustomerDataStore;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Customer;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.QueryResult;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Rules;
import com.microsoft.semantickernel.semanticfunctions.KernelFunction;
import com.microsoft.semantickernel.semanticfunctions.KernelFunctionArguments;
import com.microsoft.semantickernel.services.chatcompletion.ChatHistory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * Provides functions and skills for modifying and querying customer data.
 */
public class CustomerController {
    private final SemanticKernelProvider semanticKernelProvider;
    private final CustomerDataStore customerDataStore;
    private final Rules rules;

    public CustomerController(
            SemanticKernelProvider semanticKernelProvider,
            CustomerDataStore customerDataStore,
            Rules rules) {
        this.semanticKernelProvider = semanticKernelProvider;
        this.customerDataStore = customerDataStore;
        this.rules = rules;
    }

    public Mono<String> saveCustomerFacts(Customer customer) {
        return saveCustomerEvents(customer)
                .then(saveCustomerNotes(customer))
                .then(saveCustomerDocs(customer))
                .then(Mono.just("Done"));
    }

    private Mono<String> saveCustomerDocs(Customer customer) {
        return customerDataStore.getCollectionForCustomer(customer)
                .flatMapMany(collection -> {
                    return CustomerDataStore.loadCustomerDataFromResources(customer)
                            .filter(Objects::nonNull)
                            .concatMap(doc -> {
                                return semanticKernelProvider
                                        .getEmbedding()
                                        .generateEmbeddingsAsync(List.of(doc))
                                        .map(embeddings -> {
                                            return new CustomerInfo(
                                                    "DOCUMENT:\n" + doc + "\n",
                                                    embeddings.get(0).getVector()
                                            );
                                        });
                            })
                            .concatMap(customerInfo -> {
                                return semanticKernelProvider
                                        .getMemoryStore()
                                        .getCollection(
                                                CustomerDataStore.getCollectionName(customer),
                                                JDBCVectorStoreRecordCollectionOptions.<CustomerInfo>builder()
                                                        .withRecordClass(CustomerInfo.class)
                                                        .build()
                                        )
                                        .upsertAsync(customerInfo, null);
                            });
                })
                .then(Mono.just("Done"));
    }


    private Mono<Void> saveCustomerNotes(Customer customer) {
        return customerDataStore.getCollectionForCustomer(customer)
                .flatMapMany(collection -> {
                    return Flux.fromIterable(customer.getNotes().notes())
                            .concatMap(note -> {
                                return semanticKernelProvider
                                        .getEmbedding()
                                        .generateEmbeddingsAsync(List.of(note))
                                        .map(embeddings -> {
                                            return new CustomerInfo(
                                                    "NOTE: " + note,
                                                    embeddings.get(0).getVector()
                                            );
                                        });
                            });
                })
                .last()
                .then();
    }

    private Mono<Void> saveCustomerEvents(Customer customer) {
        return customerDataStore.getCollectionForCustomer(customer)
                .flatMapMany(collection -> {
                    return Flux.fromIterable(customer.getLog().log())
                            .concatMap(logEvent -> {
                                return semanticKernelProvider
                                        .getEmbedding()
                                        .generateEmbeddingsAsync(List.of(logEvent.event()))
                                        .map(embeddings -> {
                                            return collection
                                                    .upsertAsync(new CustomerInfo(
                                                            "EVENT: " + logEvent.timestamp().atZone(ZoneId.of("Z")).format(DateTimeFormatter.ISO_ZONED_DATE_TIME) + " - " + logEvent.event(),
                                                            embeddings.get(0).getVector()
                                                    ), null);
                                        });
                            });
                })
                .last()
                .then();
    }

    public Mono<String> getDocument(Customer customer, String documentId) {
        return queryCustomerDocument(customer, documentId);
    }

    public Mono<String> queryCustomerDocument(Customer customer, String documentId) {
        return customerDataStore.getCollectionForCustomer(customer)
                .flatMap(collection -> collection
                        .getAsync(documentId, null)
                        .map(document ->
                                document
                                        .getInfo()
                                        .replaceAll("DOCUMENT:", "")
                        )
                );
    }

    public Mono<QueryResult> addCustomerFact(Customer customer, String statement) {
        customer.getNotes().notes().add(statement);
        return Mono.zip(saveCustomerFacts(customer), Mono.just(""))
                .map(tuple -> {
                    return new QueryResult(tuple.getT1(), tuple.getT2());
                });
    }


    public Mono<QueryResult> buildPlan(Customer customer, ChatHistory chatHistory) {
        Kernel kernel = semanticKernelProvider.getKernel();

        KernelFunction<String> function = KernelFunction.<String>createFromPrompt("""
                            <message role="system">
                                Take the chat history below and perform the actions needed to action their last request
                        
                                When planning how to achieve the customer's goal, abide by the following rules:
                                {{#each rules}}
                                    - {{this}}
                                {{/each}}
                            </message>
                        
                            <message role="system">
                                info about the customer:
                                {{customer}}
                            </message>
                        
                            {{#each chatHistory}}
                                <message role="{{role}}">{{content}}</message>
                            {{/each}}
                        """)
                .withTemplateFormat("handlebars")
                .build();

        return kernel
                .invokeAsync(function)
                .withArguments(
                        KernelFunctionArguments.builder()
                                .withVariable("chatHistory", chatHistory)
                                .withVariable("customer", customer)
                                .withVariable("rules", rules.getRules())
                                .build())
                .withInvocationContext(
                        InvocationContext.builder()
                                .withReturnMode(InvocationReturnMode.NEW_MESSAGES_ONLY)
                                .withToolCallBehavior(ToolCallBehavior.allowAllKernelFunctions(true))
                                .build())
                .mapNotNull(it -> {
                    chatHistory.addAssistantMessage(it.getResult());
                    return it.getResult();
                })
                .map(it -> new QueryResult(it, null));

    }

    public Mono<QueryResult> answerQuestion(Customer customer, String query) {
        Kernel kernel = semanticKernelProvider.getKernel();

        return kernel
                .getFunction("CustomerSkills", "queryCustomerFacts")
                .invokeAsync(kernel)
                .withArguments(
                        KernelFunctionArguments.builder()
                                .withVariable("customerId", customer.getUid())
                                .withVariable("query", query)
                                .build())
                .withTypeConverter(ContextVariableJacksonConverter.create(QueryResult.class))
                .withResultType(QueryResult.class)
                .withInvocationContext(
                        InvocationContext.builder()
                                .withReturnMode(InvocationReturnMode.NEW_MESSAGES_ONLY)
                                .withToolCallBehavior(ToolCallBehavior.allowAllKernelFunctions(true))
                                .build()

                )
                .map(FunctionResult::getResult);
    }

    public Mono<String> addLogEvent(Customer customer, String eventDescription) {
        Kernel kernel = semanticKernelProvider.getKernel();

        return kernel
                .getFunction("CustomerSkills", "addLogEvent")
                .invokeAsync(kernel)
                .withArguments(
                        KernelFunctionArguments.builder()
                                .withVariable("customerId", customer.getUid())
                                .withVariable("eventDescription", eventDescription)
                                .build())
                .withResultType(String.class)
                .map(FunctionResult::getResult);
    }
}

