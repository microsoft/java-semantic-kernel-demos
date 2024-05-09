package com.microsoft.semantickernel.sample.java.sk.assistant.datastore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Player;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Holds all players within the game.
 */
@ApplicationScoped
public class Players {
    private final Map<String, Player> players;

    public Players() {
        this.players = new HashMap<>();
        loadPlayers();
    }

    @Startup
    public void loadPlayers() {
        try (InputStream is = Players.class.getResourceAsStream("players.json")) {
            Player[] players = new ObjectMapper().readValue(is, Player[].class);
            for (Player player : players) {
                this.players.put(player.getUid(), player);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Player getPlayer(String playerId) {
        return players.get(playerId);
    }

    public Player getPlayerByName(String name) throws PlayerNotFoundException {
        Optional<Player> player = players.values().stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst();

        if (player.isEmpty()) {
            throw new PlayerNotFoundException("Player not found");
        }

        return player.orElse(null);
    }

    public List<String> getPlayerNames() {
        return new ArrayList<>(players.values().stream().map(Player::getName).toList());
    }

    public Collection<Player> getPlayers() {
        return players.values();
    }

    public void addPlayer(Player player) {
        players.put(player.getUid(), player);
    }
}
