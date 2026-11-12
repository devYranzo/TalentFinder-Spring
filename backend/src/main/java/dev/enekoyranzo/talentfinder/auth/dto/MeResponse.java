package dev.enekoyranzo.talentfinder.auth.dto;

public record MeResponse(
        Long userId,
        String firstName,
        String lastName,
        String email,
        String role
) {
}