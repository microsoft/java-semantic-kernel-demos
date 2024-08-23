package com.microsoft.semantickernel.sample.java.sk.assistant;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.http.policy.FixedDelayOptions;
import com.azure.core.http.policy.RetryOptions;
import com.azure.core.util.HttpClientOptions;
import com.azure.search.documents.indexes.SearchIndexClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.aiservices.openai.chatcompletion.OpenAIChatCompletion;
import com.microsoft.semantickernel.aiservices.openai.textembedding.OpenAITextEmbeddingGenerationService;
import com.microsoft.semantickernel.connectors.data.hsqldb.HSQLDBVectorStoreQueryProvider;
import com.microsoft.semantickernel.connectors.data.jdbc.JDBCVectorStore;
import com.microsoft.semantickernel.connectors.data.jdbc.JDBCVectorStoreOptions;
import com.microsoft.semantickernel.contextvariables.ContextVariableTypes;
import com.microsoft.semantickernel.contextvariables.converters.ContextVariableJacksonConverter;
import com.microsoft.semantickernel.data.VectorStore;
import com.microsoft.semantickernel.implementation.EmbeddedResourceLoader;
import com.microsoft.semantickernel.plugin.KernelPluginFactory;
import com.microsoft.semantickernel.sample.java.sk.assistant.controllers.CustomerController;
import com.microsoft.semantickernel.sample.java.sk.assistant.datastore.CustomerDataStore;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Customer;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Customers;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Rules;
import com.microsoft.semantickernel.sample.java.sk.assistant.skills.CustomersPlugin;
import com.microsoft.semantickernel.sample.java.sk.assistant.skills.Emailer;
import com.microsoft.semantickernel.semanticfunctions.HandlebarsPromptTemplateFactory;
import com.microsoft.semantickernel.semanticfunctions.KernelFunctionYaml;
import com.microsoft.semantickernel.services.chatcompletion.ChatCompletionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hsqldb.jdbc.JDBCDataSourceFactory;
import reactor.core.publisher.Flux;

import javax.sql.DataSource;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * A factory class that constructs kernels for use by the application
 */
@ApplicationScoped
public class SemanticKernelProvider {
    private static DataSource datasource;
    private final CustomerController customerController;

    private final String model;
    private final String cognativeSearchEndpoint;
    private final String cognativeSearchToken;
    private final Customers customers;
    private final String embeddingModel;
    private final String openAiEndpoint;
    private final String openAiToken;
    private final CustomerDataStore customerDataStore;
    private VectorStore vectorStore;
    private JDBCVectorStore memoryStore;

    @Inject
    public SemanticKernelProvider(
            Customers customers,
            Rules rules,

            @ConfigProperty(name = "openai.endpoint")
            String openAiEndpoint,
            @ConfigProperty(name = "openai.token")
            String openAiToken,
            @ConfigProperty(name = "chatcompletion.model", defaultValue = "gpt-4o")
            String model,
            @ConfigProperty(name = "embedding.model", defaultValue = "text-embedding-3-large")
            String embeddingModel,
            @ConfigProperty(name = "cognativesearch.endpoint")
            Optional<String> cognativeSearchEndpoint,
            @ConfigProperty(name = "cognativesearch.token")
            Optional<String> cognativeSearchToken
    ) {
        this.model = model;
        this.embeddingModel = embeddingModel;
        this.cognativeSearchEndpoint = cognativeSearchEndpoint.orElse(null);
        this.cognativeSearchToken = cognativeSearchToken.orElse(null);
        this.customers = customers;
        this.openAiEndpoint = openAiEndpoint;
        this.openAiToken = openAiToken;

        System.out.println("END: " + openAiEndpoint);
/*
        var searchClient = new SearchIndexClientBuilder()
                .endpoint(this.cognativeSearchEndpoint)
                .credential(new AzureKeyCredential(this.cognativeSearchToken))
                .buildAsyncClient();

 */

        this.customerDataStore = new CustomerDataStore(this);

        this.customerController = new CustomerController(this, customerDataStore, rules);

        // Add converter to global types
        ContextVariableTypes.addGlobalConverter(
                ContextVariableJacksonConverter
                        .builder(Customer.class,
                                new ObjectMapper()
                                        .registerModule(new JavaTimeModule())).build());
    }

    public void init() {
        Flux.fromIterable(customers.getCustomers())
                .concatMap(customerController::saveCustomerFacts)
                .blockLast();
    }

    private OpenAIAsyncClient getOpenAIAsyncClient() {

        RetryOptions retryOptions = new RetryOptions(new FixedDelayOptions(2, Duration.ofSeconds(10)));

        return new OpenAIClientBuilder()
                .clientOptions(new HttpClientOptions()
                        .setReadTimeout(Duration.ofSeconds(10))
                        .setResponseTimeout(Duration.ofSeconds(10)))
                .retryOptions(retryOptions)
                .endpoint(openAiEndpoint)
                .credential(new AzureKeyCredential(openAiToken))
                .buildAsyncClient();
    }

    /*
    public static StepwisePlanner getStepwisePlanner(Kernel kernel) {
        return new DefaultStepwisePlanner(kernel, null, null, null);
    }

    public static SequentialPlanner getPlanner(Kernel kernel) {
        try {
            String prompt = EmbeddedResourceLoader.readFile("planprompt.txt", SemanticKernelProvider.class);
            return new SequentialPlanner(
                    kernel,
                    new SequentialPlannerRequestSettings(
                            null,
                            100,
                            Set.of(),
                            Set.of(),
                            Set.of(),
                            1024
                    ),
                    prompt,
                    true
            );
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
     */

    public Kernel getKernelEmpty() {
        return Kernel.builder()
                .withAIService(ChatCompletionService.class,
                        OpenAIChatCompletion.builder()
                                .withModelId(model)
                                .withOpenAIAsyncClient(
                                        new OpenAIClientBuilder()
                                                .endpoint(openAiEndpoint)
                                                .credential(new AzureKeyCredential(openAiToken))
                                                .buildAsyncClient()
                                )
                                .build()
                )
                .build();
    }

    public Kernel getKernel() {
        Kernel.Builder builder = Kernel.builder()
                .withAIService(ChatCompletionService.class,
                        OpenAIChatCompletion.builder()
                                .withModelId(model)
                                .withOpenAIAsyncClient(
                                        new OpenAIClientBuilder()
                                                .endpoint(openAiEndpoint)
                                                .credential(new AzureKeyCredential(openAiToken))
                                                .buildAsyncClient()
                                )
                                .build()
                );
        return addSkills(builder).build();
    }

    private Kernel.Builder addSkills(Kernel.Builder builder) {
        try {
            builder.withPlugin(
                    KernelPluginFactory.createFromFunctions(
                            "CustomerSkills",
                            "QueryInfo",
                            List.of(
                                    KernelFunctionYaml.fromPromptYaml(
                                            EmbeddedResourceLoader.readFile(
                                                    "skills/CustomerSkills/QueryInfo/queryInfo.yml",
                                                    SemanticKernelProvider.class,
                                                    EmbeddedResourceLoader.ResourceLocation.CLASSPATH_ROOT
                                            ),
                                            new HandlebarsPromptTemplateFactory())
                            )
                    )
            );

            builder.withPlugin(
                    KernelPluginFactory.createFromFunctions(
                            "CustomerSkills",
                            "GenerateDownloadLinks",
                            List.of(
                                    KernelFunctionYaml.fromPromptYaml(
                                            EmbeddedResourceLoader.readFile(
                                                    "skills/CustomerSkills/GenerateDownloadLinks/generateDownloadLinks.yml",
                                                    SemanticKernelProvider.class,
                                                    EmbeddedResourceLoader.ResourceLocation.CLASSPATH_ROOT
                                            ),
                                            new HandlebarsPromptTemplateFactory())
                            )
                    )
            );

            builder.withPlugin(
                    KernelPluginFactory.createFromObject(new CustomersPlugin(
                            customers,
                            this,
                            customerDataStore
                    ), "CustomerSkills")
            );

            builder.withPlugin(
                    KernelPluginFactory.createFromFunctions(
                            "Language",
                            "StatementType",
                            List.of(
                                    KernelFunctionYaml.fromPromptYaml(
                                            EmbeddedResourceLoader.readFile(
                                                    "skills/Language/StatementType/statementType.yml",
                                                    SemanticKernelProvider.class,
                                                    EmbeddedResourceLoader.ResourceLocation.CLASSPATH_ROOT
                                            ),
                                            new HandlebarsPromptTemplateFactory())
                            )
                    )
            );
            builder.withPlugin(KernelPluginFactory.createFromObject(customerController, "CustomerController"));
            builder.withPlugin(KernelPluginFactory.createFromObject(new Emailer(), "Emailer"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return builder;
    }

    public synchronized VectorStore getLocalMemoryStore() {
        //return new VolatileMemoryStore();

        if (vectorStore == null) {
            try {

                DataSource datasource = getDataSource();
                vectorStore = JDBCVectorStore.builder()
                        .withDataSource(datasource)
                        .withOptions(
                                JDBCVectorStoreOptions.builder()
                                        .withQueryProvider(HSQLDBVectorStoreQueryProvider.builder()
                                                .withDataSource(datasource)
                                                .build()
                                        )
                                        .build()
                        )
                        .build();

                return vectorStore;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return vectorStore;
    }

    private synchronized static DataSource getDataSource() throws Exception {
        if (datasource != null) {
            return datasource;
        }

        Properties properties = new Properties();
        properties.putAll(
                Map.of(
                        "url", "jdbc:hsqldb:file:/tmp/testdb;sql.syntax_mys=true",
                        "user", "SA",
                        "password", ""
                )
        );

        datasource = JDBCDataSourceFactory.createDataSource(properties);
        return datasource;
    }

    public CustomerController getCustomerController() {
        return customerController;
    }

    public synchronized VectorStore getMemoryStore() {
        if (memoryStore != null) {
            return memoryStore;
        }

        try {
            HSQLDBVectorStoreQueryProvider queryProvider = HSQLDBVectorStoreQueryProvider.builder()
                    .withDataSource(getDataSource())
                    .setDefaultVarCharLength(4096)
                    .build();

            memoryStore = JDBCVectorStore.builder()
                    .withDataSource(getDataSource())
                    .withOptions(JDBCVectorStoreOptions.builder()
                            .withQueryProvider(queryProvider)
                            .build())
                    .build();

            return memoryStore;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public OpenAITextEmbeddingGenerationService getEmbedding() {
        return OpenAITextEmbeddingGenerationService.builder()
                .withOpenAIAsyncClient(getOpenAIAsyncClient())
                .withModelId("text-embedding-3-large")
                .withDimensions(1536)
                .build();
    }
}
