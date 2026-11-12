package dev.enekoyranzo.talentfinder.auth;

import dev.enekoyranzo.talentfinder.audit.AuditService;
import dev.enekoyranzo.talentfinder.auth.dto.LoginRequest;
import dev.enekoyranzo.talentfinder.auth.dto.MeResponse;
import dev.enekoyranzo.talentfinder.auth.dto.RegisterCompanyRequest;
import dev.enekoyranzo.talentfinder.security.AuthenticatedUser;
import dev.enekoyranzo.talentfinder.security.SecurityConfig;
import dev.enekoyranzo.talentfinder.security.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final AuditService audit;
    private final SecurityProperties props;

    public AuthController(AuthService authService, AuditService audit, SecurityProperties props) {
        this.authService = authService;
        this.audit = audit;
        this.props = props;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequest body,
                                                     HttpServletRequest request) {
        return authService.login(body.email(), body.password(), clientIp(request))
                .map(token -> ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE,
                                accessCookie(token, Duration.ofHours(props.accessTokenHours())).toString())
                        .body(Map.of("message", "Logeado correctamente")))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("detail", "Credenciales inválidas")));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@AuthenticationPrincipal AuthenticatedUser user,
                                                      HttpServletRequest request) {
        audit.record(user.organizationId(), user.userId(), "LOGOUT", "SUCCESS", clientIp(request), null);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie("", Duration.ZERO).toString())
                .body(Map.of("message", "Sesión cerrada"));
    }

    @PostMapping("/register-company")
    public ResponseEntity<Map<String, String>> registerCompany(@Valid @RequestBody RegisterCompanyRequest body,
                                                               HttpServletRequest request) {
        authService.registerCompany(body, clientIp(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Empresa registrada"));
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return authService.me(user.userId());
    }

    // ----------------------------------------------------------------- helpers
    private ResponseCookie accessCookie(String value, Duration maxAge) {
        return ResponseCookie.from(SecurityConfig.ACCESS_COOKIE, value)
                .httpOnly(true)
                .secure(props.cookieSecure())
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    /**
     * Devuelve la IP de la conexión directa. No leas X-Forwarded-For a mano: es falsificable.
     * Cuando haya un proxy inverso de confianza, se configurará server. Forward-headers-strategy.
     */
    private static String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}