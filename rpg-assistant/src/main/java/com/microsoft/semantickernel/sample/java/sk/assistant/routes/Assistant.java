package com.microsoft.semantickernel.sample.java.sk.assistant.routes;

import com.azure.core.annotation.BodyParam;
import com.microsoft.semantickernel.orchestration.SKContext;
import com.microsoft.semantickernel.orchestration.SKFunction;
import com.microsoft.semantickernel.sample.java.sk.assistant.KernelType;
import com.microsoft.semantickernel.sample.java.sk.assistant.SemanticKernelProvider;
import com.microsoft.semantickernel.sample.java.sk.assistant.datastore.PlayerController;
import com.microsoft.semantickernel.sample.java.sk.assistant.datastore.Players;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Player;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.StatementType;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

/**
 * Provides the path for the main assistant endpoint for receiving user instructions.
 */
@Path("/assistant")
public class Assistant {
    private final Players players;
    private final SemanticKernelProvider semanticKernelProvider;
    private final PlayerController playerController;

    @Inject
    Assistant(
            SemanticKernelProvider semanticKernelProvider,
            PlayerController playerController,
            Players players) {
        this.players = players;
        this.semanticKernelProvider = semanticKernelProvider;
        this.playerController = playerController;
    }

    @POST
    @Path("perform/{playerId}")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> performRequest(
            @PathParam("playerId")
            String playerId,

            @BodyParam("statement")
            String statement
    ) {
        Player player = players.getPlayer(playerId);
        if (player == null) {
            return Uni.createFrom().failure(new NotFoundException("Customer not found"));
        }

        String cleaned = statement.replaceAll("^[^a-zA-Z0-9]+", "").trim();

        CompletableFuture<String> future =
                classifyStatement(cleaned)
                        .flatMap(type -> {
                            switch (type) {
                                case ACTION, REQUEST, EVENT -> {
                                    boolean externalResourcePlan = false;
                                    if (statement.startsWith("/")) {
                                        externalResourcePlan = true;
                                    }

                                    return playerController.buildPlan(player, cleaned, externalResourcePlan);
                                }
                                case QUESTION -> {
                                    return playerController.answerQuestion(player, cleaned);
                                }
                                case FACT -> {
                                    return playerController.addPlayerFact(player, cleaned)
                                            .then(Mono.just("Recorded note: statement"));
                                }
                                default -> {
                                    return Mono.just("Unknown statement type");
                                }
                            }
                        })
                        .toFuture();

        return Uni.createFrom().future(future);
    }

    public Mono<StatementType> classifyStatement(String statement) {
        return semanticKernelProvider.getKernel(KernelType.QUERY)
                .<SKFunction<?>>map(kernel -> kernel.getFunction("Language", "StatementType"))
                .flatMap(skFunction -> skFunction.invokeAsync(statement))
                .map(SKContext::getResult)
                .map(String::toUpperCase)
                .map(String::strip)
                .map(StatementType::valueOf);
    }
}
