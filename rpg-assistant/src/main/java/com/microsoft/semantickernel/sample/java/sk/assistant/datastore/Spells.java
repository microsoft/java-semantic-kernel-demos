package com.microsoft.semantickernel.sample.java.sk.assistant.datastore;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Spell;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides access to all spells available in the game.
 */
@ApplicationScoped
public class Spells {
    private final Map<String, Spell> spells;

    public Spells(Map<String, Spell> spells) {
        this.spells = spells;
    }

    public Spells() {
        spells = new HashMap<>();
    }

    @Startup
    public void loadSpells() {
        TypeReference<HashMap<String, Spell>> typeRef = new TypeReference<>() {
        };

        try (InputStream is = Spells.class.getResourceAsStream("spells.json")) {
            spells.putAll(new ObjectMapper().readValue(is, typeRef));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public Map<String, Spell> getSpells() {
        return spells;
    }
}
