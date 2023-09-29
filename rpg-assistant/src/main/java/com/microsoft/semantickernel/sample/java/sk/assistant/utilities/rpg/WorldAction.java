package com.microsoft.semantickernel.sample.java.sk.assistant.utilities.rpg;

import com.microsoft.semantickernel.sample.java.sk.assistant.datastore.Items;
import com.microsoft.semantickernel.sample.java.sk.assistant.datastore.Spells;
import com.microsoft.semantickernel.sample.java.sk.assistant.datastore.WorldLog;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Item;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Spell;
import com.microsoft.semantickernel.skilldefinition.annotations.DefineSKFunction;
import com.microsoft.semantickernel.skilldefinition.annotations.SKFunctionInputAttribute;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Provides skills for interacting with the world.
 */
@ApplicationScoped
public class WorldAction {
    private final WorldLog log;
    private final Items items;
    private final Spells spells;

    @Inject
    public WorldAction(WorldLog log, Items items, Spells spells) {
        this.log = log;
        this.items = items;
        this.spells = spells;
    }

    @DefineSKFunction(
            name = "getWeaponDamage",
            description = "Given a weapon name return its damage.",
            returnDescription = "The damage the weapon does."
    )
    public String getWeaponDamage(
            @SKFunctionInputAttribute(
                    description = "The name of the weapon."
            )
            String weaponName
    ) {
        Item weapon = items.getItems().get(weaponName.toLowerCase());

        if (weapon != null) {
            Integer dmg = weapon.getEffect(Item.EffectType.DAMAGE);
            if (dmg == null) {
                return "0";
            }
            return dmg.toString();
        } else
            return "0";
    }


    @DefineSKFunction(
            name = "getSpellDamage",
            description = "Given a spell name return its damage.",
            returnDescription = "The damage the spell does."
    )
    public String getSpellDamage(
            @SKFunctionInputAttribute(
                    description = "The name of the spell."
            )
            String spellName
    ) {

        Spell spell = spells.getSpells().get(spellName.toLowerCase());
        if (spell.getEffect() == Spell.EffectType.DAMAGE) {
            return String.valueOf(spell.getAmount());
        }
        return "0";
    }

    @DefineSKFunction(
            name = "addLogEntry",
            description = "Adds an event to to world log."
    )
    public void addLog(
            @SKFunctionInputAttribute(
                    description = "The log entry"
            )
            String logEntry
    ) {
        log.addLog(logEntry);
    }

}
