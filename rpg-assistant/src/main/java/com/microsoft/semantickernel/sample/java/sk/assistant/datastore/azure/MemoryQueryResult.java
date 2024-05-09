// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.sample.java.sk.assistant.datastore.azure;

import javax.annotation.Nonnull;

/** Copy of metadata associated with a memory entry. */
public class MemoryQueryResult {
    /**
     * Whether the source data used to calculate embeddings are stored in the local storage provider
     * or is available through an external service, such as website, MS Graph, etc.
     */
    @Nonnull private final MemoryRecordMetadata metadata;

    /** Search relevance, from 0 to 1, where 1 means perfect match. */
    private final double relevance;

    /**
     * Create a new instance of MemoryQueryResult
     *
     * @param metadata Whether the source data used to calculate embeddings are stored in the local
     *     storage provider or is available through an external service, such as website, MS Graph,
     *     etc.
     * @param relevance Search relevance, from 0 to 1, where 1 means perfect match.
     */
    public MemoryQueryResult(@Nonnull MemoryRecordMetadata metadata, double relevance) {
        this.metadata = metadata;
        this.relevance = clampRelevance(relevance);
    }

    public MemoryRecordMetadata getMetadata() {
        return metadata;
    }

    public double getRelevance() {
        return relevance;
    }

    // function to clamp relevance to [0, 1]
    private static double clampRelevance(double relevance) {
        return !Double.isNaN(relevance) ? Math.max(0, Math.min(1, relevance)) : 0d;
    }
}