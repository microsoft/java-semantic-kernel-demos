package com.microsoft.semantickernel.sample.java.sk.assistant.datastore;

import com.microsoft.semantickernel.exceptions.ConfigurationException;
import com.microsoft.semantickernel.sample.java.sk.assistant.SemanticKernelProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import reactor.core.publisher.Mono;

/*
After a spell is cast, the casters spells available is reduced by 1.
Before a spell is cast, check that the caster has an available spell and enter a log into the world log.
When a player gives an item, the item removed from the inventory of the player who gave it.
When a player receives an item, log this in the players log.
When a player performs an action, log it in the players log.
When a player picks up an item, it is added to their inventory.
Potions are not spells and using them should not change a players spell count.
A weapon is not a potion.
When a player uses a weapon DO NOT remove the item from their inventory.
When a player takes damage, check if they have fallen unconscious and enter it into the world log.


When a spell is cast, the casters spells available is reduced by 1.
When a player gives an item, the item removed from the inventory of the player who gave it.
When a player receives an item, log this in the players log.
When a player performs an action, log it in the players log.
When a player picks up an item, it is added to their inventory.
Potions are not spells and using them should not change a players spell count.
When a player drinks a potion, it is removed from their inventory.


When a spell is cast, the casters spells available is reduced by 1.
When a player gives an item, the item removed from the inventory of the player who gave it.
When a player receives an item, log this in the players log.
When a player performs an action, log it in the players log.
When a player picks up an item, it is added to their inventory.
Potions are not spells and using them should not change a players spell count.
A weapon is not a potion.
When a player uses a weapon DO NOT remove the item from their inventory.
When a player takes damage, check if they have fallen unconscious and log it.

 */

/**
 * Defines rules that are used in prompts to control how the world functions.
 */
@ApplicationScoped
public class Rules {
    public static final String COLLECTION_NAME = "rpg-rules";
    public static final String KEY = "rpg-rules";
    private final SemanticKernelProvider semanticKernelProvider;

    @Inject
    public Rules(SemanticKernelProvider semanticKernelProvider) {
        this.semanticKernelProvider = semanticKernelProvider;
    }

    public Mono<String> getRules() {
        try {
            return semanticKernelProvider
                    .getEmbeddingKernel()
                    .flatMap(kernel -> {
                        return kernel
                                .getMemory()
                                .getAsync(COLLECTION_NAME, KEY, false);
                    })
                    .map(it -> it.getMetadata().getText());
        } catch (ConfigurationException e) {
            return Mono.error(e);
        }
    }

    public void setRules(String rules) throws ConfigurationException {
        semanticKernelProvider
                .getEmbeddingKernel()
                .flatMap(kernel -> kernel
                        .getMemory()
                        .saveInformationAsync(COLLECTION_NAME, rules, KEY, null, null))
                .subscribe();
    }
}
