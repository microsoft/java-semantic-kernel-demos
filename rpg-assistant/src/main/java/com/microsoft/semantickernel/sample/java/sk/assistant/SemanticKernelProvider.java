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
import com.microsoft.semantickernel.sample.java.sk.assistant.datastore.PlayerController;
import com.microsoft.semantickernel.textcompletion.TextCompletion;
import com.microsoft.semantickernel.util.EmbeddedResourceLoader;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import reactor.core.publisher.Mono;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * A provider of semantic kernels for use within the application.
 */
@ApplicationScoped
public class SemanticKernelProvider {
    private final PlayerController playerController;
    private final String cognativeSearchEndpoint;
    private final String cognativeSearchToken;
    private final String dbConnectionString;
    private final String model;
    private final String embeddingModel;
    private CompletableFuture<? extends MemoryStore> memory;

    @Inject
    public SemanticKernelProvider(
            PlayerController playerController,

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
        this.dbConnectionString = dbConnectionString;

        this.playerController = playerController;
    }


    private Mono<Kernel.Builder> createKernelBuilder(KernelType kernelType) throws ConfigurationException {
        return getEmbeddingKernelBuilder(kernelType);
    }

    private OpenAIAsyncClient getOpenAIAsyncClient() throws IOException, ConfigurationException {

        RetryOptions retryOptions = new RetryOptions(new FixedDelayOptions(2, Duration.ofSeconds(10)));

        AzureOpenAISettings settings = new AzureOpenAISettings(SettingsMap.getDefault());

        return new OpenAIClientBuilder()
                .clientOptions(getHttpClientOptions())
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
                            512
                    ),
                    prompt,
                    true
            );
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Mono<Kernel> getKernelEmpty(KernelType kernelType) {
        try {
            return createKernelBuilder(kernelType)
                    .map(SemanticKernelBuilder::build);
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public Mono<Kernel> getKernel(KernelType kernelType) {
        try {
            return createKernelBuilder(kernelType)
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
        kernel.importSkillFromResources("skills", "RPGSkills", "QueryWorld");
        kernel.importSkillFromResources("skills", "Language", "StatementType");
        kernel.importSkill(playerController, "PlayerController");
    }

    public Mono<Kernel> getEmbeddingKernel() throws ConfigurationException {
        return getEmbeddingKernel(KernelType.QUERY);
    }

    public Mono<Kernel> getEmbeddingKernel(KernelType kernelType) throws ConfigurationException {
        return getEmbeddingKernelBuilder(kernelType)
                .map(SemanticKernelBuilder::build);
    }

    public Kernel helloWorldKernel() throws ConfigurationException {
        // Create a memory for the kernel to store information
        var memory = SKBuilders.semanticTextMemory().build();

        // Load Open ai settings from the configuration file/properties/environment
        AzureOpenAISettings settings = new AzureOpenAISettings(SettingsMap.getDefault());

        // Create an OpenAI client
        OpenAIAsyncClient openAiClient = new OpenAIClientBuilder()
                .endpoint(settings.getEndpoint())
                .credential(new AzureKeyCredential(settings.getKey()))
                .buildAsyncClient();

        // Create a text completion service
        TextCompletion completion = SKBuilders.textCompletion()
                .withOpenAIClient(openAiClient)
                .withModelId(model)
                .build();

        // Create a kernel
        return SKBuilders
                .kernel()
                .withMemory(memory)
                .withDefaultAIService(completion)
                .build();
    }

    public Mono<Kernel.Builder> getEmbeddingKernelBuilder(KernelType kernelType) throws ConfigurationException {

        RetryOptions retryOptions = new RetryOptions(new FixedDelayOptions(2, Duration.ofSeconds(10)));

        AzureOpenAISettings settings = new AzureOpenAISettings(SettingsMap.getDefault());

        OpenAIAsyncClient client = new OpenAIClientBuilder()
                .clientOptions(getHttpClientOptions())
                .retryOptions(retryOptions)
                .endpoint(settings.getEndpoint())
                .credential(new AzureKeyCredential(settings.getKey()))
                .buildAsyncClient();

        TextCompletion completion;

        switch (kernelType) {
            case PLANNER -> completion = SKBuilders.textCompletion()
                    .withOpenAIClient(client)
                    .withModelId("text-davinci-003")
                    .build();
            default -> completion = SKBuilders.chatCompletion()
                    .withOpenAIClient(client)
                    .withModelId(model)
                    .build();
        }

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
                            .withDefaultAIService(completion)
                            .withMemory(memoryBuilder.build());
                });
    }

    private static HttpClientOptions getHttpClientOptions() {
        return new HttpClientOptions()
                .setReadTimeout(Duration.ofSeconds(10))
                .setResponseTimeout(Duration.ofSeconds(10));
    }

    public synchronized Mono<? extends MemoryStore> getMemoryStore() {
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
                .withStorage(new AzureCognitiveSearchMemoryStore(
                        cognativeSearchEndpoint,
                        cognativeSearchToken
                ))
                .build();
    }

}
