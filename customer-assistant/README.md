# Java and OpenAI Hero Demos

This repository holds end-to-end demo applications that represent the concept of an Intelligent App, built in Java.

## List of applications

### Customer Assistant

This application is a customer assistant that can answer questions and also take actions about and towards a customer's account.

Before running the application in any way, proceed with the following:

1. In the `docker` folder, make a copy of `demo.properties_example` to `demo.properties` (in the same folder), and fill in the required keys.

To run the application locally as a container, have Docker Desktop or Podman installed, then run the following command:

```bash
cd docker
docker compose up --build --force-recreate
```

# Debug

> [!NOTE]
> Unfortunately at time of writing we depend on the latest snapshot of semantic kernel.
> If you wish to develop, you will therefore need to install the latest snapshot at https://github.com/microsoft/semantic-kernel/tree/experimental-java/java.
> This requirement will be removed on the next release of semantic kernel.

To run the application in debugging mode, from the customer-assistant folder, run:

```bash
CONF_PROPERTIES=../docker/demo.properties ../mvnw clean package quarkus:dev
```

## Technology Stack

These applications use different sets of stacks. Some may be based on Spring, others in Quarkus. Check each folder to learn more.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
