package com.microsoft.semantickernel.sample.java.sk.assistant.routes;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Provides login route if authentication is enabled
 */
@Path("/login")
@Produces(MediaType.APPLICATION_JSON)
//@Authenticated
public class Root {
    @GET
    public Uni<Response> get() {
        try {
            return Uni.createFrom().item(Response.temporaryRedirect(new URI("/")).build());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
