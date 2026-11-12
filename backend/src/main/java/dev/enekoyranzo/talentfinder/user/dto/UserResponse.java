package dev.enekoyranzo.talentfinder.user.dto;

import dev.enekoyranzo.talentfinder.user.User;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String role,
        boolean active
) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getFirstName(), u.getLastName(),
                u.getEmail(), u.getRole().name(), u.isActive());
    }
}