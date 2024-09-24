package com.microsoft.semantickernel.sample.java.sk.assistant.routes;

import com.azure.core.annotation.BodyParam;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Rule;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Rules;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

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
        return Uni.createFrom().future(Mono.just(rules.getRules())
                .flatMapMany(Flux::fromIterable)
                .map(Rule::rule)
                .collectList()
                .map(rules -> String.join("\n", rules))
                .toFuture());
    }

    @POST
    @Path("rules")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> postCustomerRules(
            @BodyParam("rules")
            String rules
    ) {
        List<Rule> rulesList = Arrays.stream(rules
                        .split("\n"))
                .map(String::trim)
                .filter(rule -> !rule.isEmpty())
                .map(Rule::new)
                .toList();

        this.rules.setRules(rulesList);
        return Uni.createFrom().nothing();
    }
}
