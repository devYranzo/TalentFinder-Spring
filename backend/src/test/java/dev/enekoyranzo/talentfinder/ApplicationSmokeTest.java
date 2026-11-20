package dev.enekoyranzo.talentfinder;

import dev.enekoyranzo.talentfinder.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationSmokeTest extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void arrancaYLasMigracionesCreanTodasLasTablas() {
        Integer tables = jdbc.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema = 'public'
                  and table_name in ('organizations', 'users', 'audit_log', 'folders', 'documents')
                """, Integer.class);

        assertThat(tables).isEqualTo(5);
    }

    @Test
    void ningunaMigracionHaFallado() {
        Integer failed = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success = false", Integer.class);

        assertThat(failed).isZero();
    }
}