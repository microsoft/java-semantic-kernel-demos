package com.microsoft.semantickernel.sample.java.sk.assistant.datastore;

import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.SKBuilders;
import com.microsoft.semantickernel.memory.MemoryQueryResult;
import com.microsoft.semantickernel.memory.SemanticTextMemory;
import com.microsoft.semantickernel.orchestration.SKContext;
import com.microsoft.semantickernel.orchestration.SKFunction;
import com.microsoft.semantickernel.planner.actionplanner.Plan;
import com.microsoft.semantickernel.planner.sequentialplanner.SequentialPlanner;
import com.microsoft.semantickernel.planner.stepwiseplanner.DefaultStepwisePlanner;
import com.microsoft.semantickernel.planner.stepwiseplanner.StepwisePlannerConfig;
import com.microsoft.semantickernel.sample.java.sk.assistant.KernelType;
import com.microsoft.semantickernel.sample.java.sk.assistant.SemanticKernelProvider;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Player;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Spell;
import com.microsoft.semantickernel.sample.java.sk.assistant.utilities.rpg.WorldAction;
import com.microsoft.semantickernel.skilldefinition.annotations.DefineSKFunction;
import com.microsoft.semantickernel.skilldefinition.annotations.SKFunctionInputAttribute;
import com.microsoft.semantickernel.skilldefinition.annotations.SKFunctionParameters;
import io.quarkus.arc.ClientProxy;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides functionality and skills for retrieving and interacting with the player data.
 */
@ApplicationScoped
public class PlayerController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerController.class);
    public static final String I_DON_T_KNOW = "I don't know";
    public static final int FACT_LIMIT = 10;
    public static final String FACT_PREFIX = "FACT: ";
    private final Players players;
    private final SemanticKernelProvider semanticKernelProvider;
    private final Rules rules;
    private final WorldAction worldAction;
    private final Spells spells;

    @Inject
    public PlayerController(
            Players players,
            SemanticKernelProvider semanticKernelProvider,
            Rules rules,
            WorldAction worldAction,
            Spells spells) {
        this.players = players;
        this.semanticKernelProvider = semanticKernelProvider;
        this.rules = rules;
        this.worldAction = worldAction;
        this.spells = spells;
    }

    @Startup
    public void init() {
        // Load existing data about the players from the database, and update with the latest
        Flux.fromIterable(players.getPlayers())
                .concatMap(player -> {
                    return loadPlayerFacts(player)
                            .map(notes -> {
                                player.getFacts().setFacts(notes);
                                return player;
                            })
                            .defaultIfEmpty(player);
                })
                .concatMap(this::savePlayerData)
                .blockLast();
    }

    // Ids are set to the SHA-256 hash of the fact, not sensitive data so not concerned with predictability/collisions
    public static String getId(String fact) {
        String id;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            id = Base64.getEncoder().encodeToString(digest.digest(fact.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return id;
    }

    public Mono<String> savePlayerData(Player player) {
        return semanticKernelProvider
                .getKernel(KernelType.QUERY)
                .map(Kernel::getMemory)
                .flatMap(memory -> {
                    return savePlayerEvents(player, memory)
                            .then(savePlayerFacts(player, memory))
                            .then(Mono.just("Done"));
                });
    }

    public Mono<String> loadPlayerFacts(Player player) {
        return semanticKernelProvider
                .getKernel(KernelType.QUERY)
                .map(Kernel::getMemory)
                .flatMap(memory -> {
                    return memory.getAsync(
                                    getAllInfoCollectionName(player),
                                    "facts",
                                    false
                            )
                            .map(it -> it.getMetadata().getText());
                });
    }

    private static Mono<Void> savePlayerFacts(Player player, SemanticTextMemory memory) {
        Flux
                .fromIterable(player.getFacts().getFacts())
                .reduce((accumulator, newData) -> accumulator + "\n" + newData)
                .subscribe(facts -> {
                    memory.saveInformationAsync(
                                    getAllInfoCollectionName(player),
                                    facts,
                                    "facts",
                                    null,
                                    null
                            )
                            .subscribe();
                });

        return Flux.fromIterable(player.getFacts().getFacts())
                .concatMap(fact -> {
                    return savePlayerInfo(fact, memory, player, FACT_PREFIX);
                })
                .last()
                .then();
    }

    private static Mono<String> savePlayerInfo(String info, SemanticTextMemory memory, Player player, String entryPrefix) {
        String id = getId(info);

        return memory
                .saveInformationAsync(
                        getCollectionName(player),
                        entryPrefix + info,
                        id,
                        null,
                        null
                );
    }

    private static Mono<Void> savePlayerEvents(Player player, SemanticTextMemory memory) {
        return Flux
                .fromIterable(player.getLog().getLog())
                .concatMap(logEvent -> {
                    return savePlayerInfo(logEvent, memory, player, "EVENT: ");
                })
                .last()
                .then();
    }

    public Mono<String> answerQuestion(Player player, String query) {
        return askQueryAboutPlayer(player.getName(), query)
                .filter(result -> !result.contains(I_DON_T_KNOW))
                .switchIfEmpty(buildPlan(player, query, true));
    }

    public Flux<String> queryPlayerFacts(Player player, String query) {
        return semanticKernelProvider
                .getKernel(KernelType.QUERY)
                .map(Kernel::getMemory)
                .flatMap(memory -> getPlayerFacts(player, query, memory))
                .flatMapMany(PlayerController::extractText);
    }

    private static Flux<String> extractText(List<MemoryQueryResult> result) {
        return Flux
                .fromIterable(result)
                .sort(Comparator.comparingDouble(MemoryQueryResult::getRelevance))
                .map(it -> it.getMetadata().getText());
    }

    private static Mono<List<MemoryQueryResult>> getPlayerFacts(Player player, String query, SemanticTextMemory memory) {
        return memory
                .searchAsync(
                        getCollectionName(player),
                        query,
                        FACT_LIMIT,
                        0.0f,
                        true);
    }

    public static String getCollectionName(Player player) {
        return "player." + player.getUid();
    }

    private static String getAllInfoCollectionName(Player player) {
        return "player." + player.getUid() + ".info";
    }

    private Mono<String> askQuery(Player player, List<String> facts, String question) {
        Collections.reverse(facts);
        String factString = facts
                .stream()
                .limit(FACT_LIMIT)
                .map(entry -> entry.replaceAll(FACT_PREFIX, "").trim())
                .collect(Collectors.joining("\n"));

        SKContext context = SKBuilders.context().build();
        context.setVariable("facts", factString);
        context.setVariable("player", player.getName());

        return semanticKernelProvider.getKernel(KernelType.QUERY)
                .<SKFunction<?>>map(kernel -> kernel.getFunction("RPGSkills", "QueryWorld"))
                .<SKContext>flatMap(function -> {
                    return function.invokeAsync(question, context, null);
                })
                .map(SKContext::getResult)
                .map(String::trim);
    }

    public Mono<String> buildPlan(Player player, String req, boolean stepwise) {
        return semanticKernelProvider.getKernelEmpty(KernelType.PLANNER)
                .flatMap(kernel -> {

                    kernel.importSkill(this, null);

                    if (worldAction instanceof io.quarkus.arc.ClientProxy) {
                        Object impl = ((ClientProxy) worldAction).arc_contextualInstance();
                        kernel.importSkill(impl, null);
                    } else {
                        kernel.importSkill(worldAction, null);
                    }

                    return this.rules.getRules()
                            .flatMap(rules -> {
                                Mono<Plan> planGetter;

                                if (stepwise) {
                                    planGetter = planStepwisePlan(player, kernel, rules, req);
                                } else {
                                    planGetter = planSequentialPlan(player, kernel, rules, req);
                                }

                                return planGetter
                                        .flatMap(plan -> {
                                            LOGGER.info(plan.toPlanString());
                                            SKContext context = SKBuilders
                                                    .context()
                                                    .withVariables(SKBuilders
                                                            .variables()
                                                            .withVariable("rules", rules)
                                                            .withVariable("name", player.getName())
                                                            .withVariable("id", player.getUid())
                                                            .withInput(player.getName())
                                                            .build())
                                                    .withSkills(kernel.getSkills())
                                                    .build();

                                            return plan.invokeAsync(context);
                                        })
                                        .map(SKContext::getResult);
                            });
                });
    }

    private static Mono<Plan> planSequentialPlan(Player player, Kernel kernel, String rules, String request) {
        Mono<Plan> planGetter;
        SequentialPlanner planner = SemanticKernelProvider.getPlanner(kernel);
        SKContext context = SKBuilders
                .context()
                .withVariables(SKBuilders
                        .variables()
                        .withVariable("rules", rules)
                        .withVariable("name", player.getName())
                        .withVariable("id", player.getUid())
                        .build())
                .withSkills(kernel.getSkills())
                .build();
        planGetter = planner.createPlanAsync(request, context);
        return planGetter;
    }

    private static Mono<Plan> planStepwisePlan(Player player, Kernel kernel, String rules, String request) {

        request += "\n[OBSERVATION]\nPlayers id is " + player.getUid() + "\n";
        request += "\n[OBSERVATION]\nPlayers name is " + player.getName() + "\n";

        request += Arrays.stream(rules
                        .split("\n"))
                .map(it -> "[OBSERVATION]\n" + it)
                .collect(Collectors.joining("\n"));

        var stepwisePlan = new DefaultStepwisePlanner(kernel,
                new StepwisePlannerConfig(),
                null,
                null)
                .createPlan("Apply the events that would result from the following action: " + request);

        return Mono.just(stepwisePlan);
    }

    public Mono<String> addPlayerFact(Player player, String statement) {
        player.getFacts().addFact(statement);
        return savePlayerData(player);
    }

    public Mono<String> askQueryAboutPlayer(
            String playerName,
            String question) {
        try {
            Player player = players.getPlayerByName(playerName);
            return queryPlayerFacts(player, question)
                    .collectList()
                    .flatMap(facts -> {
                        return askQuery(player, facts, question.replaceAll("\\?", ""));
                    });
        } catch (PlayerNotFoundException e) {
            return Mono.error(e);
        }
    }


    @DefineSKFunction(
            name = "addPlayerLogEvent",
            description = "Adds an event to a players log."
    )
    public Mono<String> addPlayerLogEvent(
            @SKFunctionParameters(
                    name = "playerName",
                    description = "The name of the player."
            )
            String playerName,

            @SKFunctionParameters(
                    name = "eventDescription",
                    description = "A description of the event."
            )
            String eventDescription
    ) {
        try {
            Player player = players.getPlayerByName(playerName);
            player.getLog().addLog(eventDescription);
            return savePlayerData(player);
        } catch (PlayerNotFoundException e) {
            return Mono.error(e);
        }
    }

    @DefineSKFunction(
            name = "removeItemFromInventory",
            description = "Removes a given item from a characters inventory."
    )
    public void removeItemFromInventory(
            @SKFunctionParameters(
                    name = "playerName",
                    description = "The name of the player."
            )
            String playerName,

            @SKFunctionParameters(
                    name = "itemName",
                    description = "The name of the item to remove."
            )
            String itemName
    ) throws PlayerNotFoundException {
        players.getPlayerByName(playerName)
                .removeItemFromInventory(itemName);
    }

    @DefineSKFunction(
            name = "addItemToInventory",
            description = "Adds a given item to the characters inventory."
    )
    public void addItemToInventory(
            @SKFunctionParameters(
                    name = "playerName",
                    description = "The name of the player."
            )
            String playerName,

            @SKFunctionParameters(
                    name = "itemName",
                    description = "The name of the item to add."
            )
            String itemName
    ) throws PlayerNotFoundException {
        Player player = players.getPlayerByName(playerName);
        player.addItemFromInventory(itemName);
    }

    @DefineSKFunction(
            name = "checkHeath",
            description = "Checks if a player is unconscious.",
            returnDescription = "A statement saying if a player is unconscious."
    )
    public String checkHeath(
            @SKFunctionParameters(
                    name = "playerName",
                    description = "The name of the player."
            )
            String playerName
    ) throws PlayerNotFoundException {
        Player player = players.getPlayerByName(playerName);
        return player.getHealth() <= 0 ? playerName + " is unconscious" : "";
    }


    @DefineSKFunction(
            name = "canPlayerCastSpell",
            description = "Given a spell name return a statement saying if a player is able to cast it.",
            returnDescription = "A statement saying weather a player can cast that spell."
    )
    public String canPlayerCastSpell(
            @SKFunctionParameters(
                    name = "playerName",
                    description = "The name of the player to check."
            )
            String playerName,
            @SKFunctionParameters(
                    name = "spellName",
                    description = "The name of the spell."
            )
            String spellName
    ) throws PlayerNotFoundException {
        spellName = spellName.toLowerCase();
        Player player = players.getPlayerByName(playerName);
        if (!player.getSpells().contains(spellName)) {
            return playerName + " does not know " + spellName;
        }

        if (player.getSpellsAvailable() <= 0) {
            return playerName + " has no spells left.";
        }

        Spell spell = spells.getSpells().get(spellName);


        return "Yes " + playerName + " can cast " + spellName + " and it does: \n" + spell.getEffect().name() + " for " + spell.getAmount() + "\n";
    }

    @DefineSKFunction(
            name = "decrementSpellCount",
            description = "Removes a single spell from a players spell count.",
            returnDescription = "Description of the number of remaining spells."
    )
    public String decrementSpellCount(
            @SKFunctionInputAttribute(
                    description = "The name of the player to have their spell count reduced."
            )
            String playerName
    ) throws PlayerNotFoundException {
        Player player = players.getPlayerByName(playerName);
        player.setSpellsAvailable(player.getSpellsAvailable() - 1);

        if (player.getSpellsAvailable() == 0) {
            return player.getName() + " player is out of spells.";
        } else if (player.getSpellsAvailable() < 0) {
            return player.getName() + " IS A CHEATER player is out of spells.";
        }

        return player.getName() + " now has " + player.getSpellsAvailable() + " spells.";
    }

    @DefineSKFunction(
            name = "applyDamage",
            description = "Applies the given damage to a players current health.",
            returnDescription = "Description of the players new health."
    )
    public String applyDamage(
            @SKFunctionParameters(
                    name = "playerName",
                    description = "The name of the player to take damage."
            )
            String playerName,

            @SKFunctionParameters(
                    name = "damage",
                    description = "The amount of damage to apply."
            )
            String dmg
    ) throws PlayerNotFoundException {
        Player player = players.getPlayerByName(playerName);

        player.setHeath(player.getHealth() - Integer.parseInt(dmg));

        return player.getName() + " now has " + player.getHealth() + " health.";
    }


}
