package com.microsoft.semantickernel.sample.java.sk.assistant.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record Log(List<LogEvent> log) {

    @JsonCreator
    public Log(
            @JsonProperty("log")
            List<LogEvent> log) {
        this.log = new ArrayList<>(log);
    }

    public Log() {
        this(new ArrayList<>());
    }

    public Log addEvent(String statement) {
        log.add(new LogEvent(Instant.now(), statement));
        return this;
    }

    public Log addEvent(Instant time, String statement) {
        log.add(new LogEvent(time, statement));
        return this;
    }
}
