package com.microsoft.semantickernel.sample.java.sk.assistant.routes;

import com.azure.core.annotation.BodyParam;
import com.microsoft.semantickernel.sample.java.sk.assistant.datastore.Rules;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Provides endpoint for retrieving and updating world rules.
 */
@Path("/rules")
public class WorldRules {

    private final Rules rules;

    @Inject
    public WorldRules(Rules rules) {
        this.rules = rules;
    }

    @GET
    @Path("worldRules")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> worldRules() {
        return Uni
                .createFrom()
                .item(String.join("\n", rules.getRules()));
    }


    @POST
    @Path("worldRules")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> postWorldRules(
            @BodyParam("rules")
            String rules
    ) {
        this.rules.setRules(rules);
        return Uni.createFrom().nothing();
    }
}
