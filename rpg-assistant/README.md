# RPG Assistant

## Description

This is a RPG assistant that will help you remember and generate content for your campaigns.

## Build

1. In the `docker` folder, make a copy of `env.example` to `.env` (in the same folder), and fill in the required keys.

To run the application locally as a container, have Docker Desktop or Podman installed, then run the following command:

```bash
cd docker
docker-compose up --build --force-recreate
```

## Debug

To run the application in debugging mode, follow these steps:

1. Create a .env file as shown in [env.example](docker%2Fenv.example)

2. Run
```bash
CONF_PROPERTIES=./docker/demo.properties ./mvnw clean package quarkus:dev
```
