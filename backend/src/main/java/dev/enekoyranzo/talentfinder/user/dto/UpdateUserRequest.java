package dev.enekoyranzo.talentfinder.user.dto;

import dev.enekoyranzo.talentfinder.user.Role;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 100) @Pattern(regexp = ".*\\S.*", message = "no puede estar vacío") String firstName,
        @Size(max = 100) @Pattern(regexp = ".*\\S.*", message = "no puede estar vacío") String lastName,
        Role role,
        Boolean active
) {
}