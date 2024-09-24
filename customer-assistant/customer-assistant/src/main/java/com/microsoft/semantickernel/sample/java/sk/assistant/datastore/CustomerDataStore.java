package com.microsoft.semantickernel.sample.java.sk.assistant.datastore;

import com.microsoft.semantickernel.connectors.data.jdbc.JDBCVectorStoreRecordCollectionOptions;
import com.microsoft.semantickernel.data.VectorStore;
import com.microsoft.semantickernel.data.VectorStoreRecordCollection;
import com.microsoft.semantickernel.data.recordoptions.GetRecordOptions;
import com.microsoft.semantickernel.implementation.EmbeddedResourceLoader;
import com.microsoft.semantickernel.sample.java.sk.assistant.SemanticKernelProvider;
import com.microsoft.semantickernel.sample.java.sk.assistant.controllers.CustomerInfo;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.FileNotFoundException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CustomerDataStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerDataStore.class);

    private final SemanticKernelProvider semanticKernelProvider;

    public CustomerDataStore(SemanticKernelProvider semanticKernelProvider) {
        this.semanticKernelProvider = semanticKernelProvider;
    }

    public Mono<VectorStoreRecordCollection<String, CustomerInfo>> getCollectionForCustomer(Customer customer) {
        VectorStoreRecordCollection<String, CustomerInfo> collection = getVectorStore()
                .getCollection(
                        getCollectionName(customer),
                        JDBCVectorStoreRecordCollectionOptions.<CustomerInfo>builder()
                                .withRecordClass(CustomerInfo.class)
                                .build());

        return collection
                .createCollectionIfNotExistsAsync()
                .doOnError(e -> {
                    LOGGER.error("Failed to create collection", e);
                });
    }


    public static Flux<String> loadCustomerDataFromResources(Customer customer) {
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


    public Flux<List<CustomerInfo>> queryCustomer(Customer customer, String query) {
        // TODO call customerDataStore.queryCustomer when we have a real implementation
        return fakeFetch(customer)
                .map(facts -> {
                    return facts.stream()
                            .map(fact -> {
                                return new CustomerInfo(fact, List.of());
                            })
                            .toList();
                })
                .flux();
/*
        return getCollectionForCustomer(customer)
                .flatMapMany(collection -> {
                    return collection
                            .getBatchAsync(
                                    List.of(query),
                                    GetRecordOptions.builder()
                                            .includeVectors(true)
                                            .build()
                            );
                });

 */
    }


    private static Mono<List<String>> fakeFetch(Customer customer) {
        List<String> notes = customer.getNotes()
                .notes()
                .stream()
                .map(it -> "NOTE: " + it)
                .toList();

        List<String> logs = customer.getLog().log()
                .stream()
                .map(it -> "EVENT: " + it.timestamp().atZone(ZoneId.of("Z")).format(DateTimeFormatter.ISO_ZONED_DATE_TIME) + " - " + it.event())
                .toList();

        return CustomerDataStore.loadCustomerDataFromResources(customer)
                .map(it -> "DOCUMENT: " + it)
                .collectList()
                .map(docs -> {
                    ArrayList<String> facts2 = new ArrayList<>(notes);
                    facts2.addAll(logs);
                    facts2.addAll(docs);
                    return facts2;
                });
    }

    private VectorStore getVectorStore() {
        return semanticKernelProvider.getMemoryStore();
    }

    public static String getCollectionName(Customer customer) {
        return ("customer-" + customer.getUid()).replaceAll("-", "_");
    }

}
