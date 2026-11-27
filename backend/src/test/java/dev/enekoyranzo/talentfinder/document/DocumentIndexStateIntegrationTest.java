package dev.enekoyranzo.talentfinder.document;

import dev.enekoyranzo.talentfinder.support.AbstractIntegrationTest;
import dev.enekoyranzo.talentfinder.support.TestApp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static dev.enekoyranzo.talentfinder.support.ApiClient.obj;
import static org.assertj.core.api.Assertions.assertThat;

class DocumentIndexStateIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    DocumentRepository documents;

    private String status(long doc) {
        return jdbc.queryForObject("select index_status from documents where id = ?", String.class, doc);
    }

    private int attempts(long doc) {
        return jdbc.queryForObject("select index_attempts from documents where id = ?", Integer.class, doc);
    }

    @Test
    void unDocumentoNuevoNaceComoPendiente() {
        TestApp.Tenant t = app().newTenant();

        long doc = app().uploadPdf(t.admin(), null);

        assertThat(status(doc)).isEqualTo("PENDING");
        assertThat(t.admin().get("/documents").field("indexStatus")).isEqualTo("PENDING");
    }

    @Test
    void laSubidaDevuelveElEstadoDeIndexado() {
        TestApp.Tenant t = app().newTenant();

        var r = t.admin().upload("/documents", "cv.pdf",
                dev.enekoyranzo.talentfinder.support.TestPdfs.unique(), null);

        assertThat(r.status()).isEqualTo(201);
        assertThat(r.field("indexStatus")).isEqualTo("PENDING");
        assertThat(r.field("indexError")).isNull();
    }

    @Test
    void soloUnoPuedeTomarUnDocumentoParaIndexarlo() {
        TestApp.Tenant t = app().newTenant();
        long doc = app().uploadPdf(t.admin(), null);

        assertThat(documents.claimForIndexing(doc, Instant.now())).isEqualTo(1);
        assertThat(documents.claimForIndexing(doc, Instant.now())).isZero();

        assertThat(status(doc)).isEqualTo("INDEXING");
        assertThat(attempts(doc)).isEqualTo(1);
    }

    @Test
    void conHilosSimultaneosSoloUnoGanaLaReserva() throws Exception {
        TestApp.Tenant t = app().newTenant();
        long doc = app().uploadPdf(t.admin(), null);
        int threads = 8;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Integer>> results = new java.util.ArrayList<>();
            for (int i = 0; i < threads; i++) {
                results.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    return documents.claimForIndexing(doc, Instant.now());
                }));
            }
            ready.await();
            go.countDown();
            int winners = 0;
            for (Future<Integer> f : results) {
                winners += f.get(30, TimeUnit.SECONDS);
            }
            assertThat(winners).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
        assertThat(attempts(doc)).isEqualTo(1);
    }

    @Test
    void marcarComoIndexadoSoloFuncionaDesdeIndexing() {
        TestApp.Tenant t = app().newTenant();
        long doc = app().uploadPdf(t.admin(), null);

        assertThat(documents.markIndexed(doc, Instant.now())).isZero(); // sigue PENDING
        documents.claimForIndexing(doc, Instant.now());
        assertThat(documents.markIndexed(doc, Instant.now())).isEqualTo(1);

        assertThat(status(doc)).isEqualTo("INDEXED");
        assertThat(jdbc.queryForObject("select indexed_at is not null from documents where id = ?",
                Boolean.class, doc)).isTrue();
        assertThat(t.admin().get("/documents").field("indexStatus")).isEqualTo("INDEXED");
    }

    @Test
    void marcarComoFallidoGuardaElCodigoDelMotivo() {
        TestApp.Tenant t = app().newTenant();
        long doc = app().uploadPdf(t.admin(), null);
        documents.claimForIndexing(doc, Instant.now());

        assertThat(documents.markFailed(doc, "NO_TEXT")).isEqualTo(1);

        assertThat(status(doc)).isEqualTo("FAILED");
        var r = t.admin().get("/documents");
        assertThat(r.field("indexStatus")).isEqualTo("FAILED");
        assertThat(r.field("indexError")).isEqualTo("NO_TEXT");
    }

    @Test
    void losDocumentosAtascadosEnIndexingVuelvenAPendiente() {
        TestApp.Tenant t = app().newTenant();
        long stuck = app().uploadPdf(t.admin(), null);
        long fresh = app().uploadPdf(t.admin(), null);
        documents.claimForIndexing(stuck, Instant.now().minus(2, ChronoUnit.HOURS));
        documents.claimForIndexing(fresh, Instant.now());

        int requeued = documents.requeueStuck(Instant.now().minus(30, ChronoUnit.MINUTES));

        assertThat(requeued).isGreaterThanOrEqualTo(1);
        assertThat(status(stuck)).isEqualTo("PENDING");
        assertThat(status(fresh)).isEqualTo("INDEXING"); // el reciente no se toca
    }

    @Test
    void reintentarUnFallidoLoDevuelveAPendienteYReiniciaLosIntentos() {
        TestApp.Tenant t = app().newTenant();
        long doc = app().uploadPdf(t.admin(), null);
        documents.claimForIndexing(doc, Instant.now());
        documents.markFailed(doc, "EXTRACTION_FAILED");

        assertThat(documents.requeueFailed(doc, t.organizationId())).isEqualTo(1);

        assertThat(status(doc)).isEqualTo("PENDING");
        assertThat(attempts(doc)).isZero();
        assertThat(jdbc.queryForObject("select index_error from documents where id = ?", String.class, doc)).isNull();
    }

    @Test
    void unaOrganizacionNoPuedeReintentarLosDocumentosDeOtra() {
        TestApp.Tenant a = app().newTenant();
        TestApp.Tenant b = app().newTenant();
        long doc = app().uploadPdf(a.admin(), null);
        documents.claimForIndexing(doc, Instant.now());
        documents.markFailed(doc, "NO_TEXT");

        assertThat(documents.requeueFailed(doc, b.organizationId())).isZero();

        assertThat(status(doc)).isEqualTo("FAILED");
    }

    @Test
    void reintentarSoloAfectaALosFallidos() {
        TestApp.Tenant t = app().newTenant();
        long pending = app().uploadPdf(t.admin(), null);

        assertThat(documents.requeueFailed(pending, t.organizationId())).isZero();
    }

    @Test
    void laListaDePendientesDevuelveLosMasAntiguosPrimero() {
        TestApp.Tenant t = app().newTenant();
        long first = app().uploadPdf(t.admin(), null);
        long second = app().uploadPdf(t.admin(), null);

        List<Long> ids = documents.findIdsByIndexStatus(IndexStatus.PENDING, PageRequest.of(0, 1000));

        assertThat(ids).contains(first, second);
        assertThat(ids.indexOf(first)).isLessThan(ids.indexOf(second));
    }

    @Test
    void moverUnDocumentoNoPisaElEstadoDeIndexado() {
        // Regresión del riesgo principal: guardar el documento no debe revertir el estado.
        TestApp.Tenant t = app().newTenant();
        long folder = t.admin().postJson("/folders", obj("name", "Destino")).id();
        long doc = app().uploadPdf(t.admin(), null);
        documents.claimForIndexing(doc, Instant.now());

        var r = t.admin().patchJson("/documents/" + doc + "/folder", obj("folderId", folder));

        assertThat(r.status()).isEqualTo(200);
        assertThat(r.field("indexStatus")).isEqualTo("INDEXING");
        assertThat(status(doc)).isEqualTo("INDEXING");
    }
}