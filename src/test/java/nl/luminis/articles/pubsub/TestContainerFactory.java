package nl.luminis.articles.pubsub;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

public class TestContainerFactory {

    public static final int PUBSUB_PORT = 8432;

    public static GenericContainer createPubSubContainer() {
        return new GenericContainer(new ImageFromDockerfile()
            .withFileFromClasspath("Dockerfile", "docker/Dockerfile")
            .withFileFromClasspath("start-pubsub.sh", "docker/start-pubsub.sh")
            .withFileFromClasspath("pubsub-client.py", "docker/pubsub-client.py"))
            .withExposedPorts(PUBSUB_PORT)
            .withEnv("PUBSUB_PROJECT_ID", "my-gcp-project")
            .withEnv("PUBSUB_CONFIG", "[{\"name\": \"my-topic\", \"subscriptions\": [\"my-subscription\"]}]");
    }

    // Convenience method to start a Pub/Sub container outside of integration tests.
    public static void main(String[] args) {
        GenericContainer pubSubContainer = createPubSubContainer();
        pubSubContainer.start();
    }
}