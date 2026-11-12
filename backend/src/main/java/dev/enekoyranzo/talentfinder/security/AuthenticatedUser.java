package dev.enekoyranzo.talentfinder.security;

import dev.enekoyranzo.talentfinder.user.Role;

/**
 * Principal que viaja en el SecurityContext. El organizationId SIEMPRE se toma de aquí,
 * nunca de parámetros de la petición: es lo que garantiza el aislamiento entre organizaciones.
 */
public record AuthenticatedUser(Long userId, Long organizationId, String email, Role role) {
}