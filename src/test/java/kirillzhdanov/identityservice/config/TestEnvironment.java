package kirillzhdanov.identityservice.config;

import java.io.File;

import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Starts docker-compose (Postgres) ONCE per JVM for all tests.
 * Avoids per-class up/down which may cause races and long teardown times.
 */
public final class TestEnvironment {
    private static final String COMPOSE_FILE_PATH = "docker-compose-testcontainers.yml";
    private static final String POSTGRES_SERVICE_NAME = "postgres";
    private static final int POSTGRES_SERVICE_PORT = 5432;

    public static final DockerComposeContainer<?> COMPOSE = createAndStart();

    @SuppressWarnings("resource")
    private static DockerComposeContainer<?> createAndStart() {
        DockerComposeContainer<?> compose = new DockerComposeContainer<>(new File(COMPOSE_FILE_PATH))
                .withExposedService(POSTGRES_SERVICE_NAME, POSTGRES_SERVICE_PORT, Wait.forListeningPort());
        compose.start();
        // Ensure the docker-compose environment is stopped when the JVM exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                compose.stop();
            } catch (Exception ignored) {
                // no-op
            }
        }));
        return compose;
    }

    private TestEnvironment() {}

    public static String getPostgresHost() {
        return COMPOSE.getServiceHost(POSTGRES_SERVICE_NAME, POSTGRES_SERVICE_PORT);
    }

    public static Integer getPostgresPort() {
        return COMPOSE.getServicePort(POSTGRES_SERVICE_NAME, POSTGRES_SERVICE_PORT);
    }
}
