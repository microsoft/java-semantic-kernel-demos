package com.microsoft.semantickernel.sample.java.sk.assistant.utilities;

import com.microsoft.semantickernel.exceptions.ConfigurationException;
import com.microsoft.semantickernel.sample.java.sk.assistant.SemanticKernelProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import reactor.core.publisher.Mono;

/**
 * Defines the rules applied to prompts
 */
@ApplicationScoped
public class Rules {
    public static final String COLLECTION_NAME = "rules";
    public static final String KEY = "rules";
    private final SemanticKernelProvider semanticKernelProvider;

    @Inject
    public Rules(SemanticKernelProvider semanticKernelProvider) {
        this.semanticKernelProvider = semanticKernelProvider;
    }

    public Mono<String> getRules() {
        try {
            return semanticKernelProvider
                    .getEmbeddingKernel()
                    .flatMap(kernel -> {
                        return kernel
                                .getMemory()
                                .getAsync(COLLECTION_NAME, KEY, false);
                    })
                    .map(it -> it.getMetadata().getText());
        } catch (ConfigurationException e) {
            return Mono.error(e);
        }

    }

    public void setRules(String rules) throws ConfigurationException {
        semanticKernelProvider
                .getEmbeddingKernel()
                .flatMap(kernel -> kernel
                        .getMemory()
                        .saveInformationAsync(COLLECTION_NAME, rules, KEY, null, null))
                .subscribe();
    }
}
