package dev.enekoyranzo.talentfinder.support;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Base64;

/** Fabrica tokens JWT a mano para comprobar que el servidor rechaza lo que debe rechazar. */
public final class TestTokens {

    private TestTokens() {
    }

    public static String payload(String issuer, long userId, long orgId, long expEpochSeconds) {
        long now = Instant.now().getEpochSecond();
        return "{\"iss\":\"" + issuer + "\",\"sub\":\"" + userId + "\",\"org\":" + orgId
                + ",\"iat\":" + (now - 10) + ",\"exp\":" + expEpochSeconds + "}";
    }

    /** Token HS256 firmado con la clave indicada. */
    public static String signed(byte[] key, String payloadJson) {
        String input = b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}") + "." + b64(payloadJson);
        return input + "." + b64(hmac(key, input));
    }

    /** Token sin firma (alg none). */
    public static String unsigned(String payloadJson) {
        return b64("{\"alg\":\"none\",\"typ\":\"JWT\"}") + "." + b64(payloadJson) + ".";
    }

    private static byte[] hmac(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String b64(String s) {
        return b64(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String b64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}