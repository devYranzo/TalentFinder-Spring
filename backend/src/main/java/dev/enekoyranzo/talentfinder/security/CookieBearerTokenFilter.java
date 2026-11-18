package dev.enekoyranzo.talentfinder.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Copia el JWT de la cookie a una cabecera Authorization: Bearer, para que lo valide el
 * resource server de Spring Security.
 *
 * IMPORTANTE: debe ir DESPUÉS de CsrfFilter. Si CSRF ve una petición con cabecera Bearer,
 * Spring la exime de protección CSRF; aquí la cabecera se añade más tarde y la protección se aplica.
 */
public class CookieBearerTokenFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (request.getHeader(HttpHeaders.AUTHORIZATION) == null) {
            String token = readCookie(request);
            if (token != null) {
                chain.doFilter(new BearerRequest(request, token), response);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private static String readCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (SecurityConfig.ACCESS_COOKIE.equals(c.getName())
                    && c.getValue() != null && !c.getValue().isBlank()) {
                return c.getValue();
            }
        }
        return null;
    }

    private static final class BearerRequest extends HttpServletRequestWrapper {

        private final String authorization;

        BearerRequest(HttpServletRequest request, String token) {
            super(request);
            this.authorization = "Bearer " + token;
        }

        @Override
        public String getHeader(String name) {
            return HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name) ? authorization : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)
                    ? Collections.enumeration(List.of(authorization))
                    : super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new LinkedHashSet<>(Collections.list(super.getHeaderNames()));
            names.add(HttpHeaders.AUTHORIZATION);
            return Collections.enumeration(names);
        }
    }
}