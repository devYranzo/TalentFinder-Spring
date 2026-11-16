CREATE TABLE folders
(
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT       NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Nombre de carpeta único por organización, sin distinguir mayúsculas
CREATE UNIQUE INDEX uq_folders_org_name_lower ON folders (organization_id, lower(name));

CREATE TABLE documents
(
    id                BIGSERIAL PRIMARY KEY,
    organization_id   BIGINT       NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    folder_id         BIGINT       REFERENCES folders (id) ON DELETE SET NULL,
    storage_key       VARCHAR(200) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    size_bytes        BIGINT       NOT NULL CHECK (size_bytes > 0 AND size_bytes <= 10485760),
    sha256            VARCHAR(64)  NOT NULL,
    uploaded_by       BIGINT       REFERENCES users (id) ON DELETE SET NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_documents_storage_key ON documents (storage_key);

CREATE UNIQUE INDEX uq_documents_org_sha256 ON documents (organization_id, sha256);
CREATE INDEX idx_documents_org_folder ON documents (organization_id, folder_id);