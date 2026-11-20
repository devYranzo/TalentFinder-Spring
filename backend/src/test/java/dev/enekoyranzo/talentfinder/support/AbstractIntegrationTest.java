package dev.enekoyranzo.talentfinder.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base de todos los tests de integración: aplicación completa contra un PostgreSQL real.
 * El contenedor es único para toda la ejecución (se arranca una vez y se comparte).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestStorageConfig.class)
public abstract class AbstractIntegrationTest {

    // Misma imagen que en desarrollo. asCompatibleSubstituteFor: Testcontainers la trata como PostgreSQL.
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("pgvector/pgvector:pg18").asCompatibleSubstituteFor("postgres"));

    static {
        POSTGRES.start();
    }
}