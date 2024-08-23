package com.microsoft.semantickernel.sample.java.sk.assistant.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class QueryResult {
    private String result;
    private final String documents;

    @JsonCreator
    public QueryResult(
            @JsonProperty("result") String result, @JsonProperty("documents") String documents) {
        this.result = result;
        this.documents = documents;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getDocuments() {
        return documents;
    }
}
