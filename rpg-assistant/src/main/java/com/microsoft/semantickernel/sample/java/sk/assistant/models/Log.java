package com.microsoft.semantickernel.sample.java.sk.assistant.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Log {
    private final List<String> log;

    @JsonCreator
    public Log(
            @JsonProperty("log") List<String> log) {
        this.log = log;
    }

    public List<String> getLog() {
        return log;
    }

    public void addLog(String statement) {
        log.add(statement);
    }

}
