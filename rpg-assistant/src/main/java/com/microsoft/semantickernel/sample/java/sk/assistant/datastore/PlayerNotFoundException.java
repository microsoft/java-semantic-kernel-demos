package com.microsoft.semantickernel.sample.java.sk.assistant.datastore;

public class PlayerNotFoundException extends Throwable {
    public PlayerNotFoundException(String playerNotFound) {
        super("Could not find player: " + playerNotFound);
    }
}
