package com.microsoft.semantickernel.sample.java.sk.assistant;


import io.quarkus.runtime.Startup;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tasks to be performed when the application starts
 */
@Startup
public class StartupTasks {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartupTasks.class);

    @Startup
    @Inject
    public void run(SemanticKernelProvider semanticKernelProvider) {
        semanticKernelProvider.init();

        LOGGER.info("===STARTUP COMPLETE===");
    }
}
