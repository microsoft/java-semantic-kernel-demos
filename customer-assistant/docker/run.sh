#!/bin/bash

if [ -f /run/secrets/demo ]; then
    mkdir -p /home/deploy/.sk || true
    cp /run/secrets/demo /home/deploy/.sk/conf.properties
fi

java -jar quarkus-run.jar -Dquarkus.http.host=0.0.0.0
#java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005 -jar quarkus-run.jar -Dquarkus.http.host=0.0.0.0
