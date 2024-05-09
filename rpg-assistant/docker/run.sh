#!/bin/bash

if [ -f /run/secrets/demo ]; then
    cp /run/secrets/demo ./.env
fi

java -jar quarkus-run.jar -Dquarkus.http.host=0.0.0.0
