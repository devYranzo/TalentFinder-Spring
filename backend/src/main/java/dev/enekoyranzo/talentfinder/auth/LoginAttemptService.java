package dev.enekoyranzo.talentfinder.auth;

import dev.enekoyranzo.talentfinder.security.SecurityProperties;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita los intentos de login fallidos por grupo de email + IP.
 * Limitaciones conocidas:
 *  - Solo vale para UNA instancia (con varias réplicas habría que moverlo a Redis o a la BD).
 *  - No frena un ataque distribuido desde muchas IPs contra el mismo email.
 */
@Service
public class LoginAttemptService {

    private static final int PURGE_THRESHOLD = 10_000;

    private record Attempt(int failures, Instant lastFailure, Instant lockedUntil) {
    }

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final SecurityProperties props;

    public LoginAttemptService(SecurityProperties props) {
        this.props = props;
    }

    private static String key(String email, String ip) {
        return email.trim().toLowerCase() + "|" + ip;
    }

    private boolean isExpired(Attempt a, Instant now) {
        Instant until = a.lockedUntil() != null
                ? a.lockedUntil()
                : a.lastFailure().plus(Duration.ofMinutes(props.loginLockMinutes()));
        return now.isAfter(until);
    }

    public boolean isLocked(String email, String ip) {
        Instant now = Instant.now();
        Attempt a = attempts.get(key(email, ip));
        if (a == null) {
            return false;
        }
        if (isExpired(a, now)) {
            attempts.remove(key(email, ip));
            return false;
        }
        return a.lockedUntil() != null;
    }

    public void recordFailure(String email, String ip) {
        Instant now = Instant.now();

        attempts.compute(key(email, ip), (k, old) -> {
            int failures = (old == null || isExpired(old, now)) ? 1 : old.failures() + 1;
            Instant lock = failures >= props.loginMaxAttempts()
                    ? now.plus(Duration.ofMinutes(props.loginLockMinutes()))
                    : null;
            return new Attempt(failures, now, lock);
        });

        // Evita que el mapa crezca sin límite si alguien lo bombardea con claves distintas.
        if (attempts.size() > PURGE_THRESHOLD) {
            attempts.values().removeIf(a -> isExpired(a, now));
        }
    }

    public void recordSuccess(String email, String ip) {
        attempts.remove(key(email, ip));
    }
}