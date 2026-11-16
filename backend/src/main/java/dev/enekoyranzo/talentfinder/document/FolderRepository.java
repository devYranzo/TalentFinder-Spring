package dev.enekoyranzo.talentfinder.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long> {
    Optional<Folder> findByIdAndOrganizationId(Long id, Long organizationId);

    List<Folder> findAllByOrganizationIdOrderByNameAsc(Long organizationId);

    boolean existsByOrganizationIdAndNameIgnoreCase(Long organizationId, String name);
}