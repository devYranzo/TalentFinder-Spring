package dev.enekoyranzo.talentfinder.auth;

import dev.enekoyranzo.talentfinder.audit.AuditService;
import dev.enekoyranzo.talentfinder.auth.dto.MeResponse;
import dev.enekoyranzo.talentfinder.auth.dto.RegisterCompanyRequest;
import dev.enekoyranzo.talentfinder.organization.Organization;
import dev.enekoyranzo.talentfinder.organization.OrganizationRepository;
import dev.enekoyranzo.talentfinder.security.JwtService;
import dev.enekoyranzo.talentfinder.user.Role;
import dev.enekoyranzo.talentfinder.user.User;
import dev.enekoyranzo.talentfinder.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository users;
    private final OrganizationRepository organizations;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final LoginAttemptService attempts;
    private final AuditService audit;
    private final TransactionTemplate tx;

    // Hash falso para igualar el tiempo de respuesta cuando el email no existe (anti-enumeración).
    private final String dummyHash;

    public AuthService(UserRepository users, OrganizationRepository organizations,
                       PasswordEncoder encoder, JwtService jwt,
                       LoginAttemptService attempts, AuditService audit,
                       TransactionTemplate tx) {
        this.users = users;
        this.organizations = organizations;
        this.encoder = encoder;
        this.jwt = jwt;
        this.attempts = attempts;
        this.audit = audit;
        this.tx = tx;
        this.dummyHash = encoder.encode("contrasena-falsa-para-igualar-tiempos");
    }

    /**
     * @return el token si las credenciales son válidas; vacío en cualquier otro caso.
     * El motivo del fallo nunca se expone al cliente.
     */
    public Optional<String> login(String email, String password, String ip) {
        if (attempts.isLocked(email, ip)) {
            audit.record(null, null, "LOGIN", "LOCKED", ip, null);
            return Optional.empty();
        }

        Optional<User> found = users.findByEmailIgnoreCase(email.trim());

        // Se verifica siempre una contraseña, exista o no el usuario.
        boolean passwordOk = encoder.matches(password,
                found.map(User::getPasswordHash).orElse(dummyHash));

        if (found.isEmpty() || !passwordOk || !found.get().isActive()) {
            attempts.recordFailure(email, ip);
            User u = found.orElse(null);
            audit.record(u != null ? u.getOrganizationId() : null,
                    u != null ? u.getId() : null, "LOGIN", "FAILURE", ip, null);
            return Optional.empty();
        }

        User user = found.get();
        attempts.recordSuccess(email, ip);
        audit.record(user.getOrganizationId(), user.getId(), "LOGIN", "SUCCESS", ip, null);
        return Optional.of(jwt.createAccessToken(user));
    }

    public void registerCompany(RegisterCompanyRequest req, String ip) {
        // Mensaje genérico. Aun así, el 409 revela que el email o la empresa ya existen:
        if (organizations.existsByNameIgnoreCase(req.companyName().trim())
                || users.existsByEmailIgnoreCase(req.email().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se pudo completar el registro.");
        }

        // Transacción explícita: así el evento de auditoría solo se escribe si el commit tuvo éxito.
        User admin = tx.execute(status -> {
            Organization org = organizations.saveAndFlush(new Organization(req.companyName()));
            return users.saveAndFlush(new User(
                    org.getId(),
                    req.email(),
                    encoder.encode(req.password()),
                    req.firstName(),
                    req.lastName(),
                    Role.ADMIN));
        });

        audit.record(admin.getOrganizationId(), admin.getId(), "REGISTER_COMPANY", "SUCCESS", ip, null);
    }

    public MeResponse me(Long userId) {
        User u = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return new MeResponse(u.getId(), u.getFirstName(), u.getLastName(), u.getEmail(), u.getRole().name());
    }
}