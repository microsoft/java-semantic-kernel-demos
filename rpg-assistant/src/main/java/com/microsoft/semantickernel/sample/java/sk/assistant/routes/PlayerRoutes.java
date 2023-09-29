package com.microsoft.semantickernel.sample.java.sk.assistant.routes;

import com.azure.core.annotation.BodyParam;
import com.microsoft.semantickernel.sample.java.sk.assistant.datastore.PlayerController;
import com.microsoft.semantickernel.sample.java.sk.assistant.datastore.Players;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Player;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Provides endpoints for retrieving and interacting with player data.
 */
@Path("/players")
@Produces(MediaType.APPLICATION_JSON)
public class PlayerRoutes {
    private final Players players;
    private final PlayerController playerController;

    @Inject
    PlayerRoutes(Players players, PlayerController playerController) {
        this.players = players;
        this.playerController = playerController;
    }

    @GET
    @Path("/info/{playerId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Player> queryPlayerFact(
            @PathParam("playerId")
            String playerId
    ) {
        Player result = players.getPlayer(playerId);
        return Uni.createFrom().item(result);
    }

    @GET
    @Path("names")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Map<String, String>> getNames() {
        Map<String, String> ids = players
                .getPlayers()
                .stream()
                .map(customer -> Map.of(customer.getUid(), customer.getName()))
                .reduce(new HashMap<>(), (accumulator, newData) -> {
                    accumulator.putAll(newData);
                    return accumulator;
                });

        return Uni.createFrom().item(ids);
    }


    @POST
    @Path("/saveFacts/{playerId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<String> saveFacts(
            @PathParam("playerId")
            String playerId,
            @BodyParam("notes")
            String notes
    ) {
        Player player = players.getPlayer(playerId);

        player.getFacts().setFacts(notes);
        CompletableFuture<String> future = playerController.savePlayerData(player).toFuture();

        return Uni.createFrom().future(future);
    }

}
