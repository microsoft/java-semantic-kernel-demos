package com.microsoft.semantickernel.sample.java.sk.assistant.models;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collections;
import java.util.List;

@ApplicationScoped
public final class Rules {
    private List<Rule> rules;

    public Rules() {
        rules = Collections.emptyList();
    }

    public List<Rule> getRules() {
        return Collections.unmodifiableList(rules);
    }

    public void setRules(List<Rule> rules) {
        this.rules = Collections.unmodifiableList(rules);
    }

}
