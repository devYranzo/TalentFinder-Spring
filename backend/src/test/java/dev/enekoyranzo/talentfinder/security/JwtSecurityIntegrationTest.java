package dev.enekoyranzo.talentfinder.security;

import dev.enekoyranzo.talentfinder.support.AbstractIntegrationTest;
import dev.enekoyranzo.talentfinder.support.ApiClient;
import dev.enekoyranzo.talentfinder.support.TestApp;
import dev.enekoyranzo.talentfinder.support.TestTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/** Comprueba que el servidor rechaza tokens falsificados, caducados o manipulados. */
class JwtSecurityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    SecurityProperties props;

    private TestApp.Tenant tenant;
    private long userId;

    @BeforeEach
    void preparar() {
        tenant = app().newTenant();
        userId = jdbc.queryForObject("select id from users where lower(email) = ?", Long.class,
                tenant.adminEmail());
    }

    private byte[] key() {
        return Base64.getDecoder().decode(props.jwtSecret().trim());
    }

    private long future() {
        return Instant.now().getEpochSecond() + 3600;
    }

    private int meWith(String token) {
        ApiClient c = new ApiClient(baseUrl());
        c.setCookie("access_token", token);
        return c.get("/auth/me").status();
    }

    @Test
    void unTokenBienFormadoYFirmadoConLaClaveCorrectaEsAceptado() {
        // Comprobación de cordura: si esto fallara, los rechazos de abajo no significarían nada.
        String token = TestTokens.signed(key(),
                TestTokens.payload(JwtService.ISSUER, userId, tenant.organizationId(), future()));

        assertThat(meWith(token)).isEqualTo(200);
    }

    @Test
    void unTokenFirmadoConOtraClaveSeRechaza() {
        byte[] otherKey = new byte[32];
        Arrays.fill(otherKey, (byte) 'b');

        String token = TestTokens.signed(otherKey,
                TestTokens.payload(JwtService.ISSUER, userId, tenant.organizationId(), future()));

        assertThat(meWith(token)).isEqualTo(401);
    }

    @Test
    void unTokenCaducadoSeRechaza() {
        long expired = Instant.now().getEpochSecond() - 3600;

        String token = TestTokens.signed(key(),
                TestTokens.payload(JwtService.ISSUER, userId, tenant.organizationId(), expired));

        assertThat(meWith(token)).isEqualTo(401);
    }

    @Test
    void unTokenDeOtroEmisorSeRechaza() {
        String token = TestTokens.signed(key(),
                TestTokens.payload("otro-emisor", userId, tenant.organizationId(), future()));

        assertThat(meWith(token)).isEqualTo(401);
    }

    @Test
    void unTokenSinFirmaConAlgNoneSeRechaza() {
        String token = TestTokens.unsigned(
                TestTokens.payload(JwtService.ISSUER, userId, tenant.organizationId(), future()));

        assertThat(meWith(token)).isEqualTo(401);
    }

    @Test
    void unTokenDeUnUsuarioQueNoExisteSeRechaza() {
        String token = TestTokens.signed(key(),
                TestTokens.payload(JwtService.ISSUER, 999_999_999L, tenant.organizationId(), future()));

        assertThat(meWith(token)).isEqualTo(401);
    }

    @Test
    void unTokenConOrganizacionDistintaALaDelUsuarioSeRechaza() {
        // Aunque estuviera bien firmado, el claim de organización debe coincidir con la base de datos.
        String token = TestTokens.signed(key(),
                TestTokens.payload(JwtService.ISSUER, userId, tenant.organizationId() + 1000, future()));

        assertThat(meWith(token)).isEqualTo(401);
    }

    @Test
    void unTokenValidoManipuladoSeRechaza() {
        String valid = tenant.admin().cookie("access_token");
        String[] parts = valid.split("\\.");
        char[] payload = parts[1].toCharArray();
        int i = payload.length / 2;
        payload[i] = payload[i] == 'A' ? 'B' : 'A';

        String tampered = parts[0] + "." + new String(payload) + "." + parts[2];

        assertThat(meWith(tampered)).isEqualTo(401);
    }

    @Test
    void cookiesConBasuraOVaciasSeTratanComoSinSesion() {
        assertThat(meWith("basura")).isEqualTo(401);
        assertThat(meWith("")).isEqualTo(401);
        assertThat(meWith("a.b.c")).isEqualTo(401);
    }
}