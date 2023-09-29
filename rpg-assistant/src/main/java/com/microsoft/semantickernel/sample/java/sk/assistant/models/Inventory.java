package com.microsoft.semantickernel.sample.java.sk.assistant.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class Inventory {

    private final Map<String, Integer> items;

    @JsonCreator
    public Inventory(
            @JsonProperty("items")
            Map<String, Integer> items) {
        this.items = new HashMap<>(items);
    }

    public Map<String, Integer> getItems() {
        return items;
    }
}
