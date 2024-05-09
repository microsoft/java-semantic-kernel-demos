package com.microsoft.semantickernel.sample.java.sk.assistant.settings;

import com.microsoft.semantickernel.sample.java.sk.assistant.datastore.azure.AzureCognitiveSearchMemoryStore;
import com.microsoft.semantickernel.sample.java.sk.assistant.datastore.azure.DefaultSemanticTextMemory;
import com.microsoft.semantickernel.sample.java.sk.assistant.datastore.azure.EmbeddingGeneration;
import com.microsoft.semantickernel.sample.java.sk.assistant.datastore.azure.MemoryStore;
import com.microsoft.semantickernel.sample.java.sk.assistant.datastore.azure.SemanticTextMemory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

@ApplicationScoped
public class MemoryStoreProvider {

    private final String cognitiveSearchEndpoint;
    private final String cognitiveSearchToken;
    private final EmbeddingGeneration<String> embeddingGeneration;

    public MemoryStoreProvider(
            @ConfigProperty(name = "cognitivesearch.endpoint")
            Optional<String> cognitiveSearchEndpoint,
            @ConfigProperty(name = "cognitivesearch.token")
            Optional<String> cognitiveSearchToken,
            EmbeddingGeneration<String> embeddingGeneration
    ) {
        this.cognitiveSearchEndpoint = cognitiveSearchEndpoint.orElse(null);
        this.cognitiveSearchToken = cognitiveSearchToken.orElse(null);
        this.embeddingGeneration = embeddingGeneration;
    }


    @ApplicationScoped
    @Produces
    public AzureCognitiveSearchMemoryStore getAzureCognitiveSearchMemoryStore() {
        return new AzureCognitiveSearchMemoryStore(
                cognitiveSearchEndpoint,
                cognitiveSearchToken
        );
    }

    @ApplicationScoped
    @Produces
    public SemanticTextMemory getSemanticTextMemory(
            MemoryStore memoryStore
    ) {
        return new DefaultSemanticTextMemory(
                memoryStore,
                embeddingGeneration);
    }
}
