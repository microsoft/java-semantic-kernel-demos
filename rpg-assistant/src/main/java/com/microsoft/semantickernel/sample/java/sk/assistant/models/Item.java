package com.microsoft.semantickernel.sample.java.sk.assistant.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class Item {

    private final int damage;
    private final int cost;
    private final String description;
    private final ItemType type;
    private final Map<EffectType, Integer> effects;


    public enum EffectType {
        ILLUMINATION,
        HEAL,
        DAMAGE,
        DAMAGEREDUCTION
    }

    public enum ItemType {
        WEAPON,
        ARMOUR,
        CONSUMABLE
    }

    @JsonCreator
    public Item(
            @JsonProperty("damage")
            int damage,
            @JsonProperty("cost")
            int cost,
            @JsonProperty("description")
            String description,
            @JsonProperty("type")
            ItemType type,
            @JsonProperty("effects")
            Map<EffectType, Integer> effects) {
        this.damage = damage;
        this.cost = cost;
        this.description = description;
        this.type = type;
        this.effects = effects;
    }

    public Integer getEffect(EffectType type) {
        return effects.get(type);
    }

}
