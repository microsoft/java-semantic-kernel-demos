package com.microsoft.semantickernel.sample.java.sk.assistant.settings;

import com.microsoft.semantickernel.exceptions.ConfigurationException;
import com.microsoft.semantickernel.settings.SettingsMap;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Makes semantic kernel settings available to quarkus.
 */
public class AssistantSettings implements ConfigSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(AssistantSettings.class);
    private static final Map<String, String> settings;

    static {
        settings = new HashMap<>();
        try {
            settings.putAll(SettingsMap.getDefault());
        } catch (ConfigurationException e) {
            LOGGER.warn("Failed to load settings", e);
        }
    }

    @Override
    public int getOrdinal() {
        return 275;
    }

    @Override
    public Set<String> getPropertyNames() {
        return settings.keySet();
    }

    @Override
    public String getValue(final String propertyName) {
        return settings.get(propertyName);
    }

    @Override
    public String getName() {
        return AssistantSettings.class.getSimpleName();
    }
}
