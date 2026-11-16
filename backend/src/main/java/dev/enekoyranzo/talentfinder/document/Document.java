package dev.enekoyranzo.talentfinder.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    public void setFolderId(Long folderId) {
        this.folderId = folderId;
    }
}