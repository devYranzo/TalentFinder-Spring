package dev.enekoyranzo.talentfinder.security;

import dev.enekoyranzo.talentfinder.user.User;
import dev.enekoyranzo.talentfinder.user.UserRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Convierte un JWT válido en un Authentication cuyo principal es un AuthenticatedUser.
 * Recarga el usuario de la BD en cada petición: un usuario desactivado o eliminado
 * pierde el acceso de inmediato, sin esperar a que el token caduque.
 */
@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository users;

    public JwtAuthConverter(UserRepository users) {
        this.users = users;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        long userId;
        try {
            userId = Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException e) {
            throw new BadCredentialsException("Token inválido");
        }

        // findById sin organización es correcto aquí: el id viene de un token firmado por nosotros.
        User user = users.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new BadCredentialsException("Token inválido"));

        // El token debe corresponder a la organización actual del usuario.
        // OJO: se asigna a un Object antes de convertir. Pasar jwt.getClaim(...) directamente a
        // String.valueOf(...) selecciona la sobrecarga char[] y lanza ClassCastException.
        Object orgClaim = jwt.getClaim("org");
        if (orgClaim == null
                || !String.valueOf(user.getOrganizationId()).equals(String.valueOf(orgClaim))) {
            throw new BadCredentialsException("Token inválido");
        }

        AuthenticatedUser principal = new AuthenticatedUser(
                user.getId(), user.getOrganizationId(), user.getEmail(), user.getRole());

        return new UsernamePasswordAuthenticationToken(
                principal,
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}