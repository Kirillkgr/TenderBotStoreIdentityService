package kirillzhdanov.identityservice.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTestBase {

    private static final String COMPOSE_FILE_PATH = "docker-compose-test.yml";
    private static final String POSTGRES_SERVICE_NAME = "postgres";
    private static final int POSTGRES_SERVICE_PORT = 5432;
    private static final String KAFKA_SERVICE_NAME = "kafka";
    private static final int KAFKA_SERVICE_PORT = 29092;

    @Container
    public static DockerComposeContainer<?> compose = new DockerComposeContainer<>(new File(COMPOSE_FILE_PATH))
            .withExposedService(POSTGRES_SERVICE_NAME, POSTGRES_SERVICE_PORT, Wait.forListeningPort())
            .withExposedService(KAFKA_SERVICE_NAME, KAFKA_SERVICE_PORT, Wait.forListeningPort());

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        String postgresHost = compose.getServiceHost(POSTGRES_SERVICE_NAME, POSTGRES_SERVICE_PORT);
        Integer postgresPort = compose.getServicePort(POSTGRES_SERVICE_NAME, POSTGRES_SERVICE_PORT);

        registry.add("spring.datasource.url", () -> String.format("jdbc:postgresql://%s:%d/identity_db", postgresHost, postgresPort));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");

        String kafkaHost = compose.getServiceHost(KAFKA_SERVICE_NAME, KAFKA_SERVICE_PORT);
        Integer kafkaPort = compose.getServicePort(KAFKA_SERVICE_NAME, KAFKA_SERVICE_PORT);

        registry.add("spring.kafka.bootstrap-servers", () -> String.format("%s:%d", kafkaHost, kafkaPort));
    }
}
