package com.microsoft.semantickernel.sample.java.sk.assistant.routes;

import com.azure.core.annotation.BodyParam;
import com.microsoft.semantickernel.exceptions.ConfigurationException;
import com.microsoft.semantickernel.sample.java.sk.assistant.utilities.Rules;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Provides the admin route, mostly used for setting processing rules
 */
@Path("/api/admin")
//@Authenticated
public class Admin {

    private final Rules rules;

    @Inject
    public Admin(Rules rules) {
        this.rules = rules;
    }

    @GET
    @Path("rules")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> customerRules() {
        return Uni.createFrom().future(rules.getRules().toFuture());
    }

    @POST
    @Path("rules")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> postCustomerRules(
            @BodyParam("rules")
            String rules
    ) throws ConfigurationException {
        this.rules.setRules(rules);
        return Uni.createFrom().nothing();
    }
}
