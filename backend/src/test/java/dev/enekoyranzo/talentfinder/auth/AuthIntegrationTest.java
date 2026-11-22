package dev.enekoyranzo.talentfinder.auth;

import dev.enekoyranzo.talentfinder.security.SecurityProperties;
import dev.enekoyranzo.talentfinder.support.AbstractIntegrationTest;
import dev.enekoyranzo.talentfinder.support.ApiClient;
import dev.enekoyranzo.talentfinder.support.ApiClient.Csrf;
import dev.enekoyranzo.talentfinder.support.ApiClient.Response;
import dev.enekoyranzo.talentfinder.support.TestApp;
import dev.enekoyranzo.talentfinder.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static dev.enekoyranzo.talentfinder.support.ApiClient.obj;
import static org.assertj.core.api.Assertions.assertThat;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    SecurityProperties securityProps;

    private String loginBody(String email, String password) {
        return obj("email", email, "password", password);
    }

    private ApiClient anon() {
        return new ApiClient(baseUrl());
    }

    private int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    // ------------------------------------------------------------------- login
    @Test
    void loginCorrectoEmiteUnaCookieDeSesionSegura() {
        TestApp.Tenant t = app().newTenant();

        Response r = anon().postJson("/auth/login", loginBody(t.adminEmail(), TestApp.PASSWORD), Csrf.NONE);

        assertThat(r.status()).isEqualTo(200);
        String cookie = r.setCookies().stream()
                .filter(c -> c.startsWith("access_token=")).findFirst().orElseThrow();
        assertThat(cookie).contains("HttpOnly").contains("Secure").contains("SameSite=Strict")
                .contains("Path=/").contains("Max-Age=" + securityProps.accessTokenHours() * 3600);
    }

    @Test
    void elEmailNoDistingueMayusculasNiEspaciosAlRodear() {
        TestApp.Tenant t = app().newTenant();

        Response r = anon().postJson("/auth/login",
                loginBody(t.adminEmail().toUpperCase(), TestApp.PASSWORD), Csrf.NONE);

        assertThat(r.status()).isEqualTo(200);
    }

    @Test
    void credencialesInvalidasDan401SinCookieYConElMismoMensajeQueUnEmailInexistente() {
        TestApp.Tenant t = app().newTenant();

        Response wrongPassword = anon().postJson("/auth/login",
                loginBody(t.adminEmail(), "otra-contrasena-123"), Csrf.NONE);
        Response unknownEmail = anon().postJson("/auth/login",
                loginBody("nadie-" + TestApp.unique() + "@talentfinder.test", "otra-contrasena-123"), Csrf.NONE);

        assertThat(wrongPassword.status()).isEqualTo(401);
        assertThat(unknownEmail.status()).isEqualTo(401);
        // Misma respuesta: no se puede averiguar qué emails existen.
        assertThat(wrongPassword.body()).isEqualTo(unknownEmail.body());
        assertThat(wrongPassword.setCookies()).noneMatch(c -> c.startsWith("access_token="));
    }

    @Test
    void unUsuarioDesactivadoNoPuedeIniciarSesion() {
        TestApp.Tenant t = app().newTenant();
        TestApp.Member member = app().addMember(t, Role.RECRUITER);

        assertThat(t.admin().patchJson("/users/" + member.id(), obj("active", false)).status()).isEqualTo(200);

        Response r = anon().postJson("/auth/login", loginBody(member.email(), TestApp.PASSWORD), Csrf.NONE);
        assertThat(r.status()).isEqualTo(401);
    }

    @Test
    void tras_los_intentos_maximos_la_cuenta_se_bloquea_aunque_la_contrasena_sea_correcta() {
        TestApp.Tenant locked = app().newTenant();
        TestApp.Tenant other = app().newTenant();
        ApiClient attacker = anon();

        for (int i = 0; i < securityProps.loginMaxAttempts(); i++) {
            assertThat(attacker.postJson("/auth/login", loginBody(locked.adminEmail(), "incorrecta-" + i),
                    Csrf.NONE).status()).isEqualTo(401);
        }

        Response withRightPassword = attacker.postJson("/auth/login",
                loginBody(locked.adminEmail(), TestApp.PASSWORD), Csrf.NONE);
        assertThat(withRightPassword.status()).isEqualTo(401);

        // El bloqueo es por cuenta: otra cuenta desde la misma IP sigue pudiendo entrar.
        Response otherAccount = anon().postJson("/auth/login",
                loginBody(other.adminEmail(), TestApp.PASSWORD), Csrf.NONE);
        assertThat(otherAccount.status()).isEqualTo(200);
    }

    @Test
    void unLoginCorrectoReiniciaElContadorDeFallos() {
        TestApp.Tenant t = app().newTenant();
        ApiClient c = anon();

        for (int i = 0; i < securityProps.loginMaxAttempts() - 1; i++) {
            c.postJson("/auth/login", loginBody(t.adminEmail(), "incorrecta"), Csrf.NONE);
        }
        assertThat(c.postJson("/auth/login", loginBody(t.adminEmail(), TestApp.PASSWORD), Csrf.NONE).status())
                .isEqualTo(200);
        for (int i = 0; i < securityProps.loginMaxAttempts() - 1; i++) {
            c.postJson("/auth/login", loginBody(t.adminEmail(), "incorrecta"), Csrf.NONE);
        }

        assertThat(c.postJson("/auth/login", loginBody(t.adminEmail(), TestApp.PASSWORD), Csrf.NONE).status())
                .isEqualTo(200);
    }

    @Test
    void loginRechazaEntradasInvalidasConError400() {
        assertThat(anon().postJson("/auth/login", loginBody("no-es-un-email", "x"), Csrf.NONE).status())
                .isEqualTo(400);
        assertThat(anon().postJson("/auth/login",
                loginBody("a@talentfinder.test", "x".repeat(129)), Csrf.NONE).status()).isEqualTo(400);
        assertThat(anon().postJson("/auth/login", "{}", Csrf.NONE).status()).isEqualTo(400);
    }

    @Test
    void loginRegistraAuditoriaSinGuardarElEmailDeUnUsuarioInexistente() {
        int failuresBefore = count("select count(*) from audit_log where action = 'LOGIN' and outcome = 'FAILURE'");
        String unknown = "nadie-" + TestApp.unique() + "@talentfinder.test";

        anon().postJson("/auth/login", loginBody(unknown, "incorrecta-123"), Csrf.NONE);

        assertThat(count("select count(*) from audit_log where action = 'LOGIN' and outcome = 'FAILURE'"))
                .isEqualTo(failuresBefore + 1);
        assertThat(count("select count(*) from audit_log where details like ?", "%" + unknown + "%")).isZero();
        assertThat(count("""
                select count(*) from audit_log
                where id = (select max(id) from audit_log where action = 'LOGIN' and outcome = 'FAILURE')
                  and user_id is null and organization_id is null and details is null
                """)).isEqualTo(1);
    }

    @Test
    void loginCorrectoQuedaAuditadoConElUsuario() {
        TestApp.Tenant t = app().newTenant();
        Long userId = jdbc.queryForObject("select id from users where lower(email) = ?", Long.class, t.adminEmail());

        assertThat(count("select count(*) from audit_log where action = 'LOGIN' and outcome = 'SUCCESS' and user_id = ?",
                userId)).isEqualTo(1);
    }

    // ---------------------------------------------------------------------- me
    @Test
    void meSinSesionDa401() {
        assertThat(anon().get("/auth/me").status()).isEqualTo(401);
    }

    @Test
    void meConSesionDevuelveLosDatosDelUsuarioYNingunSecreto() {
        TestApp.Tenant t = app().newTenant();

        Response r = t.admin().get("/auth/me");

        assertThat(r.status()).isEqualTo(200);
        assertThat(r.field("email")).isEqualTo(t.adminEmail());
        assertThat(r.field("role")).isEqualTo("ADMIN");
        assertThat(r.body().toLowerCase()).doesNotContain("password").doesNotContain("hash").doesNotContain("argon");
    }

    // ------------------------------------------------------------------ logout
    @Test
    void logoutBorraLaCookieDeSesion() {
        TestApp.Tenant t = app().newTenant();
        ApiClient c = t.admin();

        Response r = c.post("/auth/logout");

        assertThat(r.status()).isEqualTo(200);
        assertThat(r.setCookies()).anyMatch(s -> s.startsWith("access_token=") && s.contains("Max-Age=0"));
        assertThat(c.cookie("access_token")).isNull();
        assertThat(c.get("/auth/me").status()).isEqualTo(401);
    }

    @Test
    void logoutExigeCsrfYSesion() {
        TestApp.Tenant t = app().newTenant();

        assertThat(t.admin().post("/auth/logout", Csrf.NONE).status()).isEqualTo(403);
        assertThat(anon().post("/auth/logout").status()).isEqualTo(401);
    }

    // ---------------------------------------------------------------- registro
    @Test
    void registrarUnaEmpresaCreaSuAdministrador() {
        String tag = TestApp.unique();
        String email = "nueva-" + tag + "@talentfinder.test";

        Response r = anon().postJson("/auth/register-company", obj("companyName", "Nueva " + tag,
                "email", email, "password", TestApp.PASSWORD, "firstName", "Eva", "lastName", "Nueva"), Csrf.NONE);

        assertThat(r.status()).isEqualTo(201);
        ApiClient session = app().login(email, TestApp.PASSWORD);
        assertThat(session.get("/auth/me").field("role")).isEqualTo("ADMIN");
        assertThat(count("select count(*) from audit_log where action = 'REGISTER_COMPANY' and outcome = 'SUCCESS'"))
                .isGreaterThanOrEqualTo(1);
    }

    @Test
    void noSePuedeRegistrarUnEmailNiUnaEmpresaYaExistentes() {
        TestApp.Tenant t = app().newTenant();
        String tag = TestApp.unique();

        Response sameEmail = anon().postJson("/auth/register-company", obj("companyName", "Otra " + tag,
                "email", t.adminEmail().toUpperCase(), "password", TestApp.PASSWORD,
                "firstName", "Eva", "lastName", "Dos"), Csrf.NONE);
        Response sameCompany = anon().postJson("/auth/register-company", obj("companyName", t.companyName().toUpperCase(),
                "email", "otro-" + tag + "@talentfinder.test", "password", TestApp.PASSWORD,
                "firstName", "Eva", "lastName", "Dos"), Csrf.NONE);

        assertThat(sameEmail.status()).isEqualTo(409);
        assertThat(sameCompany.status()).isEqualTo(409);
    }

    @Test
    void elRegistroRechazaDatosInvalidos() {
        String tag = TestApp.unique();
        String ok = "ok-" + tag + "@talentfinder.test";

        assertThat(register("Empresa " + tag, ok, "corta-123", "Eva", "X").status()).isEqualTo(400);
        assertThat(register("Empresa " + tag, "no-es-email", TestApp.PASSWORD, "Eva", "X").status()).isEqualTo(400);
        assertThat(register("  ", ok, TestApp.PASSWORD, "Eva", "X").status()).isEqualTo(400);
        assertThat(register("Empresa " + tag, ok, TestApp.PASSWORD, "", "X").status()).isEqualTo(400);
    }

    private Response register(String company, String email, String password, String first, String last) {
        return anon().postJson("/auth/register-company",
                obj("companyName", company, "email", email, "password", password,
                        "firstName", first, "lastName", last), Csrf.NONE);
    }
}