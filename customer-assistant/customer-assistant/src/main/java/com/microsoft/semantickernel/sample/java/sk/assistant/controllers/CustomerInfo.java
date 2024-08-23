package com.microsoft.semantickernel.sample.java.sk.assistant.controllers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.semantickernel.data.recordattributes.VectorStoreRecordDataAttribute;
import com.microsoft.semantickernel.data.recordattributes.VectorStoreRecordKeyAttribute;
import com.microsoft.semantickernel.data.recordattributes.VectorStoreRecordVectorAttribute;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

public class CustomerInfo {

    private static final int EMBEDDING_DIMENSIONS = 1536;

    @VectorStoreRecordDataAttribute(storageName = "info", hasEmbedding = true, embeddingFieldName = "embedding")
    private final String info;

    @VectorStoreRecordKeyAttribute(storageName = "id")
    private final String id;

    @VectorStoreRecordVectorAttribute(dimensions = EMBEDDING_DIMENSIONS * 15, indexKind = "")
    private final List<Float> embedding;

    @JsonCreator
    public CustomerInfo(
            @JsonProperty("id") String id,
            @JsonProperty("info") String info,
            @JsonProperty("embedding") List<Float> embedding) {
        this.id = id;
        this.info = info;
        this.embedding = embedding;
    }

    public CustomerInfo(String info,
                        List<Float> embedding) {
        this.info = info;
        id = getId(info);
        this.embedding = embedding;
    }


    public static String getId(String fact) {
        String id;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            id = HexFormat.of().formatHex(digest.digest(fact.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return id;
    }

    public String getInfo() {
        return info;
    }

    public String getId() {
        return id;
    }
}
