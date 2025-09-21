package kirillzhdanov.identityservice.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
// Контейнеры поднимает TestEnvironment один раз на JVM
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTestBase {

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        String postgresHost = TestEnvironment.getPostgresHost();
        Integer postgresPort = TestEnvironment.getPostgresPort();

        registry.add("spring.datasource.url", () -> String.format("jdbc:postgresql://%s:%d/identity_db", postgresHost, postgresPort));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");

        String kafkaHost = TestEnvironment.getKafkaHost();
        Integer kafkaPort = TestEnvironment.getKafkaPort();

        registry.add("spring.kafka.bootstrap-servers", () -> String.format("%s:%d", kafkaHost, kafkaPort));

        // Ускоряем выключение пула и уменьшаем шум логов
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "5");
        registry.add("spring.datasource.hikari.minimum-idle", () -> "0");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "3000");
        registry.add("spring.datasource.hikari.validation-timeout", () -> "1000");
        registry.add("spring.datasource.hikari.idle-timeout", () -> "5000");
        registry.add("spring.datasource.hikari.max-lifetime", () -> "15000");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.open-in-view", () -> "false");
        registry.add("spring.liquibase.enabled", () -> "false");
        registry.add("logging.level.root", () -> "WARN");
        registry.add("logging.level.org.hibernate.SQL", () -> "INFO");
    }
}
