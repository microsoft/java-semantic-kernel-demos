package com.microsoft.semantickernel.sample.java.sk.assistant.models;

import java.time.Instant;

public record LogEvent(Instant timestamp, String event) {
}