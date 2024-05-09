package com.microsoft.semantickernel.sample.java.sk.assistant.routes;

import com.azure.core.annotation.BodyParam;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.orchestration.FunctionResult;
import com.microsoft.semantickernel.plugin.KernelPlugin;
import com.microsoft.semantickernel.plugin.KernelPluginFactory;
import com.microsoft.semantickernel.sample.java.sk.assistant.KernelType;
import com.microsoft.semantickernel.sample.java.sk.assistant.SemanticKernelProvider;
import com.microsoft.semantickernel.sample.java.sk.assistant.datastore.PlayerController;
import com.microsoft.semantickernel.sample.java.sk.assistant.datastore.Players;
import com.microsoft.semantickernel.sample.java.sk.assistant.datastore.WorldLog;
import com.microsoft.semantickernel.sample.java.sk.assistant.datastore.azure.SemanticTextMemory;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Player;
import com.microsoft.semantickernel.semanticfunctions.KernelFunctionArguments;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Provides endpoint for generating game content.
 */
@Path("/api/generate")
@Produces(MediaType.APPLICATION_JSON)
public class GenerateContentRoutes {
    private final SemanticKernelProvider semanticKernelProvider;
    private final WorldLog worldLog;
    private final Players players;
    private final SemanticTextMemory memory;

    @Inject
    GenerateContentRoutes(
            SemanticKernelProvider semanticKernelProvider,
            WorldLog worldLog,
            Players players,
            SemanticTextMemory memory) {
        this.semanticKernelProvider = semanticKernelProvider;
        this.worldLog = worldLog;
        this.players = players;
        this.memory = memory;
    }

    @POST
    @Path("/request")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> generateInfo(
            @BodyParam("request")
            String request
    ) {
        CompletableFuture<String> future = semanticKernelProvider
                .getKernel(KernelType.GENERATE)
                .flatMap(kernel -> {
                    return getRelevantPlayerInformation(request, kernel)
                            .flatMap(playerInfo -> {
                                KernelPlugin func = KernelPluginFactory.importPluginFromResourcesDirectory("skills", "RPGSkills", "GenerateInfo", null);

                                KernelFunctionArguments.Builder variables = KernelFunctionArguments.builder();

                                List<String> events = getWorldEvents();

                                variables = variables.withVariable("events", events);
                                variables = variables.withVariable("playerinfo", playerInfo);
                                variables = variables.withVariable("input", request);

                                return func
                                        .get("GenerateInfo")
                                        .invokeAsync(kernel)
                                        .withArguments(variables.build())
                                        .withResultType(String.class);
                            });
                })
                .map(FunctionResult::getResult)
                .map(result -> {
                    try {
                        Player player = new ObjectMapper().readValue(result, Player.class);
                        if (player != null) {
                            players.addPlayer(player);
                        }
                    } catch (JsonProcessingException e) {
                        // NOP
                    }
                    return result;
                })
                .toFuture();

        return Uni.createFrom().future(future);
    }

    private List<String> getWorldEvents() {
        return worldLog.getLog().getLog();
    }

    private Mono<List<String>> getRelevantPlayerInformation(String request, Kernel kernel) {
        return Flux.fromIterable(players.getPlayers())
                .concatMap(player -> {
                            String collectionName = PlayerController.getCollectionName(player);
                            return memory
                                    .searchAsync(
                                            collectionName,
                                            request,
                                            100,
                                            0.5f,
                                            true)
                                    .flatMapMany(Flux::fromIterable)
                                    .map(info -> info.getMetadata().getText());
                        }
                )
                .collectList();
    }

}
