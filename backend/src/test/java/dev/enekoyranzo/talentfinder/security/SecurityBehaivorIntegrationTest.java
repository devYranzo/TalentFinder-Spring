package dev.enekoyranzo.talentfinder.security;

import dev.enekoyranzo.talentfinder.support.AbstractIntegrationTest;
import dev.enekoyranzo.talentfinder.support.ApiClient;
import dev.enekoyranzo.talentfinder.support.ApiClient.Csrf;
import dev.enekoyranzo.talentfinder.support.ApiClient.Response;
import dev.enekoyranzo.talentfinder.support.TestApp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static dev.enekoyranzo.talentfinder.support.ApiClient.obj;
import static org.assertj.core.api.Assertions.assertThat;

/** Comportamiento transversal: CSRF, códigos 401/403, cabeceras, CORS y endpoints expuestos. */
class SecurityBehaviorIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    SecurityProperties props;

    private ApiClient anon() {
        return new ApiClient(baseUrl());
    }

    // ---------------------------------------------------------- sin sesión: 401
    @Test
    void lasRutasProtegidasSinSesionDan401() {
        for (String path : List.of("/auth/me", "/users", "/users/members", "/folders", "/documents",
                "/documents/1/file")) {
            assertThat(anon().get(path).status()).as("GET " + path).isEqualTo(401);
        }
    }

    @Test
    void lasEscriturasSinSesionDan401() {
        assertThat(anon().postJson("/users", obj("email", "a@b.co")).status()).isEqualTo(401);
        assertThat(anon().postJson("/folders", obj("name", "x")).status()).isEqualTo(401);
        assertThat(anon().delete("/documents/1").status()).isEqualTo(401);
    }

    @Test
    void laRutaDeErroresNoEsAccesibleDirectamenteSinSesion() {
        assertThat(anon().get("/error").status()).isEqualTo(401);
    }

    // -------------------------------------------------------------------- CSRF
    @Test
    void laPrimeraRespuestaEntregaLaCookieCsrf() {
        ApiClient c = anon();

        Response r = c.get("/auth/me");

        assertThat(r.status()).isEqualTo(401);
        assertThat(c.cookie("XSRF-TOKEN")).isNotBlank();
    }

    @Test
    void laCookieCsrfPersisteEntrePeticionesAutenticadas() {
        // Regresión: Spring Security la invalidaba en cada petición autenticada y la borraba.
        ApiClient c = app().newTenant().admin();
        c.get("/auth/me");
        String first = c.cookie("XSRF-TOKEN");
        assertThat(first).isNotBlank();

        for (int i = 0; i < 5; i++) {
            assertThat(c.get("/auth/me").status()).isEqualTo(200);
        }

        assertThat(c.cookie("XSRF-TOKEN")).isEqualTo(first);
    }

    @Test
    void variasEscriturasSeguidasFuncionanTodas() {
        // Regresión: antes fallaba una de cada dos.
        ApiClient c = app().newTenant().admin();

        for (int i = 0; i < 6; i++) {
            assertThat(c.postJson("/folders", obj("name", "Carpeta " + i)).status()).as("escritura " + i).isEqualTo(201);
        }
    }

    @Test
    void unaEscrituraSinCabeceraCsrfDa403NoUn401() {
        ApiClient c = app().newTenant().admin();

        assertThat(c.postJson("/folders", obj("name", "x"), Csrf.NONE).status()).isEqualTo(403);
        assertThat(c.patchJson("/folders/1", obj("name", "x"), Csrf.NONE).status()).isEqualTo(403);
        assertThat(c.delete("/documents/1", Csrf.NONE).status()).isEqualTo(403);
        assertThat(c.upload("/documents", "cv.pdf", new byte[]{1}, null, Csrf.NONE).status()).isEqualTo(403);
    }

    @Test
    void unaEscrituraConCabeceraCsrfFalsaDa403() {
        ApiClient c = app().newTenant().admin();

        assertThat(c.postJson("/folders", obj("name", "x"), Csrf.FAKE).status()).isEqualTo(403);
    }

    @Test
    void lasLecturasNoNecesitanCabeceraCsrf() {
        ApiClient c = app().newTenant().admin();

        assertThat(c.get("/folders").status()).isEqualTo(200);
    }

    // ------------------------------------------------------------- cabeceras
    @Test
    void lasRespuestasLlevanLasCabecerasDeSeguridad() {
        Response r = anon().get("/auth/me");

        assertThat(r.header("X-Frame-Options")).isEqualTo("DENY");
        assertThat(r.header("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(r.header("Referrer-Policy")).isEqualTo("no-referrer");
        assertThat(r.header("Content-Security-Policy")).contains("default-src 'none'");
        assertThat(r.header("Cache-Control")).contains("no-store");
    }

    // ------------------------------------------------------------------- CORS
    @Test
    void soloLosOrigenesPermitidosRecibenCabecerasCors() {
        String allowed = props.allowedOrigins().get(0);

        Response ok = anon().options("/users", allowed, "GET");
        assertThat(ok.header("Access-Control-Allow-Origin")).isEqualTo(allowed);
        assertThat(ok.header("Access-Control-Allow-Credentials")).isEqualTo("true");

        Response evil = anon().options("/users", "http://evil.example", "GET");
        assertThat(evil.header("Access-Control-Allow-Origin")).isNull();

        Response evilGet = anon().request("GET", "/auth/me", null, null, Csrf.NONE,
                Map.of("Origin", "http://evil.example"));
        assertThat(evilGet.header("Access-Control-Allow-Origin")).isNull();
    }

    // ------------------------------------------------------- endpoints expuestos
    @Test
    void soloElEstadoDeSaludEsPublicoEnActuator() {
        assertThat(anon().get("/actuator/health").status()).isEqualTo(200);

        for (String path : List.of("/actuator/env", "/actuator/beans", "/actuator/metrics", "/actuator/heapdump")) {
            assertThat(anon().get(path).status()).as(path).isNotEqualTo(200);
        }
    }

    @Test
    void lasRespuestasDeErrorNoFiltranDetallesInternos() {
        Response r = anon().postJson("/auth/login", "{ esto no es json", Csrf.NONE);

        assertThat(r.status()).isEqualTo(400);
        assertThat(r.body()).doesNotContain("Exception").doesNotContain("dev.enekoyranzo").doesNotContain("jackson");
    }
}