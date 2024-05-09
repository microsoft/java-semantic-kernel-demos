package com.microsoft.semantickernel.sample.java.sk.assistant.datastore;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Arrays;
import java.util.List;

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
    private List<String> rules;

    public Rules() {
        setRules("""
                When a spell is cast, the casters spells available is reduced by 1.
                When a player gives an item, the item removed from the inventory of the player who gave it.
                When a player receives an item, log this in the world log.
                When a player performs an action, log it in the world log.
                When a player picks up an item, it is added to their inventory.
                Potions are not spells and using them should not change a players spell count.
                A weapon is not a potion.
                Weapons are not spells and using them should not change a players spell count.
                When a player uses or attacks with a weapon DO NOT remove the weapon from their inventory.
                When a player takes damage, check if they have fallen unconscious and log it.
                When a player drinks a potion it is removed from their inventory
                """.stripIndent());
    }

    public List<String> getRules() {
        return rules;
    }

    public void setRules(String rules) {
        this.rules = Arrays.stream(rules.split("\n"))
                .map(String::trim)
                .toList();
    }
}
