package dev.enekoyranzo.talentfinder.user;

import dev.enekoyranzo.talentfinder.user.dto.CreateUserRequest;
import dev.enekoyranzo.talentfinder.user.dto.UpdateUserRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UserService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public List<User> list(Long organizationId) {
        return users.findAllByOrganizationIdOrderByLastNameAscFirstNameAsc(organizationId);
    }

    @Transactional
    public User create(Long organizationId, CreateUserRequest req) {
        if (users.existsByEmailIgnoreCase(req.email().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se pudo crear el usuario.");
        }

        return users.saveAndFlush(new User(
                organizationId,
                req.email(),
                encoder.encode(req.password()),
                req.firstName(),
                req.lastName(),
                req.role()));
    }

    @Transactional
    public User update(Long organizationId, Long id, UpdateUserRequest req) {
        User target = find(organizationId, id);

        boolean demotes = req.role() != null && req.role() != Role.ADMIN;
        boolean deactivates = Boolean.FALSE.equals(req.active());
        if (isActiveAdmin(target) && (demotes || deactivates)) {
            ensureAnotherActiveAdmin(organizationId, target.getId());
        }

        if (req.firstName() != null) {
            target.setFirstName(req.firstName());
        }
        if (req.lastName() != null) {
            target.setLastName(req.lastName());
        }
        if (req.role() != null) {
            target.setRole(req.role());
        }
        if (req.active() != null) {
            target.setActive(req.active());
        }

        return users.saveAndFlush(target);
    }

    @Transactional
    public void delete(Long organizationId, Long actorId, Long id) {
        if (id.equals(actorId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No puedes eliminar tu propia cuenta.");
        }

        User target = find(organizationId, id);

        if (isActiveAdmin(target)) {
            ensureAnotherActiveAdmin(organizationId, target.getId());
        }

        users.delete(target);
    }

    // --------------------------------------------------------------- helpers
    private User find(Long organizationId, Long id) {
        // 404 también cuando el usuario existe en otra organización: no se revela su existencia.
        return users.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));
    }

    private static boolean isActiveAdmin(User u) {
        return u.isActive() && u.getRole() == Role.ADMIN;
    }

    private void ensureAnotherActiveAdmin(Long organizationId, Long targetId) {
        List<User> admins = users.lockActiveByRole(organizationId, Role.ADMIN);
        boolean otherExists = admins.stream().anyMatch(a -> !a.getId().equals(targetId));

        if (!otherExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La organización debe conservar al menos un administrador activo.");
        }
    }
}