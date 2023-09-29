package com.microsoft.semantickernel.sample.java.sk.assistant;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.http.policy.FixedDelayOptions;
import com.azure.core.http.policy.RetryOptions;
import com.azure.core.util.HttpClientOptions;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.SKBuilders;
import com.microsoft.semantickernel.builders.SemanticKernelBuilder;
import com.microsoft.semantickernel.chatcompletion.ChatCompletion;
import com.microsoft.semantickernel.chatcompletion.ChatHistory;
import com.microsoft.semantickernel.connectors.ai.openai.util.AzureOpenAISettings;
import com.microsoft.semantickernel.connectors.ai.openai.util.SettingsMap;
import com.microsoft.semantickernel.connectors.memory.azurecognitivesearch.AzureCognitiveSearchMemoryStore;
import com.microsoft.semantickernel.connectors.memory.postgresql.PostgreSQLMemoryStore;
import com.microsoft.semantickernel.exceptions.ConfigurationException;
import com.microsoft.semantickernel.memory.MemoryStore;
import com.microsoft.semantickernel.memory.SemanticTextMemory;
import com.microsoft.semantickernel.planner.sequentialplanner.SequentialPlanner;
import com.microsoft.semantickernel.planner.sequentialplanner.SequentialPlannerRequestSettings;
import com.microsoft.semantickernel.planner.stepwiseplanner.DefaultStepwisePlanner;
import com.microsoft.semantickernel.planner.stepwiseplanner.StepwisePlanner;
import com.microsoft.semantickernel.sample.java.sk.assistant.controllers.CustomerController;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Customers;
import com.microsoft.semantickernel.sample.java.sk.assistant.skills.Emailer;
import com.microsoft.semantickernel.sample.java.sk.assistant.utilities.Rules;
import com.microsoft.semantickernel.util.EmbeddedResourceLoader;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.FileNotFoundException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * A factory class that constructs kernels for use by the application
 */
@ApplicationScoped
public class SemanticKernelProvider {
    private final CustomerController customerController;

    private final String model;
    private final String cognativeSearchEndpoint;
    private final String cognativeSearchToken;
    private final Customers customers;
    private final String dbConnectionString;
    private final String embeddingModel;
    private CompletableFuture<? extends MemoryStore> memory;

    @Inject
    public SemanticKernelProvider(
            Customers customers,
            Rules rules,

            @ConfigProperty(name = "chatcompletion.model", defaultValue = "gpt-35-turbo")
            String model,
            @ConfigProperty(name = "embedding.model", defaultValue = "text-embedding-ada-002")
            String embeddingModel,
            @ConfigProperty(name = "cognativesearch.endpoint")
            Optional<String> cognativeSearchEndpoint,
            @ConfigProperty(name = "cognativesearch.token")
            Optional<String> cognativeSearchToken,
            @ConfigProperty(name = "db.connectionString")
            String dbConnectionString
    ) {
        this.model = model;
        this.embeddingModel = embeddingModel;
        this.cognativeSearchEndpoint = cognativeSearchEndpoint.orElse(null);
        this.cognativeSearchToken = cognativeSearchToken.orElse(null);
        this.customers = customers;
        this.dbConnectionString = dbConnectionString;

        this.customerController = new CustomerController(customers, this, rules);
    }

    public void init() {
        Flux.fromIterable(customers.getCustomers())
                .concatMap(customerController::saveCustomerFacts)
                .blockLast();
    }

    private Mono<Kernel.Builder> getACSBuilder() throws ConfigurationException {
        final Kernel.Builder kernelBuilder;
        OpenAIAsyncClient client = getOpenAIAsyncClient();
        SemanticTextMemory semanticTextMemory = getAzureCognitiveSearchMemory();

        kernelBuilder = SKBuilders.kernel()
                .withDefaultAIService(SKBuilders
                        .chatCompletion()
                        .withOpenAIClient(client)
                        .withModelId(model)
                        .build())
                .withMemory(semanticTextMemory);

        return Mono.just(kernelBuilder);
    }

    private OpenAIAsyncClient getOpenAIAsyncClient() throws ConfigurationException {

        RetryOptions retryOptions = new RetryOptions(new FixedDelayOptions(2, Duration.ofSeconds(10)));

        AzureOpenAISettings settings = new AzureOpenAISettings(SettingsMap.getDefault());

        return new OpenAIClientBuilder()
                .clientOptions(new HttpClientOptions()
                        .setReadTimeout(Duration.ofSeconds(10))
                        .setResponseTimeout(Duration.ofSeconds(10)))
                .retryOptions(retryOptions)
                .endpoint(settings.getEndpoint())
                .credential(new AzureKeyCredential(settings.getKey()))
                .buildAsyncClient();
    }

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

    public Mono<Kernel> getKernelEmpty() {
        try {
            return getEmbeddingKernelBuilder()
                    .map(SemanticKernelBuilder::build);
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public Mono<Kernel> getKernel() {
        try {
            return getEmbeddingKernelBuilder()
                    .map(it -> {
                        Kernel kernel = it.build();
                        addSkills(kernel);
                        return kernel;
                    });
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private void addSkills(Kernel kernel) {
        kernel.importSkillFromResources("skills", "CustomerSkills", "QueryInfo");
        kernel.importSkillFromResources("skills", "CustomerSkills", "GenerateDownloadLinks");
        kernel.importSkillFromResources("skills", "Language", "StatementType");
        kernel.importSkill(customerController, "CustomerController");
        kernel.importSkill(new Emailer(), "Emailer");
    }

    public Mono<Kernel> getEmbeddingKernel() throws ConfigurationException {
        return getEmbeddingKernelBuilder()
                .map(SemanticKernelBuilder::build);
    }

    public Mono<Kernel.Builder> getEmbeddingKernelBuilder() throws ConfigurationException {
        OpenAIAsyncClient client = getOpenAIAsyncClient();

        return getMemoryStore()
                .map(memoryStore -> {
                    SemanticTextMemory.Builder memoryBuilder = SKBuilders
                            .semanticTextMemory()
                            .withStorage(memoryStore)
                            .withEmbeddingGenerator(
                                    SKBuilders.textEmbeddingGeneration()
                                            .withOpenAIClient(client)
                                            .withModelId(embeddingModel)
                                            .build()
                            );

                    return SKBuilders.kernel()
                            .withDefaultAIService(SKBuilders.chatCompletion()
                                    .withOpenAIClient(client)
                                    .withModelId(model)
                                    .build())
                            .withMemory(memoryBuilder.build());
                });
    }


    public synchronized Mono<? extends MemoryStore> getMemoryStore() {
        //return new VolatileMemoryStore();

        if (memory == null) {
            try {
                memory = new PostgreSQLMemoryStore.Builder()
                        .withConnection(
                                DriverManager.getConnection(dbConnectionString)
                        )
                        .buildAsync()
                        .toFuture();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        return Mono.fromFuture(memory);
    }

    private SemanticTextMemory getAzureCognitiveSearchMemory() {
        return SKBuilders
                .semanticTextMemory()
                .withStorage(
                        new AzureCognitiveSearchMemoryStore(
                                cognativeSearchEndpoint,
                                cognativeSearchToken
                        ))
                .build();
    }

    public ChatCompletion<ChatHistory> getChatCompletion() {
        try {
            return SKBuilders.chatCompletion()
                    .withOpenAIClient(getOpenAIAsyncClient())
                    .withModelId(model)
                    .build();
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
}
