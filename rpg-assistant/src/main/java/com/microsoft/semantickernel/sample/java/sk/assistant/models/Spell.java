package com.microsoft.semantickernel.sample.java.sk.assistant.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Spell {

    private final EffectType effect;
    private final int duration;
    private final int amount;

    @JsonCreator
    public Spell(
            @JsonProperty(value = "amount", defaultValue = "0")
            int amount,
            @JsonProperty(value = "duration", defaultValue = "0")
            int duration,
            @JsonProperty("effect")
            EffectType effect) {
        this.amount = amount;
        this.duration = duration;
        this.effect = effect;
    }

    public int getAmount() {
        return amount;
    }

    public EffectType getEffect() {
        return effect;
    }

    public enum EffectType {
        HEAL,
        DAMAGE,
        DISABLE
    }

}
