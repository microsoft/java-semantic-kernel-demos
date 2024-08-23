package com.microsoft.semantickernel.sample.java.sk.assistant.models;

import com.microsoft.semantickernel.data.recordattributes.VectorStoreRecordDataAttribute;
import com.microsoft.semantickernel.data.recordattributes.VectorStoreRecordKeyAttribute;

import java.time.Instant;

import static com.microsoft.semantickernel.sample.java.sk.assistant.controllers.CustomerInfo.getId;

public record LogEvent(
        @VectorStoreRecordKeyAttribute(storageName = "id")
        String id,
        @VectorStoreRecordDataAttribute(storageName = "timestamp")
        Instant timestamp,
        @VectorStoreRecordDataAttribute(storageName = "info", hasEmbedding = true, embeddingFieldName = "embedding")
        String event) {

    LogEvent(Instant timestamp, String event) {
        this(getId(event), timestamp, event);
    }
}