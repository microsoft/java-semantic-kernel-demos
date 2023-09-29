package com.microsoft.semantickernel.sample.java.sk.assistant.routes;

import com.microsoft.semantickernel.sample.java.sk.assistant.datastore.WorldLog;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Log;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Provides endpoint for obtaining logs of events that have happened in game.
 */
@Path("/log")
@Produces(MediaType.APPLICATION_JSON)
public class LogRoutes {
    private final WorldLog log;

    @Inject
    LogRoutes(WorldLog log) {
        this.log = log;
    }

    @GET
    @Path("/world")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Log> getWorldLog() {
        return Uni.createFrom().item(log.getLog());
    }

}
