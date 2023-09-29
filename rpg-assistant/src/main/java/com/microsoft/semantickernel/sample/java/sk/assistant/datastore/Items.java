package com.microsoft.semantickernel.sample.java.sk.assistant.datastore;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Item;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is used to load the items.json file into a map of items.
 * The map is then used to retrieve the item information for the item
 * that the user has selected.
 */
@ApplicationScoped
public class Items {
    private final Map<String, Item> items;

    public Items() {
        items = new HashMap<>();
    }

    public Items(Map<String, Item> items) {
        this.items = items;
    }

    @Startup
    public void loadItems() {
        TypeReference<HashMap<String, Item>> typeRef = new TypeReference<>() {
        };

        try (InputStream is = Items.class.getResourceAsStream("items.json")) {
            items.putAll(new ObjectMapper().readValue(is, typeRef));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Item> getItems() {
        return items;
    }
}
