package com.microsoft.semantickernel.sample.java.sk.assistant.datastore;

import com.microsoft.semantickernel.sample.java.sk.assistant.models.AccountStatus;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.AccountType;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Customer;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Customers;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Log;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Notes;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.ext.Provider;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Generates fake customer data for the application.
 */
@Provider
public class GenerateCustomerData {

    @ApplicationScoped
    @Produces
    public static Customers generate() {
        Customers customers = new Customers();

        Instant time = Instant.parse("2023-01-01T11:00:00.00Z");

        // Example customer data
        customers
                .addCustomer(
                        new Customer(
                                "f9bea814-c99b-4834-a262-c4dbc8077530",
                                "John Doe",
                                AccountType.PREMIUM,
                                AccountStatus.CLOSED,
                                new Notes(
                                        List.of(
                                                "Wants to be called JD",
                                                "Complained of outage on 2021-01-01",
                                                "Contact during CET business hours",
                                                "Abusive customer, do not reopen account"
                                        )
                                ),
                                new Log()
                                        .addEvent(time.minus(20, ChronoUnit.DAYS), "Given free subscription for a month")
                                        .addEvent(time.minus(10, ChronoUnit.DAYS), "Given premium status as compensation for outage")
                                        .addEvent(time.minus(5, ChronoUnit.DAYS), "Account closed for abusing support staff"),
                                "1 Microsoft Way, Redmond, WA 98052, United States"
                        )
                )
                .addCustomer(
                        new Customer(
                                "bb200391-4f48-40ed-bb02-09f86a769b7b",
                                "Jane Doe",
                                AccountType.STANDARD,
                                AccountStatus.ACTIVE,
                                new Notes(
                                        List.of(
                                                "Waiting for response from customer services about premium status",
                                                "Happy with current service"
                                        )
                                ),
                                new Log()
                                        .addEvent(time.minus(20, ChronoUnit.DAYS), "Account created")
                                        .addEvent(time.minus(10, ChronoUnit.DAYS), "Contacted support about purchasing premium status"),
                                "1 Microsoft Way, Redmond, WA 98052, United States"
                        )
                )
                .addCustomer(
                        new Customer(
                                "f9c6ae7b-f2fc-401f-a5ef-db6594dce45f",
                                "Sam",
                                AccountType.PREMIUM,
                                AccountStatus.ACTIVE,
                                new Notes(
                                        List.of(
                                                "In EST time zone"
                                        )
                                ),
                                new Log()
                                        .addEvent(time.minus(1, ChronoUnit.DAYS), "Account created"),
                                "1 Microsoft Way, Redmond, WA 98052, United States"
                        )
                );
        return customers;
    }
}
