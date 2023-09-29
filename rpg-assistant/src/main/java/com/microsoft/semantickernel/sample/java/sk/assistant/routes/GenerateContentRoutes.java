package com.microsoft.semantickernel.sample.java.sk.assistant.routes;

import com.azure.core.annotation.BodyParam;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.SKBuilders;
import com.microsoft.semantickernel.orchestration.ContextVariables;
import com.microsoft.semantickernel.orchestration.SKContext;
import com.microsoft.semantickernel.sample.java.sk.assistant.KernelType;
import com.microsoft.semantickernel.sample.java.sk.assistant.SemanticKernelProvider;
import com.microsoft.semantickernel.sample.java.sk.assistant.datastore.PlayerController;
import com.microsoft.semantickernel.sample.java.sk.assistant.datastore.Players;
import com.microsoft.semantickernel.sample.java.sk.assistant.datastore.WorldLog;
import com.microsoft.semantickernel.skilldefinition.ReadOnlyFunctionCollection;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

    @Inject
    GenerateContentRoutes(
            SemanticKernelProvider semanticKernelProvider,
            WorldLog worldLog,
            Players players) {
        this.semanticKernelProvider = semanticKernelProvider;
        this.worldLog = worldLog;
        this.players = players;
    }

    @POST
    @Path("/request")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> generateInfo(
            @BodyParam("request")
            String request
    ) {
        CompletableFuture<String> future = semanticKernelProvider
                .getKernel(KernelType.QUERY)
                .flatMap(kernel -> {
                    return getRelevantPlayerInformation(request, kernel)
                            .flatMap(playerInfo -> {
                                ReadOnlyFunctionCollection func = kernel.importSkillFromResources("skills", "RPGSkills", "GenerateInfo");

                                ContextVariables.Builder variables = SKBuilders.variables();

                                String events = getWorldEvents();

                                variables = variables.withVariable("events", events);
                                variables = variables.withVariable("playerinfo", playerInfo);
                                variables = variables.withVariable("input", request);

                                SKContext context = SKBuilders
                                        .context()
                                        .withVariables(variables.build())
                                        .build();

                                return func
                                        .getFunction("GenerateInfo")
                                        .invokeAsync(context);
                            });
                })
                .map(SKContext::getResult)
                .toFuture();

        return Uni.createFrom().future(future);
    }

    private String getWorldEvents() {
        String events = worldLog
                .getLog()
                .getLog()
                .stream()
                .reduce("", (accumulator, newData) -> accumulator + "\n" + newData);

        return events;
    }

    private Mono<String> getRelevantPlayerInformation(String request, Kernel kernel) {
        return Flux.fromIterable(players.getPlayers())
                .concatMap(player -> {
                            String collectionName = PlayerController.getCollectionName(player);
                            return kernel
                                    .getMemory()
                                    .searchAsync(
                                            collectionName,
                                            request,
                                            100,
                                            0.5f,
                                            true)
                                    .flatMapMany(Flux::fromIterable)
                                    .map(info -> info.getMetadata().getText())
                                    .reduce("", (accumulator, newData) -> accumulator + "\n" + newData)
                                    .map(info ->
                                            "[INFO ABOUT PLAYER " + player.getName() + "]\n" +
                                                    info +
                                                    "\n[END INFO ABOUT PLAYER " + player.getName() + "]\n"
                                    );
                        }
                )
                .reduce("", (accumulator, newData) -> accumulator + "\n" + newData);
    }

}
