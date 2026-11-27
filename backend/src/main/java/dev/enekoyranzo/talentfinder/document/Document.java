package dev.enekoyranzo.talentfinder.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Inmutable: un documento nunca cambia de organización.
    @Column(name = "organization_id", nullable = false, updatable = false)
    private Long organizationId;

    // null = "General"
    @Column(name = "folder_id")
    private Long folderId;

    @Column(name = "storage_key", nullable = false, updatable = false, length = 200)
    private String storageKey;

    @Column(name = "original_filename", nullable = false, updatable = false, length = 255)
    private String originalFilename;

    @Column(name = "size_bytes", nullable = false, updatable = false)
    private long sizeBytes;

    @Column(nullable = false, updatable = false, length = 64)
    private String sha256;

    @Column(name = "uploaded_by", updatable = false)
    private Long uploadedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // --- Estado de indexado ---
    // JPA NUNCA escribe estas columnas (insertable=false, updatable=false): solo cambian con las
    // consultas atómicas de DocumentRepository. Así, guardar el documento (por ejemplo, al moverlo
    // de carpeta) no puede pisar con un valor antiguo el estado que acaba de poner el indexador.
    // El valor inicial coincide con el DEFAULT de la base de datos.
    @Enumerated(EnumType.STRING)
    @Column(name = "index_status", nullable = false, insertable = false, updatable = false, length = 20)
    private IndexStatus indexStatus = IndexStatus.PENDING;

    @Column(name = "index_error", insertable = false, updatable = false, length = 40)
    private String indexError;

    @Column(name = "indexed_at", insertable = false, updatable = false)
    private Instant indexedAt;

    @Column(name = "index_started_at", insertable = false, updatable = false)
    private Instant indexStartedAt;

    @Column(name = "index_attempts", nullable = false, insertable = false, updatable = false)
    private int indexAttempts;

    protected Document() {
        // requerido por JPA
    }

    public Document(Long organizationId, Long folderId, String storageKey, String originalFilename,
                    long sizeBytes, String sha256, Long uploadedBy) {
        this.organizationId = organizationId;
        this.folderId = folderId;
        this.storageKey = storageKey;
        this.originalFilename = originalFilename;
        this.sizeBytes = sizeBytes;
        this.sha256 = sha256;
        this.uploadedBy = uploadedBy;
    }

    public Long getId() {
        return id;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public Long getFolderId() {
        return folderId;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getSha256() {
        return sha256;
    }

    public Long getUploadedBy() {
        return uploadedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public IndexStatus getIndexStatus() {
        return indexStatus;
    }

    public String getIndexError() {
        return indexError;
    }

    public Instant getIndexedAt() {
        return indexedAt;
    }

    public Instant getIndexStartedAt() {
        return indexStartedAt;
    }

    public int getIndexAttempts() {
        return indexAttempts;
    }

    public void setFolderId(Long folderId) {
        this.folderId = folderId;
    }
}