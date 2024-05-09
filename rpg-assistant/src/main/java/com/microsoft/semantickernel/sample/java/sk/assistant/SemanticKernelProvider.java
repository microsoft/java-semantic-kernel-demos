package com.microsoft.semantickernel.sample.java.sk.assistant;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.http.policy.FixedDelayOptions;
import com.azure.core.http.policy.RetryOptions;
import com.azure.core.util.HttpClientOptions;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.exceptions.ConfigurationException;
import com.microsoft.semantickernel.implementation.EmbeddedResourceLoader;
import com.microsoft.semantickernel.plugin.KernelPlugin;
import com.microsoft.semantickernel.plugin.KernelPluginFactory;
import com.microsoft.semantickernel.sample.java.sk.assistant.datastore.PlayerController;
import com.microsoft.semantickernel.sample.java.sk.assistant.utilities.rpg.WorldAction;
import com.microsoft.semantickernel.semanticfunctions.HandlebarsPromptTemplateFactory;
import com.microsoft.semantickernel.semanticfunctions.KernelFunction;
import com.microsoft.semantickernel.semanticfunctions.KernelFunctionYaml;
import com.microsoft.semantickernel.services.audio.AudioToTextService;
import com.microsoft.semantickernel.services.audio.TextToAudioService;
import com.microsoft.semantickernel.services.chatcompletion.ChatCompletionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;

/**
 * A provider of semantic kernels for use within the application.
 */
@ApplicationScoped
public class SemanticKernelProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(SemanticKernelProvider.class);

    private final PlayerController playerController;
    private final WorldAction worldAction;

    private final String model;
    private final OpenAIAsyncClient client;
    private final OpenAIAsyncClient clientAlt;

    @Inject
    public SemanticKernelProvider(
            @ConfigProperty(name = "client.endpoint")
            String clientEndpoint,
            @ConfigProperty(name = "client.key")
            String clientKey,


            @ConfigProperty(name = "client.endpoint.alt")
            String clientEndpointAlt,
            @ConfigProperty(name = "client.key.alt")
            String clientKeyAlt,

            WorldAction worldAction,
            PlayerController playerController,

            @ConfigProperty(name = "chatcompletion.model", defaultValue = "chat")
            String model
    ) {
        this.model = model;

        RetryOptions retryOptions = new RetryOptions(new FixedDelayOptions(2, Duration.ofSeconds(10)));
        client = new OpenAIClientBuilder()
                .clientOptions(getHttpClientOptions())
                .retryOptions(retryOptions)
                .endpoint(clientEndpoint)
                .credential(new AzureKeyCredential(clientKey))
                .buildAsyncClient();


        clientAlt = new OpenAIClientBuilder()
                .clientOptions(getHttpClientOptions())
                .retryOptions(retryOptions)
                .endpoint(clientEndpointAlt)
                .credential(new AzureKeyCredential(clientKeyAlt))
                .buildAsyncClient();

        this.worldAction = worldAction;
        this.playerController = playerController;
    }

    private Kernel.Builder createKernelBuilder(KernelType kernelType) throws ConfigurationException {
        return getEmbeddingKernelBuilder(kernelType);
    }

    @Produces
    @ApplicationScoped
    public OpenAIAsyncClient getOpenAIAsyncClient() {
        return client;
    }

    public Mono<Kernel> getKernelEmpty(KernelType kernelType) {
        try {
            return Mono.just(createKernelBuilder(kernelType).build());
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public Mono<Kernel> getKernel(KernelType kernelType) {
        try {
            Kernel kernel = addSkills(createKernelBuilder(kernelType), kernelType).build();
            kernel.getGlobalKernelHooks().addPostChatCompletionHook(
                    (event) -> {
                        LOGGER.info("Chat completion result: " + event.getChatCompletions().getChoices());
                        return event;
                    }
            );
            return Mono.just(kernel);
        } catch (ConfigurationException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Kernel.Builder addSkills(Kernel.Builder kernelBuilder, KernelType kernelType) throws IOException {

        Kernel.Builder builder = kernelBuilder
                .withPlugin(
                        KernelPluginFactory.importPluginFromResourcesDirectory(
                                "skills",
                                "RPGSkills",
                                "QueryWorld",
                                null)
                )
                .withPlugin(
                        KernelPluginFactory.createFromObject(PlayerController.class, playerController, "PlayerActions")
                )
                .withPlugin(
                        KernelPluginFactory.createFromObject(WorldAction.class, worldAction, "WorldActions")
                );
        KernelPlugin languagePlugin = KernelPluginFactory.importPluginFromResourcesDirectory(
                "skills",
                "Language",
                "StatementType",
                null);
        languagePlugin.addFunction(getExtractActionsFunction());

        if (kernelType == KernelType.DESCRIBE_ACTIONS) {
            languagePlugin.addFunction(getSummarizeActionsTakenFunction());
        }

        builder = builder.withPlugin(languagePlugin);
        return builder;
    }


    public static KernelFunction<String> getSummarizeActionsTakenFunction() {
        try {
            return KernelFunctionYaml.fromPromptYaml(
                    EmbeddedResourceLoader.readFile(
                            "skills/Language/SummarizeActionsTaken/skprompt.yaml",
                            SemanticKernelProvider.class,
                            EmbeddedResourceLoader.ResourceLocation.CLASSPATH_ROOT),
                    new HandlebarsPromptTemplateFactory());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static KernelFunction<String> getExtractActionsFunction() {
        try {
            return KernelFunctionYaml.fromPromptYaml(
                    EmbeddedResourceLoader.readFile(
                            "skills/Language/ExtractActions/skprompt.yaml",
                            SemanticKernelProvider.class,
                            EmbeddedResourceLoader.ResourceLocation.CLASSPATH_ROOT),
                    new HandlebarsPromptTemplateFactory());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static KernelFunction<String> getPerformActionFunction() {
        try {
            return KernelFunctionYaml.fromPromptYaml(
                    EmbeddedResourceLoader.readFile(
                            "skills/RPGSkills/PerformAction/skprompt.yaml",
                            SemanticKernelProvider.class,
                            EmbeddedResourceLoader.ResourceLocation.CLASSPATH_ROOT),
                    new HandlebarsPromptTemplateFactory());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Mono<Kernel> getEmbeddingKernel() throws ConfigurationException {
        return getEmbeddingKernel(KernelType.QUERY);
    }

    public Mono<Kernel> getEmbeddingKernel(KernelType kernelType) throws ConfigurationException {
        return Mono.just(getEmbeddingKernelBuilder(kernelType).build());
    }

    public Kernel.Builder getEmbeddingKernelBuilder(KernelType kernelType) {

        if (kernelType == KernelType.GENERATE) {
            // Create a text completion service
            ChatCompletionService completion = ChatCompletionService.builder()
                    .withOpenAIAsyncClient(clientAlt)
                    .withModelId(model)
                    .build();

            return Kernel.builder()
                    .withAIService(ChatCompletionService.class, completion);
        }

        // Create a text completion service
        ChatCompletionService completion = ChatCompletionService.builder()
                .withOpenAIAsyncClient(client)
                .withModelId(model)
                .build();

        // Create a kernel
        return Kernel.builder()
                .withAIService(ChatCompletionService.class, completion);
    }

    private static HttpClientOptions getHttpClientOptions() {
        return new HttpClientOptions()
                .setReadTimeout(Duration.ofSeconds(30))
                .setResponseTimeout(Duration.ofSeconds(30));
    }

    public AudioToTextService getAudioToTextService() {
        return AudioToTextService
                .builder()
                .withModelId("whisper-1")
                .withOpenAIAsyncClient(client)
                .build();
    }

    public TextToAudioService getTextToAudioService() {

        return TextToAudioService.builder()
                .withModelId("tts-1")
                .withOpenAIAsyncClient(client)
                .build();
    }
}
