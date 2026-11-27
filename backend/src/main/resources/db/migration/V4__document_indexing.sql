-- Búsqueda semántica: extensión de vectores, estado de indexado y fragmentos de texto.
CREATE EXTENSION IF NOT EXISTS vector;

-- ------------------------------------------------------------ estado de indexado
ALTER TABLE documents
    ADD COLUMN index_status      VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (index_status IN ('PENDING', 'INDEXING', 'INDEXED', 'FAILED')),
    -- Código del motivo del fallo (NO_TEXT, EXTRACTION_FAILED...). Nunca texto del documento.
    ADD COLUMN index_error       VARCHAR(40),
    ADD COLUMN indexed_at        TIMESTAMPTZ,
    -- Cuándo empezó el intento en curso: sirve para recuperar los que se quedaron a medias.
    ADD COLUMN index_started_at  TIMESTAMPTZ,
    ADD COLUMN index_attempts    INT NOT NULL DEFAULT 0;

-- Los documentos que ya existían quedan PENDING y los recogerá el indexador.
CREATE INDEX idx_documents_index_status ON documents (index_status)
    WHERE index_status IN ('PENDING', 'INDEXING');

-- Necesaria para la clave foránea compuesta de los fragmentos.
ALTER TABLE documents ADD CONSTRAINT uq_documents_id_org UNIQUE (id, organization_id);

-- ------------------------------------------------------------------- fragmentos
-- El texto y los vectores de un CV son datos personales: tienen el mismo ciclo de vida
-- que el documento (se borran en cascada con él, o con su organización).
CREATE TABLE document_chunks (
                                 id               BIGSERIAL PRIMARY KEY,
                                 organization_id  BIGINT NOT NULL,
                                 document_id      BIGINT NOT NULL,
                                 chunk_index      INT NOT NULL CHECK (chunk_index >= 0),
                                 page_number      INT NOT NULL CHECK (page_number >= 1),
                                 content          TEXT NOT NULL CHECK (length(btrim(content)) > 0),
                                 embedding        vector(384) NOT NULL,
    -- Modelo y revisión con los que se calculó el vector, para poder reindexar al cambiar de modelo.
                                 embedding_model  VARCHAR(100) NOT NULL,
                                 created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),

                                 UNIQUE (document_id, chunk_index),

    -- Garantiza que el fragmento pertenece a la misma organización que su documento.
                                 CONSTRAINT fk_chunks_document FOREIGN KEY (document_id, organization_id)
                                     REFERENCES documents (id, organization_id) ON DELETE CASCADE
);

CREATE INDEX idx_chunks_org ON document_chunks (organization_id);