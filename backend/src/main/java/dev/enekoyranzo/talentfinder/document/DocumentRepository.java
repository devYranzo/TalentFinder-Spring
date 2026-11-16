package dev.enekoyranzo.talentfinder.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    Optional<Document> findByIdAndOrganizationId(Long id, Long organizationId);

    List<Document> findAllByOrganizationIdOrderByCreatedAtDesc(Long organizationId);

    boolean existsByOrganizationIdAndSha256(Long organizationId, String sha256);
}