package dev.enekoyranzo.talentfinder.auth;

import dev.enekoyranzo.talentfinder.security.SecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    private static final String EMAIL = "ana@talentfinder.test";
    private static final String IP = "10.0.0.1";
    private static final int MAX_ATTEMPTS = 3;

    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        SecurityProperties props = new SecurityProperties(
                "no-se-usa-aqui", 4, List.of("http://localhost"), false, MAX_ATTEMPTS, 15);
        service = new LoginAttemptService(props);
    }

    private void fail(String email, String ip, int times) {
        for (int i = 0; i < times; i++) {
            service.recordFailure(email, ip);
        }
    }

    @Test
    void sinIntentosFallidosNoEstaBloqueado() {
        assertThat(service.isLocked(EMAIL, IP)).isFalse();
    }

    @Test
    void noBloqueaAntesDeAlcanzarElMaximo() {
        fail(EMAIL, IP, MAX_ATTEMPTS - 1);

        assertThat(service.isLocked(EMAIL, IP)).isFalse();
    }

    @Test
    void bloqueaAlAlcanzarElMaximo() {
        fail(EMAIL, IP, MAX_ATTEMPTS);

        assertThat(service.isLocked(EMAIL, IP)).isTrue();
    }

    @Test
    void elBloqueoSeMantieneConIntentosAdicionales() {
        fail(EMAIL, IP, MAX_ATTEMPTS + 5);

        assertThat(service.isLocked(EMAIL, IP)).isTrue();
    }

    @Test
    void elBloqueoNoAfectaAOtraIp() {
        fail(EMAIL, IP, MAX_ATTEMPTS);

        assertThat(service.isLocked(EMAIL, "10.0.0.2")).isFalse();
    }

    @Test
    void elBloqueoNoAfectaAOtroEmail() {
        fail(EMAIL, IP, MAX_ATTEMPTS);

        assertThat(service.isLocked("otra@talentfinder.test", IP)).isFalse();
    }

    @Test
    void elEmailSeNormalizaSinMayusculasNiEspacios() {
        // Si no se normalizara, bastaría con variar las mayúsculas para esquivar el límite.
        service.recordFailure("ANA@talentfinder.test", IP);
        service.recordFailure("  ana@TALENTFINDER.test ", IP);
        service.recordFailure(EMAIL, IP);

        assertThat(service.isLocked(EMAIL, IP)).isTrue();
    }

    @Test
    void unLoginCorrectoReiniciaElContador() {
        fail(EMAIL, IP, MAX_ATTEMPTS - 1);
        service.recordSuccess(EMAIL, IP);
        fail(EMAIL, IP, MAX_ATTEMPTS - 1);

        assertThat(service.isLocked(EMAIL, IP)).isFalse();
    }
}