package com.microsoft.semantickernel.sample.java.sk.assistant.controllers;

import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.SKBuilders;
import com.microsoft.semantickernel.memory.MemoryQueryResult;
import com.microsoft.semantickernel.memory.SemanticTextMemory;
import com.microsoft.semantickernel.orchestration.SKContext;
import com.microsoft.semantickernel.orchestration.SKFunction;
import com.microsoft.semantickernel.planner.actionplanner.Plan;
import com.microsoft.semantickernel.planner.sequentialplanner.SequentialPlanner;
import com.microsoft.semantickernel.planner.stepwiseplanner.DefaultStepwisePlanner;
import com.microsoft.semantickernel.planner.stepwiseplanner.StepwisePlannerConfig;
import com.microsoft.semantickernel.sample.java.sk.assistant.SemanticKernelProvider;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.AccountStatus;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.AccountType;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Customer;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.CustomerNotFoundException;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Customers;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.QueryResult;
import com.microsoft.semantickernel.sample.java.sk.assistant.skills.Emailer;
import com.microsoft.semantickernel.sample.java.sk.assistant.skills.MapSkill;
import com.microsoft.semantickernel.sample.java.sk.assistant.utilities.Rules;
import com.microsoft.semantickernel.skilldefinition.annotations.DefineSKFunction;
import com.microsoft.semantickernel.skilldefinition.annotations.SKFunctionParameters;
import com.microsoft.semantickernel.util.EmbeddedResourceLoader;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.FileNotFoundException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Provides functions and skills for modifying and querying customer data.
 */
@ApplicationScoped
public class CustomerController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerController.class);
    public static final String START_DOCUMENT = "START DOCUMENT";
    public static final String END_DOCUMENT = "END DOCUMENT";
    private final Customers customers;
    private final SemanticKernelProvider semanticKernelProvider;
    private final Rules rules;

    @Inject
    public CustomerController(
            Customers customers,
            SemanticKernelProvider semanticKernelProvider,
            Rules rules) {
        this.customers = customers;
        this.semanticKernelProvider = semanticKernelProvider;
        this.rules = rules;
    }

    public static String getId(String fact) {
        String id;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            id = Base64.getEncoder().encodeToString(digest.digest(fact.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return id;
    }

    public Mono<String> saveCustomerFacts(Customer customer) {
        return semanticKernelProvider.getKernel()
                .map(Kernel::getMemory)
                .flatMap(memory -> {
                    return saveCustomerEvents(customer, memory)
                            .then(saveCustomerNotes(customer, memory))
                            .then(saveCustomerDocs(customer, memory))
                            .then(Mono.just("Done"));
                });
    }

    private Mono<String> saveCustomerDocs(Customer customer, SemanticTextMemory memory) {
        return loadCustomerDataFromResources(customer)
                .filter(Objects::nonNull)
                .concatMap(doc -> {
                    String id = getId(doc);

                    return memory.saveInformationAsync(
                            getCollectionName(customer),
                            START_DOCUMENT + ":\n" + doc + "\n" + END_DOCUMENT,
                            id,
                            null,
                            null
                    );
                })
                .last("Done");
    }

    private static Flux<String> loadCustomerDataFromResources(Customer customer) {
        return Flux.range(1, 100)
                .map(i -> {
                    return "data/" + customer.getUid() + "/" + i + ".txt";
                }).mapNotNull(file -> {
                    try {
                        return EmbeddedResourceLoader.readFile(file, Customer.class, EmbeddedResourceLoader.ResourceLocation.CLASSPATH_ROOT);
                    } catch (FileNotFoundException e) {
                        return null;
                    }
                });
    }

    private static Mono<Void> saveCustomerNotes(Customer customer, SemanticTextMemory memory) {
        return Flux.fromIterable(customer.getNotes().notes())
                .concatMap(note -> {
                    String id = getId(note);
                    return memory.saveInformationAsync(
                            getCollectionName(customer),
                            "NOTE: " + note,
                            id,
                            null,
                            null
                    );
                })
                .last()
                .then();
    }


    private static Mono<Void> saveCustomerEvents(Customer customer, SemanticTextMemory memory) {
        return Flux.fromIterable(customer.getLog().log())
                .concatMap(logEvent -> {
                    String id = getId(logEvent.event());

                    return memory.saveInformationAsync(
                            getCollectionName(customer),
                            "EVENT: " + logEvent.timestamp().atZone(ZoneId.of("Z")).format(DateTimeFormatter.ISO_ZONED_DATE_TIME) + " - " + logEvent.event(),
                            id,
                            null,
                            null
                    );
                })
                .last()
                .then();
    }

    public Mono<String> getDocument(Customer customer, String documentId) {
        return queryCustomerDocument(customer, documentId);
    }

    public Mono<QueryResult> answerQuestion(Customer customer, String query) {
        return askQueryAboutCustomer(customer.getUid(), query)
                .filter(result -> {
                    return !result.getResult().contains("I don't know");
                })
                .switchIfEmpty(
                        buildPlan(customer, query, true).single()
                );
    }

    public Flux<String> queryCustomerFacts(Customer customer, String query) {

        return semanticKernelProvider.getKernel()
                .map(Kernel::getMemory)
                .flatMap(memory -> {
                    return memory.searchAsync(getCollectionName(customer), query, 10, 0.0f, true);
                })
                .flatMapMany(result -> {
                    return Flux
                            .fromIterable(result)
                            .sort(Comparator.comparingDouble(MemoryQueryResult::getRelevance))
                            .map(it -> {
                                if (it.getMetadata().getText().contains(START_DOCUMENT)) {
                                    String text = it.getMetadata().getText();
                                    text = text.replace(START_DOCUMENT, START_DOCUMENT + " " + URLEncoder.encode(it.getMetadata().getId(), StandardCharsets.UTF_8));
                                    return text;
                                } else {
                                    return it.getMetadata().getText();
                                }
                            });
                });

    }

    public Mono<String> queryCustomerDocument(Customer customer, String documentId) {
        return semanticKernelProvider.getKernel()
                .map(Kernel::getMemory)
                .flatMap(memory -> {
                    return memory.getAsync(getCollectionName(customer), documentId, false)
                            .map(result -> {
                                return result.getMetadata().getText().replace(START_DOCUMENT + ":\n", "").replace(END_DOCUMENT, "");
                            });
                });
    }

    private static String getCollectionName(Customer customer) {
        return "customer." + customer.getUid();
    }

    private Mono<QueryResult> askQuery(Customer customer, List<String> facts, String question) {

        Collections.reverse(facts);
        String factString = facts.stream().limit(10).collect(Collectors.joining("\n"));

        SKContext context = SKBuilders.context().build();
        context.setVariable("facts", factString);
        context.setVariable("customer", customer.getName());
        context.setVariable("customerId", customer.getUid());
        context.setVariable("currentDateTime", Instant.now().atZone(ZoneId.of("Z")).format(DateTimeFormatter.ISO_ZONED_DATE_TIME));

        Mono<String> answer = semanticKernelProvider.getKernel()
                .<SKFunction<?>>map(kernel -> kernel.getFunction("CustomerSkills", "QueryInfo"))
                .flatMap(function -> {
                    return function.invokeAsync(question, context, null);
                })
                .map(SKContext::getResult)
                .map(String::trim);

        Mono<String> documents = semanticKernelProvider.getKernel()
                .<SKFunction<?>>map(kernel -> kernel.getFunction("CustomerSkills", "GenerateDownloadLinks"))
                .flatMap(function -> {
                    return function.invokeAsync(question, context, null);
                })
                .map(SKContext::getResult)
                .map(String::trim);

        return Mono.zip(answer, documents)
                .map(tuple -> {
                    return new QueryResult(tuple.getT1(), tuple.getT2());
                });
    }


    public Mono<QueryResult> buildPlan(Customer customer, String req, boolean stepwise) {
        return semanticKernelProvider.getKernelEmpty()
                .flatMap(kernel -> {
                    kernel.importSkill(this, "CustomerController");
                    return this.rules.getRules()
                            .defaultIfEmpty("")
                            .flatMap(rules -> buildPlan(customer, req, stepwise, kernel, rules));
                });
    }

    private Mono<QueryResult> buildPlan(
            Customer customer,
            String req,
            boolean stepwise,
            Kernel kernel,
            String rules) {
        String request = req;
        Mono<Plan> planGetter;

        if (stepwise) {
            kernel.importSkill(new MapSkill(), "MapSkill");

            StepwisePlannerConfig config = new StepwisePlannerConfig();
            config.addExcludedFunctions("addLogEvent");

            request += "\n[OBSERVATION]\nCustomers customerId is " + customer.getUid() + "\n";

            var stepwisePlan = new DefaultStepwisePlanner(kernel,
                    config,
                    null,
                    null)
                    .createPlan(request);
            planGetter = Mono.just(stepwisePlan);
        } else {
            kernel.importSkill(new Emailer(), "Emailer");

            SequentialPlanner planner = SemanticKernelProvider.getPlanner(kernel);
            SKContext context = SKBuilders
                    .context()
                    .withVariables(SKBuilders
                            .variables()
                            .withVariable("rules", rules)
                            .withVariable("customer", customer.getName())
                            .withVariable("customerName", customer.getName())
                            .withVariable("customerId", customer.getUid())
                            .build())
                    .withSkills(kernel.getSkills())
                    .build();
            planGetter = planner.createPlanAsync(request, context);
        }

        Mono<String> planResult = planGetter
                .flatMap(plan -> {
                    LOGGER.info(plan.toPlanString());
                    SKContext context2 = SKBuilders
                            .context()
                            .withVariables(SKBuilders
                                    .variables()
                                    .withVariable("rules", rules)
                                    .withVariable("customer", customer.getName())
                                    .withVariable("customerName", customer.getName())
                                    .withVariable("customerId", customer.getUid())
                                    .withInput(customer.getName())
                                    .build())
                            .withSkills(kernel.getSkills())
                            .build();

                    return plan.invokeAsync(context2);
                })
                .map(SKContext::getResult);

        // Plan result + no related documents
        return Mono.zip(planResult, Mono.just(""))
                .map(tuple -> new QueryResult(tuple.getT1(), tuple.getT2()));
    }


    @DefineSKFunction(
            name = "queryCustomerFacts",
            description = "Searches the customer database to find information about a customer."
    )
    public Mono<String> queryCustomerFacts(
            @SKFunctionParameters(
                    name = "customerId",
                    description = "The id of the customer to search for."
            )
            String customerId,
            @SKFunctionParameters(
                    name = "query",
                    description = "The query to search for."
            )
            String question) {
        return askQueryAboutCustomer(customerId, question)
                .map(QueryResult::getResult);
    }

    public Mono<QueryResult> askQueryAboutCustomer(
            String customerId,
            String question) {
        try {
            Customer customer = customers.getCustomer(customerId);
            return queryCustomerFacts(customer, question)
                    .collectList()
                    .flatMap(facts -> {
                        return askQuery(customer, facts, question.replaceAll("\\?", ""));
                    });
        } catch (CustomerNotFoundException e) {
            return Mono.error(e);
        }
    }

    // Fake address lookup
    @DefineSKFunction(
            name = "getAddress",
            description = "Gets the address of a customer."
    )
    public String getAddress(
            @SKFunctionParameters(
                    name = "customerId",
                    description = "The id of the customer."
            )
            String uid) throws CustomerNotFoundException {
        return customers.getCustomer(uid).getAddress();
    }

    @DefineSKFunction(
            name = "setAccountStatus",
            description = "Sets the account status."
    )
    public void setAccountStatus(
            @SKFunctionParameters(
                    name = "customerId",
                    description = "The id of the customer."
            )
            String customerId,

            @SKFunctionParameters(
                    name = "accountStatus",
                    description = "status to set the account to, either active, suspended or closed."
            )
            String accountStatus
    ) throws CustomerNotFoundException {
        AccountStatus status = AccountStatus.valueOf(accountStatus.toUpperCase());
        Customer customer = customers.getCustomer(customerId);
        customer.setAccountStatus(status);
    }


    @DefineSKFunction(
            name = "setAccountType",
            description = "Sets the account type."
    )
    public void setAccountType(
            @SKFunctionParameters(
                    name = "customerId",
                    description = "The id of the customer."
            )
            String customerId,

            @SKFunctionParameters(
                    name = "accountType",
                    description = "account type to set the account to, either standard, premium or enterprise."
            )
            String accountType
    ) throws CustomerNotFoundException {
        AccountType type = AccountType.valueOf(accountType.toUpperCase());
        Customer customer = customers.getCustomer(customerId);
        customer.setAccountType(type);
    }

    @DefineSKFunction(
            name = "addLogEvent",
            description = "Adds an event to a customers log."
    )
    public Mono<String> addLogEvent(
            @SKFunctionParameters(
                    name = "customerId",
                    description = "The id of the customer."
            )
            String customerId,

            @SKFunctionParameters(
                    name = "eventDescription",
                    description = "A description of the event."
            )
            String eventDescription
    ) {
        Customer customer = null;
        try {
            customer = customers.getCustomer(customerId);
        } catch (CustomerNotFoundException e) {
            return Mono.error(e);
        }
        customer.getLog().addEvent(eventDescription);
        return Mono.just("Done");
    }

    public Mono<QueryResult> addCustomerFact(Customer customer, String statement) {
        customer.getNotes().notes().add(statement);
        return Mono.zip(saveCustomerFacts(customer), Mono.just(""))
                .map(tuple -> {
                    return new QueryResult(tuple.getT1(), tuple.getT2());
                });
    }
}
