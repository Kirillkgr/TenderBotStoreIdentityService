package kirillzhdanov.identityservice.config;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
// Контейнеры поднимает TestEnvironment один раз на JVM
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

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
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "10");
        registry.add("spring.datasource.hikari.minimum-idle", () -> "0");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "3000");
        registry.add("spring.datasource.hikari.validation-timeout", () -> "1000");
        registry.add("spring.datasource.hikari.idle-timeout", () -> "5000");
        registry.add("spring.datasource.hikari.max-lifetime", () -> "15000");
        // Use Liquibase to create schema in tests; avoid Hibernate auto DDL
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.open-in-view", () -> "false");
        registry.add("spring.liquibase.enabled", () -> "true");
        registry.add("spring.liquibase.change-log", () -> "classpath:/db/changelog/db.changelog-master.xml");
        registry.add("logging.level.root", () -> "WARN");
        registry.add("logging.level.org.hibernate.SQL", () -> "INFO");
    }

    @BeforeAll
    void cleanDatabaseOncePerClass() {
        // Очистка всех пользовательских таблиц между тестами (сохраняем таблицы Liquibase)
        jdbc.execute("""
                DO $$
                DECLARE
                  stm TEXT;
                BEGIN
                  SELECT 'TRUNCATE TABLE ' || string_agg(format('%I.%I', schemaname, tablename), ', ') || ' RESTART IDENTITY CASCADE'
                  INTO stm
                  FROM pg_tables
                  WHERE schemaname = 'public'
                    AND tablename NOT IN ('databasechangelog', 'databasechangeloglock', 'roles');
                  IF stm IS NOT NULL THEN EXECUTE stm; END IF;
                END$$;""");

        // Базовые роли для регистрации пользователей
        Integer roleCount = jdbc.queryForObject("SELECT COUNT(*) FROM roles", Integer.class);
        if (roleCount != null && roleCount == 0) {
            jdbc.batchUpdate(
                    "INSERT INTO roles(name) VALUES ('OWNER') ON CONFLICT DO NOTHING",
                    "INSERT INTO roles(name) VALUES ('ADMIN') ON CONFLICT DO NOTHING",
                    "INSERT INTO roles(name) VALUES ('USER') ON CONFLICT DO NOTHING"
            );
        }
    }
}
