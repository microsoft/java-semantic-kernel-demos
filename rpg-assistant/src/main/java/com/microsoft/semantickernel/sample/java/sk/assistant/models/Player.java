package com.microsoft.semantickernel.sample.java.sk.assistant.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Player {
    private final String uid;
    private final String name;
    private final int level;
    private final Map<String, Integer> inventory;
    private final Facts facts;
    private final Log log;
    private final List<String> spells;
    private int health;
    private int spellsAvailable;

    @JsonCreator
    public Player(
            @JsonProperty("uid")
            String uid,
            @JsonProperty("name")
            String name,
            @JsonProperty("health")
            int health,
            @JsonProperty("level")
            int level,
            @JsonProperty("spellsAvailable")
            int spellsAvailable,
            @JsonProperty("inventory")
            Map<String, Integer> inventory,
            @JsonProperty("facts")
            Facts facts,
            @JsonProperty("log")
            Log log,
            @JsonProperty("spells")
            List<String> spells) {
        this.uid = uid;
        this.name = name;
        this.health = health;
        this.level = level;
        this.spellsAvailable = spellsAvailable;
        this.inventory = inventory;
        this.facts = facts;
        this.log = log;
        this.spells = spells;
    }

    public String getName() {
        return name;
    }

    public int getHealth() {
        return health;
    }

    public int getLevel() {
        return level;
    }

    public int getSpellsAvailable() {
        return spellsAvailable;
    }

    public void setSpellsAvailable(int i) {
        spellsAvailable = i;
    }

    public Map<String, Integer> getInventory() {
        return Collections.unmodifiableMap(inventory);
    }

    public Facts getFacts() {
        return facts;
    }

    public void addFact(String statement) {
        facts.addFact(statement);
    }

    public void setHeath(int i) {
        health = i;
    }

    public String getUid() {
        return uid;
    }

    public Log getLog() {
        return log;
    }

    public List<String> getSpells() {
        return spells;
    }

    public void removeItemFromInventory(String itemName) {
        Integer count = inventory.get(itemName.toLowerCase());
        if (count != null && count > 0) {
            count = count - 1;
            if (count == 0) {
                inventory.remove(itemName.toLowerCase());
            } else {
                inventory.put(itemName.toLowerCase(), count);
            }
        }
    }

    public void addItemFromInventory(String itemName) {
        Integer count = inventory.get(itemName.toLowerCase());
        if (count == null) {
            count = 0;
        }
        count++;

        inventory.put(itemName.toLowerCase(), count);
    }
}
