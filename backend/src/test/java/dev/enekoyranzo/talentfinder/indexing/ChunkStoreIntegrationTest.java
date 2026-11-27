package dev.enekoyranzo.talentfinder.indexing;

import dev.enekoyranzo.talentfinder.support.AbstractIntegrationTest;
import dev.enekoyranzo.talentfinder.support.TestApp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChunkStoreIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    ChunkStore chunks;

    @Autowired
    TextEmbedder embedder;

    private List<NewChunk> sample(int n) {
        List<String> texts = IntStream.range(0, n)
                .mapToObj(i -> "Experiencia con Java y Spring, fragmento " + i).toList();
        List<float[]> vectors = embedder.embedPassages(texts);
        return IntStream.range(0, n).mapToObj(i -> new NewChunk(i, 1, texts.get(i), vectors.get(i))).toList();
    }

    private int rows(String where, Object... args) {
        return jdbc.queryForObject("select count(*) from document_chunks where " + where, Integer.class, args);
    }

    @Test
    void guardaLosFragmentosYLosCuenta() {
        TestApp.Tenant t = app().newTenant();
        long doc = app().uploadPdf(t.admin(), null);

        chunks.replace(t.organizationId(), doc, embedder.modelId(), sample(3));

        assertThat(chunks.count(t.organizationId(), doc)).isEqualTo(3);
        assertThat(rows("document_id = ? and embedding_model = ?", doc, embedder.modelId())).isEqualTo(3);
    }

    @Test
    void reemplazarSustituyeLosFragmentosAnterioresEnLugarDeAcumularlos() {
        TestApp.Tenant t = app().newTenant();
        long doc = app().uploadPdf(t.admin(), null);
        chunks.replace(t.organizationId(), doc, embedder.modelId(), sample(5));

        chunks.replace(t.organizationId(), doc, embedder.modelId(), sample(2));

        assertThat(chunks.count(t.organizationId(), doc)).isEqualTo(2);
    }

    @Test
    void elVectorSeGuardaConLasDimensionesYElValorCorrectos() {
        TestApp.Tenant t = app().newTenant();
        long doc = app().uploadPdf(t.admin(), null);
        float[] v = embedder.embedQuery("hola mundo prueba");

        chunks.replace(t.organizationId(), doc, embedder.modelId(),
                List.of(new NewChunk(0, 1, "hola mundo prueba", v)));

        Integer dims = jdbc.queryForObject(
                "select vector_dims(embedding) from document_chunks where document_id = ?", Integer.class, doc);
        Double distance = jdbc.queryForObject(
                "select embedding <=> cast(? as vector) from document_chunks where document_id = ?",
                Double.class, ChunkStore.toVectorLiteral(v), doc);
        assertThat(dims).isEqualTo(384);
        assertThat(distance).isLessThan(1e-4); // distancia coseno de un vector consigo mismo
    }

    @Test
    void unVectorConDimensionesIncorrectasSeRechaza() {
        TestApp.Tenant t = app().newTenant();
        long doc = app().uploadPdf(t.admin(), null);

        assertThatThrownBy(() -> chunks.replace(t.organizationId(), doc, "m",
                List.of(new NewChunk(0, 1, "texto", new float[10]))))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void rechazaVectoresConValoresNoValidos() {
        float[] bad = new float[384];
        bad[5] = Float.NaN;

        assertThatThrownBy(() -> ChunkStore.toVectorLiteral(bad)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void noPermiteDosFragmentosConElMismoIndiceEnUnDocumento() {
        TestApp.Tenant t = app().newTenant();
        long doc = app().uploadPdf(t.admin(), null);
        float[] v = embedder.embedQuery("texto");

        assertThatThrownBy(() -> chunks.replace(t.organizationId(), doc, "m",
                List.of(new NewChunk(0, 1, "uno", v), new NewChunk(0, 1, "dos", v))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void unFragmentoNoPuedeApuntarAUnDocumentoDeOtraOrganizacion() {
        TestApp.Tenant a = app().newTenant();
        TestApp.Tenant b = app().newTenant();
        long docOfA = app().uploadPdf(a.admin(), null);

        // Intento de guardar un fragmento bajo la organización B para un documento de A.
        assertThatThrownBy(() -> chunks.replace(b.organizationId(), docOfA, "m", sample(1)))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(rows("document_id = ?", docOfA)).isZero();
    }

    @Test
    void borrarElDocumentoPorLaApiBorraSusFragmentos() {
        // Derecho de supresión (RGPD): texto y vectores desaparecen con el documento.
        TestApp.Tenant t = app().newTenant();
        long doc = app().uploadPdf(t.admin(), null);
        chunks.replace(t.organizationId(), doc, embedder.modelId(), sample(4));
        assertThat(rows("document_id = ?", doc)).isEqualTo(4);

        assertThat(t.admin().delete("/documents/" + doc).status()).isEqualTo(204);

        assertThat(rows("document_id = ?", doc)).isZero();
    }

    @Test
    void borrarLaOrganizacionBorraTodosSusFragmentos() {
        TestApp.Tenant t = app().newTenant();
        long doc = app().uploadPdf(t.admin(), null);
        chunks.replace(t.organizationId(), doc, embedder.modelId(), sample(2));

        jdbc.update("delete from organizations where id = ?", t.organizationId());

        assertThat(rows("organization_id = ?", t.organizationId())).isZero();
    }

    @Test
    void losFragmentosDeUnaOrganizacionNoSeMezclanConLosDeOtra() {
        TestApp.Tenant a = app().newTenant();
        TestApp.Tenant b = app().newTenant();
        long docA = app().uploadPdf(a.admin(), null);
        long docB = app().uploadPdf(b.admin(), null);

        chunks.replace(a.organizationId(), docA, embedder.modelId(), sample(3));
        chunks.replace(b.organizationId(), docB, embedder.modelId(), sample(1));

        assertThat(rows("organization_id = ?", a.organizationId())).isEqualTo(3);
        assertThat(rows("organization_id = ?", b.organizationId())).isEqualTo(1);
        // Consultar con la organización equivocada no devuelve nada.
        assertThat(chunks.count(b.organizationId(), docA)).isZero();
    }
}