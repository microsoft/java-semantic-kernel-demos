package com.microsoft.semantickernel.sample.java.sk.assistant.datastore;

import com.microsoft.semantickernel.sample.java.sk.assistant.models.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;

@ApplicationScoped
public class WorldLog {

    private final Log log = new Log(new ArrayList<>());

    public void addLog(String statement) {
        log.addLog(statement);
    }

    public Log getLog() {
        return log;
    }

}
