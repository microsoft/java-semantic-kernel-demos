// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.sample.java.sk.assistant.datastore.azure;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.microsoft.semantickernel.builders.SemanticKernelBuilder;
import com.microsoft.semantickernel.services.AIService;
import reactor.core.publisher.Mono;

import java.util.List;

/** Interface for text embedding generation services */
public interface EmbeddingGeneration<TValue> extends AIService {
    /**
     * Generates a list of embeddings associated to the data
     *
     * @param data List of texts to generate embeddings for
     * @return List of embeddings of each data point
     */
    Mono<List<Embedding>> generateEmbeddingsAsync(List<TValue> data);

    interface Builder<T, E extends EmbeddingGeneration<T>> extends SemanticKernelBuilder<E> {

        Builder<T, E> withOpenAIClient(OpenAIAsyncClient client);

        Builder<T, E> withModelId(String modelId);
    }
}