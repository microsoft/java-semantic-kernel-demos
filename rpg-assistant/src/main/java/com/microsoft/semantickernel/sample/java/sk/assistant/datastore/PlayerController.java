package com.microsoft.semantickernel.sample.java.sk.assistant.datastore;

import com.microsoft.semantickernel.hooks.KernelHook;
import com.microsoft.semantickernel.orchestration.FunctionResult;
import com.microsoft.semantickernel.orchestration.ToolCallBehavior;
import com.microsoft.semantickernel.sample.java.sk.assistant.KernelType;
import com.microsoft.semantickernel.sample.java.sk.assistant.SemanticKernelProvider;
import com.microsoft.semantickernel.sample.java.sk.assistant.datastore.azure.MemoryQueryResult;
import com.microsoft.semantickernel.sample.java.sk.assistant.datastore.azure.SemanticTextMemory;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Player;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Spell;
import com.microsoft.semantickernel.sample.java.sk.assistant.utilities.rpg.WorldAction;
import com.microsoft.semantickernel.semanticfunctions.KernelFunction;
import com.microsoft.semantickernel.semanticfunctions.KernelFunctionArguments;
import com.microsoft.semantickernel.semanticfunctions.KernelFunctionMetadata;
import com.microsoft.semantickernel.semanticfunctions.annotations.DefineKernelFunction;
import com.microsoft.semantickernel.semanticfunctions.annotations.KernelFunctionParameter;
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
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides functionality and skills for retrieving and interacting with the player data.
 */
@ApplicationScoped
public class PlayerController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerController.class);
    public static final String I_DON_T_KNOW = "I don't know";
    public static final int FACT_LIMIT = 5;
    public static final String FACT_PREFIX = "FACT";
    private final Players players;
    private final SemanticKernelProvider semanticKernelProvider;
    private final Rules rules;
    private final WorldAction worldAction;
    private final Spells spells;

    private final SemanticTextMemory memory;

    @Inject
    public PlayerController(
            Players players,
            SemanticKernelProvider semanticKernelProvider,
            Rules rules,
            WorldAction worldAction,
            Spells spells,
            SemanticTextMemory memory) {
        this.players = players;
        this.semanticKernelProvider = semanticKernelProvider;
        this.rules = rules;
        this.worldAction = worldAction;
        this.spells = spells;
        this.memory = memory;
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
                            .onErrorResume(e -> Mono.empty())
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
        return savePlayerEvents(player, memory)
                .doOnError(e -> {
                    LOGGER.warn("failed to save data", e);
                })
                .then(savePlayerFacts(player, memory))
                .then(Mono.just("Done"));
    }

    public Mono<String> loadPlayerFacts(Player player) {
        return memory.getAsync(
                        getAllInfoCollectionName(player),
                        "facts",
                        false
                )
                .map(it -> it.getMetadata().getText());
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
                                    "Fact about " + player.getName(),
                                    "Fact about " + player.getName()
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
                        entryPrefix + ": A fact about " + player.getName() + ": " + info,
                        id,
                        entryPrefix + " for player " + player.getName(),
                        entryPrefix + " for player " + player.getName()
                );
    }

    private static Mono<Void> savePlayerEvents(Player player, SemanticTextMemory memory) {
        return Flux
                .fromIterable(player.getLog().getLog())
                .concatMap(logEvent -> {
                    return savePlayerInfo(logEvent, memory, player, "EVENT");
                })
                .last()
                .then();
    }

    public Mono<String> answerQuestion(Player player, String query) {
        return askQueryAboutPlayer(player.getName(), query);
    }

    public Flux<String> queryPlayerFacts(Player player, String query) {
        return getPlayerFacts(player, query, memory)
                .flux().concatMap(PlayerController::extractText);
    }

    private static Flux<String> extractText(List<MemoryQueryResult> result) {
        return Flux
                .fromIterable(result)
                .map(it -> it.getMetadata().getText());
    }

    private static Mono<List<MemoryQueryResult>> getPlayerFacts(Player player, String query, SemanticTextMemory memory) {
        return memory
                .searchAsync(
                        getCollectionName(player),
                        query,
                        FACT_LIMIT,
                        0.0f,
                        true)
                .onErrorReturn(List.of());
    }

    public static String getCollectionName(Player player) {
        return "player." + player.getUid();
    }

    private static String getAllInfoCollectionName(Player player) {
        return "player." + player.getUid() + ".info";
    }

    private Mono<String> askQuery(Player player, List<String> facts, String question) {
        List<String> factString = facts
                .stream()
                .limit(FACT_LIMIT)
                .map(entry -> entry.replaceAll(FACT_PREFIX + ": ", player.getName() + " is ").trim())
                .map(entry -> entry.replaceAll("\\n", "").trim())
                .collect(Collectors.toList());

        KernelFunctionArguments context = KernelFunctionArguments.builder()
                .withVariable("question", question)
                .withVariable("player", player.getName())
                .withVariable("facts", factString)
                .build();

        return semanticKernelProvider.getKernel(KernelType.QUERY)
                .flatMap(kernel -> kernel
                        .invokeAsync("RPGSkills", "QueryWorld")
                        .withArguments(context)
                        .withResultType(String.class))
                .map(FunctionResult::getResult)
                .map(String::trim);
    }

    public Mono<String> buildPlan(Player player, String req, boolean stepwise) {
        return semanticKernelProvider
                .getKernel(KernelType.PLANNER)
                .flatMap(kernel -> {

                    kernel.getPlugins()
                            .forEach(plugin -> {
                                System.out.println("Plugin: " + plugin.getName());
                                plugin
                                        .getFunctions()
                                        .values()
                                        .stream()
                                        .map(KernelFunction::getMetadata)
                                        .forEach(PlayerController::printFunction);
                            });

                    return SemanticKernelProvider.getPerformActionFunction()
                            .invokeAsync(kernel)
                            .addKernelHook(
                                    (KernelHook.PreToolCallHook) preToolCallEvent -> {
                                        LOGGER.info("Invoking: " + preToolCallEvent.getFunction().getName() + "\n" + preToolCallEvent.getArguments().prettyPrint());
                                        return preToolCallEvent;
                                    })
                            .withArguments(KernelFunctionArguments.builder()
                                    .withVariable("playerName", player.getName())
                                    .withVariable("request", req)
                                    .withVariable("rules", rules.getRules())
                                    .build())
                            .withToolCallBehavior(ToolCallBehavior.allowAllKernelFunctions(true));
                })
                .flatMap(result -> {
                    String value = result.getResultVariable().getValue(String.class);
                    if (value == null) {
                        return Mono.empty();
                    } else {
                        return Mono.just(value);
                    }
                });
    }

    private static void printFunction(KernelFunctionMetadata<?> func) {
        System.out.println("   " + func.getName() + ": " + func.getDescription());

        if (!func.getParameters().isEmpty()) {
            System.out.println("      Params:");

            func.getParameters()
                    .forEach(p -> {
                        System.out.println("      - " + p.getName() + ": " + p.getDescription());
                        System.out.println("        default: '" + p.getDefaultValue() + "'");
                    });
        }

        System.out.println();
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

    @DefineKernelFunction(
            name = "removeItemFromInventory",
            description = "Removes a given item from a characters inventory."
    )
    public void removeItemFromInventory(
            @KernelFunctionParameter(
                    name = "playerName",
                    description = "The name of the player."
            )
            String playerName,

            @KernelFunctionParameter(
                    name = "itemName",
                    description = "The name of the item to remove."
            )
            String itemName
    ) throws PlayerNotFoundException {
        players.getPlayerByName(playerName)
                .removeItemFromInventory(itemName);
    }

    @DefineKernelFunction(
            name = "addItemToInventory",
            description = "Adds a given item to the characters inventory.",
            returnType = "void"
    )
    public void addItemToInventory(
            @KernelFunctionParameter(
                    name = "playerName",
                    description = "The name of the player."
            )
            String playerName,

            @KernelFunctionParameter(
                    name = "itemName",
                    description = "The name of the item to add."
            )
            String itemName
    ) throws PlayerNotFoundException {
        Player player = players.getPlayerByName(playerName);
        player.addItemFromInventory(itemName);
    }

    @DefineKernelFunction(
            name = "checkHeath",
            description = "Checks if a player is unconscious.",
            returnDescription = "A statement saying if a player is unconscious.",
            returnType = "string"
    )
    public String checkHeath(
            @KernelFunctionParameter(
                    name = "playerName",
                    description = "The name of the player."
            )
            String playerName
    ) throws PlayerNotFoundException {
        try {
            Player player = players.getPlayerByName(playerName);
            return player.getHealth() <= 0 ? playerName + " is unconscious" : "";
        } catch (Throwable t) {
            throw new RuntimeException("");
        }
    }


    @DefineKernelFunction(
            name = "canPlayerCastSpell",
            description = "Given a spell name return a statement saying if a player is able to cast it.",
            returnDescription = "A statement saying weather a player can cast that spell.",
            returnType = "string"
    )
    public String canPlayerCastSpell(
            @KernelFunctionParameter(
                    name = "playerName",
                    description = "The name of the player to check."
            )
            String playerName,
            @KernelFunctionParameter(
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

    @DefineKernelFunction(
            name = "decrementSpellCount",
            description = "Removes a single spell from a players spell count.",
            returnDescription = "Description of the number of remaining spells.",
            returnType = "string"
    )
    public String decrementSpellCount(
            @KernelFunctionParameter(
                    name = "playerName",
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

    @DefineKernelFunction(
            name = "applyDamage",
            description = "Applies the given damage to a players current health.",
            returnDescription = "Description of the players new health.",
            returnType = "string"
    )
    public String applyDamage(
            @KernelFunctionParameter(
                    name = "playerName",
                    description = "The name of the player to take damage."
            )
            String playerName,

            @KernelFunctionParameter(
                    name = "damage",
                    description = "The amount of damage to apply.",
                    type = Integer.class
            )
            Integer dmg
    ) throws PlayerNotFoundException {
        try {
            Player player = players.getPlayerByName(playerName);

            player.setHeath(player.getHealth() - dmg);

            return player.getName() + " now has " + player.getHealth() + " health.";
        } catch (Throwable t) {
            throw new RuntimeException("");
        }
    }

    @DefineKernelFunction(
            name = "heal",
            description = "Applies the given healing to a players current health.",
            returnDescription = "Description of the players new health.",
            returnType = "string"
    )
    public String applyHeal(
            @KernelFunctionParameter(
                    name = "playerName",
                    description = "The name of the player to take damage."
            )
            String playerName,

            @KernelFunctionParameter(
                    name = "damage",
                    description = "The amount of healing to apply.",
                    type = Integer.class
            )
            Integer healing
    ) throws PlayerNotFoundException {
        try {
            Player player = players.getPlayerByName(playerName);

            player.setHeath(player.getHealth() + healing);

            return player.getName() + " now has " + player.getHealth() + " health.";
        } catch (Throwable t) {
            throw new RuntimeException("");
        }
    }
}
