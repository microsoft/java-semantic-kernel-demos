# RPG Assistant

## Description

This is a RPG assistant that will help you remember and generate content for your campaigns.

## Build

1. In the `docker` folder, make a copy of `demo.properties_example` to `demo.properties` (in the same folder), and fill in the required keys.

To run the application locally as a container, have Docker Desktop or Podman installed, then run the following command:

```bash
cd docker
docker-compose up --build --force-recreate
```

## Debug

> [!NOTE]
> Unfortunately at time of writing we depend on the latest snapshot of semantic kernel.
> If you wish to develop, you will therefore need to install the latest snapshot at https://github.com/microsoft/semantic-kernel/tree/experimental-java/java.
> This requirement will be removed on the next release of semantic kernel.

To run the application in debugging mode, follow these steps:

1. Copy the adjusted `demo.properties` file into

2. Run
```bash
CONF_PROPERTIES=./docker/demo.properties ./mvnw clean package quarkus:dev
```
