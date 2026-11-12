package dev.enekoyranzo.talentfinder.user;

import dev.enekoyranzo.talentfinder.audit.AuditService;
import dev.enekoyranzo.talentfinder.security.AuthenticatedUser;
import dev.enekoyranzo.talentfinder.user.dto.CreateUserRequest;
import dev.enekoyranzo.talentfinder.user.dto.MemberResponse;
import dev.enekoyranzo.talentfinder.user.dto.UpdateUserRequest;
import dev.enekoyranzo.talentfinder.user.dto.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;
    private final AuditService audit;

    public UserController(UserService userService, AuditService audit) {
        this.userService = userService;
        this.audit = audit;
    }

    @GetMapping
    public List<UserResponse> list(@AuthenticationPrincipal AuthenticatedUser actor) {
        return userService.list(actor.organizationId()).stream().map(UserResponse::from).toList();
    }

    @GetMapping("/members")
    @PreAuthorize("hasRole('RECRUITER')")
    public List<MemberResponse> members(@AuthenticationPrincipal AuthenticatedUser actor) {
        return userService.list(actor.organizationId()).stream()
                .filter(User::isActive)
                .map(MemberResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest body,
                                               @AuthenticationPrincipal AuthenticatedUser actor,
                                               HttpServletRequest request) {
        User created = userService.create(actor.organizationId(), body);

        audit.record(actor.organizationId(), actor.userId(), "USER_CREATE", "SUCCESS",
                clientIp(request), "target=" + created.getId() + ", role=" + created.getRole());

        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(created));
    }

    @PatchMapping("/{id}")
    public UserResponse update(@PathVariable Long id,
                               @Valid @RequestBody UpdateUserRequest body,
                               @AuthenticationPrincipal AuthenticatedUser actor,
                               HttpServletRequest request) {
        User updated = userService.update(actor.organizationId(), id, body);

        audit.record(actor.organizationId(), actor.userId(), "USER_UPDATE", "SUCCESS",
                clientIp(request),
                "target=" + id + ", role=" + updated.getRole() + ", active=" + updated.isActive());

        return UserResponse.from(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal AuthenticatedUser actor,
                                       HttpServletRequest request) {
        userService.delete(actor.organizationId(), actor.userId(), id);

        audit.record(actor.organizationId(), actor.userId(), "USER_DELETE", "SUCCESS",
                clientIp(request), "target=" + id);

        return ResponseEntity.noContent().build();
    }

    private static String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}