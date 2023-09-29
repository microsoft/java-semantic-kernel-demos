package com.microsoft.semantickernel.sample.java.sk.assistant.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public record Notes(List<String> notes) {
    @JsonCreator
    public Notes(
            @JsonProperty("notes")
            List<String> notes) {
        this.notes = new ArrayList<>(notes);
    }
}
