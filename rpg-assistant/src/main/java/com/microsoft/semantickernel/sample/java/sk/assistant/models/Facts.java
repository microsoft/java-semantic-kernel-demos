package com.microsoft.semantickernel.sample.java.sk.assistant.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Facts {
    private final List<String> facts;

    @JsonCreator
    public Facts(
            @JsonProperty("facts")
            List<String> facts) {
        this.facts = new ArrayList<>(facts);
    }

    public List<String> getFacts() {
        return facts;
    }

    public void addFact(String statement) {
        facts.add(statement);
    }

    public void setFacts(String notes) {
        facts.clear();
        facts.addAll(
                Arrays.stream(notes
                                .split("\n"))
                        .map(String::trim)
                        .toList());
    }
}
