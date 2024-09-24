package com.microsoft.semantickernel.sample.java.sk.assistant.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.semantickernel.data.recordattributes.VectorStoreRecordDataAttribute;
import com.microsoft.semantickernel.data.recordattributes.VectorStoreRecordKeyAttribute;
import com.microsoft.semantickernel.sample.java.sk.assistant.controllers.CustomerInfo;

import java.util.Objects;

public final class Rule {
    @VectorStoreRecordKeyAttribute()
    private final String id;
    @VectorStoreRecordDataAttribute()
    private final String rule;

    @JsonCreator
    public Rule(
            @JsonProperty("id") String id,
            @JsonProperty("rule") String rule) {
        this.id = id;
        this.rule = rule;
    }

    public Rule(String rule) {
        this(CustomerInfo.getId(rule), rule);
    }

    public String id() {
        return id;
    }

    public String rule() {
        return rule;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Rule) obj;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.rule, that.rule);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, rule);
    }

    @Override
    public String toString() {
        return "Rule[" +
                "id=" + id + ", " +
                "rule=" + rule + ']';
    }


}
