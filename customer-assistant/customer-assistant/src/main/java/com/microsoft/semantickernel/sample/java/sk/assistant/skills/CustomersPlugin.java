package com.microsoft.semantickernel.sample.java.sk.assistant.skills;

import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.orchestration.FunctionInvocation;
import com.microsoft.semantickernel.orchestration.FunctionResult;
import com.microsoft.semantickernel.sample.java.sk.assistant.SemanticKernelProvider;
import com.microsoft.semantickernel.sample.java.sk.assistant.datastore.CustomerDataStore;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.AccountStatus;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.AccountType;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Customer;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.CustomerNotFoundException;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Customers;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.QueryResult;
import com.microsoft.semantickernel.semanticfunctions.KernelFunctionArguments;
import com.microsoft.semantickernel.semanticfunctions.annotations.DefineKernelFunction;
import com.microsoft.semantickernel.semanticfunctions.annotations.KernelFunctionParameter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;


public class CustomersPlugin {
    private final Customers customers;
    private final SemanticKernelProvider semanticKernelProvider;
    private final CustomerDataStore customerDataStore;

    public CustomersPlugin(
            Customers customers,
            SemanticKernelProvider semanticKernelProvider,
            CustomerDataStore customerDataStore) {
        this.customers = customers;
        this.semanticKernelProvider = semanticKernelProvider;
        this.customerDataStore = customerDataStore;
    }

    @DefineKernelFunction(
            name = "queryCustomerFacts",
            description = "Searches the customer database to find information about a customer.",
            returnType = "String"
    )
    public Mono<QueryResult> queryCustomerFacts(
            @KernelFunctionParameter(
                    name = "customerId",
                    description = "The id of the customer to search for."
            )
            String customerId,
            @KernelFunctionParameter(
                    name = "query",
                    description = "The query to search for."
            )
            String question) {
        try {
            Customer customer = customers.getCustomer(customerId);
            return customerDataStore
                    .queryCustomer(customer, question)
                    .concatMap(result -> Flux
                            .fromIterable(result)
                            .map(it -> {
                                return it.getInfo().replaceAll("DOCUMENT", "DOCUMENT " + it.getId());
                            }))
                    .collectList()
                    .flatMap(facts -> askQuery(customer, facts, question.replaceAll("\\?", "")));
        } catch (CustomerNotFoundException e) {
            return Mono.error(e);
        }
    }


    // Fake address lookup
    @DefineKernelFunction(
            name = "getAddress",
            description = "Gets the address of a customer."
    )
    public String getAddress(
            @KernelFunctionParameter(
                    name = "customerId",
                    description = "The id of the customer."
            )
            String uid) throws CustomerNotFoundException {
        return customers.getCustomer(uid).getAddress();
    }

    @DefineKernelFunction(
            name = "setAccountStatus",
            description = "Sets the account status."
    )
    public void setAccountStatus(
            @KernelFunctionParameter(
                    name = "customerId",
                    description = "The id of the customer."
            )
            String customerId,

            @KernelFunctionParameter(
                    name = "accountStatus",
                    description = "status to set the account to, either active, suspended or closed."
            )
            String accountStatus
    ) throws CustomerNotFoundException {
        AccountStatus status = AccountStatus.valueOf(accountStatus.toUpperCase());
        Customer customer = customers.getCustomer(customerId);
        customer.setAccountStatus(status);
    }


    @DefineKernelFunction(
            name = "setAccountType",
            description = "Sets the account type."
    )
    public void setAccountType(
            @KernelFunctionParameter(
                    name = "customerId",
                    description = "The id of the customer."
            )
            String customerId,

            @KernelFunctionParameter(
                    name = "accountType",
                    description = "account type to set the account to, either standard, premium or enterprise."
            )
            String accountType
    ) throws CustomerNotFoundException {
        AccountType type = AccountType.valueOf(accountType.toUpperCase());
        Customer customer = customers.getCustomer(customerId);
        customer.setAccountType(type);
    }

    @DefineKernelFunction(
            name = "addLogEvent",
            description = "Adds an event to a customers log.",
            returnType = "String"
    )
    public Mono<String> addLogEvent(
            @KernelFunctionParameter(
                    name = "customerId",
                    description = "The id of the customer."
            )
            String customerId,

            @KernelFunctionParameter(
                    name = "eventDescription",
                    description = "A description of the event."
            )
            String eventDescription
    ) {
        Customer customer;
        try {
            customer = customers.getCustomer(customerId);
        } catch (CustomerNotFoundException e) {
            return Mono.error(e);
        }
        customer.getLog().addEvent(eventDescription);
        return Mono.just("Done");
    }


    private Mono<QueryResult> askQuery(Customer customer, List<String> facts, String question) {

        Collections.reverse(facts);

        Kernel kernel = semanticKernelProvider.getKernel();

        KernelFunctionArguments arguments = KernelFunctionArguments.builder()
                .withVariable("facts", facts)
                .withVariable("customer", customer)
                .withVariable("customerId", customer.getUid())
                .withVariable("currentDateTime", Instant.now().atZone(ZoneId.of("Z")).format(DateTimeFormatter.ISO_ZONED_DATE_TIME))
                .withVariable("input", question)
                .build();

        Mono<String> answer = kernel
                .<String>getFunction("CustomerSkills", "QueryInfo")
                .invokeAsync(kernel)
                .withArguments(arguments)
                .mapNotNull(FunctionResult::getResult)
                .map(String::trim);

        FunctionInvocation<String> documents = kernel
                .<String>getFunction("CustomerSkills", "GenerateDownloadLinks")
                .invokeAsync(kernel)
                .withArguments(arguments);

        return Mono.zip(answer, documents)
                .map(tuple -> {
                    return new QueryResult(tuple.getT1(), tuple.getT2().getResult());
                });
    }


}
