package dev.enekoyranzo.talentfinder.common;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestUtils {

    private RequestUtils() {
    }

    /**
     * Devuelve la IP de la conexión directa. No leas X-Forwarded-For a mano: es falsificable.
     * Cuando haya un proxy inverso de confianza, se configurará server.forward-headers-strategy.
     */
    public static String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}