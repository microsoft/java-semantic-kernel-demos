// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.sample.java.sk.assistant.datastore.azure;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.models.EmbeddingItem;
import com.azure.ai.openai.models.Embeddings;
import com.azure.ai.openai.models.EmbeddingsOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class OpenAITextEmbeddingGeneration implements EmbeddingGeneration<String> {
    private final OpenAIAsyncClient client;
    private final String modelId;

    @Inject
    public OpenAITextEmbeddingGeneration(
            OpenAIAsyncClient client,

            @ConfigProperty(name = "embedding.model", defaultValue = "embedding")
            String embeddingModel) {
        this.client = client;
        this.modelId = embeddingModel;
    }

    @Override
    public Mono<List<Embedding>> generateEmbeddingsAsync(List<String> data) {
        return this.internalGenerateTextEmbeddingsAsync(data);
    }

    protected Mono<List<Embedding>> internalGenerateTextEmbeddingsAsync(List<String> data) {
        EmbeddingsOptions options =
                new EmbeddingsOptions(data).setModel(getModelId()).setUser("default")
                        .setInputType("text");

        return client
                .getEmbeddings(getModelId(), options)
                .doOnError(e -> {
                    e.toString();
                })
                .flatMapIterable(Embeddings::getData)
                .mapNotNull(EmbeddingItem::getEmbedding)
                .map(
                        embedding -> embedding.stream()
                                .collect(Collectors.toList()))
                .mapNotNull(Embedding::new)
                .collectList();
    }

    @Nullable
    @Override
    public String getModelId() {
        return modelId;
    }

    @Nullable
    @Override
    public String getServiceId() {
        return null;
    }
}