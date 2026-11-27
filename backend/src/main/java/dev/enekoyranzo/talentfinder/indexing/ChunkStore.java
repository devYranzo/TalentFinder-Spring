package dev.enekoyranzo.talentfinder.indexing;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Acceso a los fragmentos de texto y sus vectores. Usa JDBC porque mapear el tipo "vector"
 * en JPA exige una dependencia adicional y aquí solo escribimos en bloque y consultamos con SQL propio.
 * Todas llevan organizationId: la propia base de datos impide que un fragmento pertenezca a una
 * organización distinta de la de su documento.
 */
@Repository
public class ChunkStore {

    private static final int BATCH_SIZE = 100;

    private final JdbcTemplate jdbc;

    public ChunkStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Sustituye todos los fragmentos de un documento por los nuevos, en una sola transacción. */
    @Transactional
    public void replace(long organizationId, long documentId, String modelId, List<NewChunk> chunks) {
        jdbc.update("delete from document_chunks where document_id = ? and organization_id = ?",
                documentId, organizationId);

        jdbc.batchUpdate("""
                        insert into document_chunks
                            (organization_id, document_id, chunk_index, page_number, content, embedding, embedding_model)
                        values (?, ?, ?, ?, ?, cast(? as vector), ?)
                        """,
                chunks, BATCH_SIZE, (ps, c) -> {
                    ps.setLong(1, organizationId);
                    ps.setLong(2, documentId);
                    ps.setInt(3, c.index());
                    ps.setInt(4, c.page());
                    ps.setString(5, c.content());
                    ps.setString(6, toVectorLiteral(c.embedding()));
                    ps.setString(7, modelId);
                });
    }

    @Transactional(readOnly = true)
    public int count(long organizationId, long documentId) {
        Integer n = jdbc.queryForObject(
                "select count(*) from document_chunks where document_id = ? and organization_id = ?",
                Integer.class, documentId, organizationId);
        return n == null ? 0 : n;
    }

    /** Representación de texto de un vector para pgvector: [0.1,0.2,...]. */
    public static String toVectorLiteral(float[] v) {
        StringBuilder sb = new StringBuilder(v.length * 10).append('[');
        for (int i = 0; i < v.length; i++) {
            if (Float.isNaN(v[i]) || Float.isInfinite(v[i])) {
                throw new IllegalArgumentException("El vector contiene valores no válidos.");
            }
            if (i > 0) {
                sb.append(',');
            }
            sb.append(v[i]);
        }
        return sb.append(']').toString();
    }
}