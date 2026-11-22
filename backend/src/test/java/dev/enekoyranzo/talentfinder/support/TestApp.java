package dev.enekoyranzo.talentfinder.support;

import dev.enekoyranzo.talentfinder.support.ApiClient.Csrf;
import dev.enekoyranzo.talentfinder.user.Role;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

/** Atajos para preparar escenarios: organizaciones, usuarios con sesión y documentos. */
public final class TestApp {

    public static final String PASSWORD = "Contrasena-Segura-123";

    /** Una organización recién registrada, con su administrador ya identificado. */
    public record Tenant(long organizationId, String companyName, String adminEmail, ApiClient admin) {
    }

    public record Member(long id, String email, ApiClient client) {
    }

    private final String base;
    private final JdbcTemplate jdbc;

    public TestApp(String base, JdbcTemplate jdbc) {
        this.base = base;
        this.jdbc = jdbc;
    }

    public static String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public Tenant newTenant() {
        String tag = unique();
        String company = "Empresa " + tag;
        String email = "admin-" + tag + "@talentfinder.test";

        ApiClient c = new ApiClient(base);
        ApiClient.Response r = c.postJson("/auth/register-company",
                ApiClient.obj("companyName", company, "email", email, "password", PASSWORD,
                        "firstName", "Ana", "lastName", "Admin"), Csrf.NONE);
        if (r.status() != 201) {
            throw new IllegalStateException("No se pudo registrar la organización: HTTP " + r.status());
        }

        Long orgId = jdbc.queryForObject(
                "select organization_id from users where lower(email) = ?", Long.class, email);
        return new Tenant(orgId, company, email, login(email, PASSWORD));
    }

    public ApiClient login(String email, String password) {
        ApiClient c = new ApiClient(base);
        ApiClient.Response r = c.postJson("/auth/login",
                ApiClient.obj("email", email, "password", password), Csrf.NONE);
        if (r.status() != 200) {
            throw new IllegalStateException("Login fallido: HTTP " + r.status());
        }
        return c;
    }

    /** Crea un usuario en la organización (lo da de alta el administrador) y lo deja con sesión. */
    public Member addMember(Tenant tenant, Role role) {
        String email = role.name().toLowerCase() + "-" + unique() + "@talentfinder.test";
        ApiClient.Response r = tenant.admin().postJson("/users",
                ApiClient.obj("email", email, "password", PASSWORD,
                        "firstName", "Luis", "lastName", role.name(), "role", role.name()));
        if (r.status() != 201) {
            throw new IllegalStateException("No se pudo crear el usuario: HTTP " + r.status());
        }
        return new Member(r.id(), email, login(email, PASSWORD));
    }

    /** Sube un PDF válido y distinto cada vez. Devuelve el id del documento. */
    public long uploadPdf(ApiClient client, Long folderId) {
        ApiClient.Response r = client.upload("/documents", "cv-" + unique() + ".pdf",
                TestPdfs.unique(), folderId);
        if (r.status() != 201) {
            throw new IllegalStateException("No se pudo subir el PDF: HTTP " + r.status());
        }
        return r.id();
    }
}