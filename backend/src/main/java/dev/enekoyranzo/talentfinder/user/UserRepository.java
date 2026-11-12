package dev.enekoyranzo.talentfinder.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    // A partir de aquí, TODA consulta de negocio lleva organizationId.
    // Nunca uses findById a secas para datos que vienen de una petición.
    Optional<User> findByIdAndOrganizationId(Long id, Long organizationId);

    List<User> findAllByOrganizationId(Long organizationId);
}