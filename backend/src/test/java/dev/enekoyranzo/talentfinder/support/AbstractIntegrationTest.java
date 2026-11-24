package dev.enekoyranzo.talentfinder.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base de todos los tests de integración: aplicación completa, con servidor HTTP real en un
 * puerto aleatorio, contra un PostgreSQL real. El contenedor es único para toda la ejecución.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestStorageConfig.class, TestEmbeddingConfig.class})
public abstract class AbstractIntegrationTest {

    // Misma imagen que en desarrollo. asCompatibleSubstituteFor: Testcontainers la trata como PostgreSQL.
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("pgvector/pgvector:pg18").asCompatibleSubstituteFor("postgres"));

    static {
        POSTGRES.start();
    }

    @Value("${local.server.port}")
    protected int port;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected InMemoryCvStorage storage;

    @BeforeEach
    void resetStorage() {
        storage.reset();
    }

    protected String baseUrl() {
        return "http://localhost:" + port;
    }

    protected TestApp app() {
        return new TestApp(baseUrl(), jdbc);
    }
}