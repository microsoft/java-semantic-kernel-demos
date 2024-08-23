#!/bin/bash

docker-compose -f ./docker/docker-compose-dev.yml build
docker-compose -f ./docker/docker-compose-dev.yml run customer-assistant-debug
