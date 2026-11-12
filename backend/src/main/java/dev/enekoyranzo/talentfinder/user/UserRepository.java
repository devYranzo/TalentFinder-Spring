package dev.enekoyranzo.talentfinder.user;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Solo para login: el email es único a nivel global.
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    // A partir de aquí, TODA consulta de negocio lleva organizationId.
    // Nunca uses findById a secas para datos que vienen de una petición.
    Optional<User> findByIdAndOrganizationId(Long id, Long organizationId);

    List<User> findAllByOrganizationIdOrderByLastNameAscFirstNameAsc(Long organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.organizationId = :orgId and u.role = :role and u.active = true")
    List<User> lockActiveByRole(@Param("orgId") Long organizationId, @Param("role") Role role);
}