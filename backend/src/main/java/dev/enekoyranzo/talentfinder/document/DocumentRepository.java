package dev.enekoyranzo.talentfinder.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByIdAndOrganizationId(Long id, Long organizationId);

    boolean existsByOrganizationIdAndSha256(Long organizationId, String sha256);

    long countByOrganizationIdAndFolderId(Long organizationId, Long folderId);

    // Listados siempre paginados.
    Page<Document> findByOrganizationId(Long organizationId, Pageable pageable);

    Page<Document> findByOrganizationIdAndFolderId(Long organizationId, Long folderId, Pageable pageable);

    /** Documentos sin carpeta ("General"). */
    Page<Document> findByOrganizationIdAndFolderIdIsNull(Long organizationId, Pageable pageable);

    /** Número de documentos por carpeta. folderId null = documentos sin carpeta ("General"). */
    interface FolderCount {
        Long getFolderId();

        long getTotal();
    }

    @Query("select d.folderId as folderId, count(d) as total "
            + "from Document d where d.organizationId = :orgId group by d.folderId")
    List<FolderCount> countByFolder(@Param("orgId") Long organizationId);
}