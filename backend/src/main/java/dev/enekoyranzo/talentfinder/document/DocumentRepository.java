package dev.enekoyranzo.talentfinder.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    // TODA consulta de negocio lleva organizationId. No uses findById a secas con datos de una petición.
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

    // ------------------------------------------------------------------------------------------
    // Estado de indexado. Transiciones atómicas: cada UPDATE solo actúa si el documento está en el
    // estado de origen esperado, y devuelve cuántas filas cambió (1 = lo consiguió, 0 = no).
    // Son consultas nativas porque las columnas no son escribibles desde JPA (ver Document).
    // ------------------------------------------------------------------------------------------

    /** Identificadores de los documentos en un estado, de más antiguo a más reciente. Sin filtro de organización: lo usa el proceso interno. */
    @Query("select d.id from Document d where d.indexStatus = :status order by d.id")
    List<Long> findIdsByIndexStatus(@Param("status") IndexStatus status, Pageable pageable);

    /** PENDING -> INDEXING. Solo uno de varios hilos simultáneos lo consigue. */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update documents
               set index_status = 'INDEXING', index_started_at = :now,
                   index_attempts = index_attempts + 1, index_error = null
             where id = :id and index_status = 'PENDING'
            """, nativeQuery = true)
    int claimForIndexing(@Param("id") Long id, @Param("now") Instant now);

    /** INDEXING -> INDEXED. */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update documents
               set index_status = 'INDEXED', indexed_at = :now, index_started_at = null, index_error = null
             where id = :id and index_status = 'INDEXING'
            """, nativeQuery = true)
    int markIndexed(@Param("id") Long id, @Param("now") Instant now);

    /** INDEXING -> FAILED, con un código de motivo (nunca texto del documento). */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update documents
               set index_status = 'FAILED', index_error = :error, index_started_at = null
             where id = :id and index_status = 'INDEXING'
            """, nativeQuery = true)
    int markFailed(@Param("id") Long id, @Param("error") String error);

    /** Recupera los INDEXING que empezaron antes de "cutoff" (la aplicación se reinició a medias). */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update documents
               set index_status = 'PENDING', index_started_at = null
             where index_status = 'INDEXING' and index_started_at < :cutoff
            """, nativeQuery = true)
    int requeueStuck(@Param("cutoff") Instant cutoff);

    /** FAILED -> PENDING por orden de un usuario (reintento). Acotado a su organización. */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update documents
               set index_status = 'PENDING', index_attempts = 0, index_error = null
             where id = :id and organization_id = :orgId and index_status = 'FAILED'
            """, nativeQuery = true)
    int requeueFailed(@Param("id") Long id, @Param("orgId") Long organizationId);
}