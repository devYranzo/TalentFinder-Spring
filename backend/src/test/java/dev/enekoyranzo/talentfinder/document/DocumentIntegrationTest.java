package dev.enekoyranzo.talentfinder.document;

import dev.enekoyranzo.talentfinder.support.AbstractIntegrationTest;
import dev.enekoyranzo.talentfinder.support.ApiClient;
import dev.enekoyranzo.talentfinder.support.ApiClient.Response;
import dev.enekoyranzo.talentfinder.support.TestApp;
import dev.enekoyranzo.talentfinder.support.TestPdfs;
import dev.enekoyranzo.talentfinder.user.Role;
import org.junit.jupiter.api.Test;

import java.io.UncheckedIOException;
import java.util.List;

import static dev.enekoyranzo.talentfinder.support.ApiClient.obj;
import static org.assertj.core.api.Assertions.assertThat;

class DocumentIntegrationTest extends AbstractIntegrationTest {

    private int docCount(long orgId) {
        return jdbc.queryForObject("select count(*) from documents where organization_id = ?", Integer.class, orgId);
    }

    private long createFolder(ApiClient c, String name) {
        return c.postJson("/folders", obj("name", name)).id();
    }

    // ------------------------------------------------------------------ subida
    @Test
    void laSubidaCorrectaDejaFilaYObjetoCoherentes() {
        TestApp.Tenant t = app().newTenant();
        byte[] pdf = TestPdfs.unique();

        Response r = t.admin().upload("/documents", "cv-ana.pdf", pdf, null);

        assertThat(r.status()).isEqualTo(201);
        assertThat(r.field("filename")).isEqualTo("cv-ana.pdf");
        assertThat(r.field("sizeBytes")).isEqualTo(String.valueOf(pdf.length));
        assertThat(r.body()).doesNotContain("storageKey").doesNotContain("sha256").doesNotContain("org_");

        String key = jdbc.queryForObject("select storage_key from documents where id = ?", String.class, r.id());
        assertThat(key).matches("org_" + t.organizationId() + "/[0-9a-f-]{36}");
        assertThat(storage.keys()).containsExactly(key);
        assertThat(storage.get(key).orElseThrow()).isEqualTo(pdf);
    }

    @Test
    void unPdfDuplicadoEnLaMismaOrganizacionDa409YNoSubeNada() {
        TestApp.Tenant t = app().newTenant();
        byte[] pdf = TestPdfs.unique();
        t.admin().upload("/documents", "uno.pdf", pdf, null);

        Response again = t.admin().upload("/documents", "otro-nombre.pdf", pdf, null);

        assertThat(again.status()).isEqualTo(409);
        assertThat(storage.size()).isEqualTo(1);
        assertThat(docCount(t.organizationId())).isEqualTo(1);
    }

    @Test
    void elMismoPdfPuedeSubirseEnOrganizacionesDistintas() {
        TestApp.Tenant a = app().newTenant();
        TestApp.Tenant b = app().newTenant();
        byte[] pdf = TestPdfs.unique();

        assertThat(a.admin().upload("/documents", "cv.pdf", pdf, null).status()).isEqualTo(201);
        assertThat(b.admin().upload("/documents", "cv.pdf", pdf, null).status()).isEqualTo(201);
    }

    @Test
    void rechazaLoQueNoEsUnPdfValidoSinGuardarNada() {
        TestApp.Tenant t = app().newTenant();
        ApiClient admin = t.admin();

        assertThat(admin.upload("/documents", "falso.pdf", "hola".getBytes(), null).status()).isEqualTo(400);
        assertThat(admin.upload("/documents", "cv.txt", TestPdfs.unique(), null).status()).isEqualTo(400);
        assertThat(admin.upload("/documents", "sin-extension", TestPdfs.unique(), null).status()).isEqualTo(400);
        assertThat(admin.upload("/documents", "cifrado.pdf", TestPdfs.encrypted(), null).status()).isEqualTo(400);
        assertThat(admin.upload("/documents", "vacio.pdf", new byte[0], null).status()).isEqualTo(400);

        assertThat(storage.size()).isZero();
        assertThat(docCount(t.organizationId())).isZero();
    }

    @Test
    void elRechazoQuedaAuditadoConSuMotivoSinElNombreDelArchivo() {
        TestApp.Tenant t = app().newTenant();

        t.admin().upload("/documents", "secreto-ana-garcia.pdf", TestPdfs.encrypted(), null);

        String details = jdbc.queryForObject("""
                select details from audit_log
                where organization_id = ? and action = 'DOCUMENT_UPLOAD' and outcome = 'REJECTED'
                order by id desc limit 1
                """, String.class, t.organizationId());
        assertThat(details).isEqualTo("reason=ENCRYPTED");
        assertThat(jdbc.queryForObject("select count(*) from audit_log where details like '%garcia%'",
                Integer.class)).isZero();
    }

    @Test
    void elNombreDeArchivoSeSaneaYNuncaLlevaRutas() {
        ApiClient admin = app().newTenant().admin();

        Response r = admin.upload("/documents", "../../etc/evil.pdf", TestPdfs.unique(), null);

        assertThat(r.status()).isEqualTo(201);
        assertThat(r.field("filename")).isEqualTo("evil.pdf");
    }

    @Test
    void rechazaArchivosDeMasDe10MbSinGuardarNada() {
        TestApp.Tenant t = app().newTenant();
        int status;
        try {
            status = t.admin().upload("/documents", "enorme.pdf", TestPdfs.oversized(11 * 1024 * 1024), null).status();
        } catch (UncheckedIOException e) {
            status = -1;
        }

        // Un 500 aquí sería un fallo real: el límite debe responder con un error de cliente.
        assertThat(status).isIn(-1, 400, 413);
        assertThat(storage.size()).isZero();
        assertThat(docCount(t.organizationId())).isZero();
    }

    @Test
    void conElAlmacenCaidoLaSubidaDa503YNoDejaFila() {
        TestApp.Tenant t = app().newTenant();
        storage.setFailing(true);

        Response r = t.admin().upload("/documents", "cv.pdf", TestPdfs.unique(), null);

        assertThat(r.status()).isEqualTo(503);
        assertThat(docCount(t.organizationId())).isZero();
    }

    @Test
    void subirUnCvADistintasCarpetas() {
        TestApp.Tenant t = app().newTenant();
        long folder = createFolder(t.admin(), "Destino");

        Response r = t.admin().upload("/documents", "cv.pdf", TestPdfs.unique(), folder);

        assertThat(r.status()).isEqualTo(201);
        assertThat(r.field("folderId")).isEqualTo(String.valueOf(folder));
        assertThat(t.admin().upload("/documents", "cv2.pdf", TestPdfs.unique(), 999_999_999L).status()).isEqualTo(404);
    }

    @Test
    void unRecruiterPuedeSubirYAnonimoNo() {
        TestApp.Tenant t = app().newTenant();
        ApiClient recruiter = app().addMember(t, Role.RECRUITER).client();

        assertThat(recruiter.upload("/documents", "cv.pdf", TestPdfs.unique(), null).status()).isEqualTo(201);
        assertThat(new ApiClient(baseUrl()).upload("/documents", "cv.pdf", TestPdfs.unique(), null).status())
                .isEqualTo(401);
    }

    // ----------------------------------------------------------------- listado
    @Test
    void elListadoPaginaYOrdenaDelMasRecienteAlMasAntiguo() {
        TestApp.Tenant t = app().newTenant();
        long d1 = app().uploadPdf(t.admin(), null);
        long d2 = app().uploadPdf(t.admin(), null);
        long d3 = app().uploadPdf(t.admin(), null);

        Response page0 = t.admin().get("/documents?size=2&page=0");
        Response page1 = t.admin().get("/documents?size=2&page=1");

        assertThat(page0.field("totalItems")).isEqualTo("3");
        assertThat(page0.field("totalPages")).isEqualTo("2");
        assertThat(page0.ids()).containsExactly(d3, d2);
        assertThat(page1.ids()).containsExactly(d1);
    }

    @Test
    void elListadoFiltraPorCarpetaYPorGeneral() {
        TestApp.Tenant t = app().newTenant();
        long folder = createFolder(t.admin(), "Filtro");
        long inFolder = app().uploadPdf(t.admin(), folder);
        long loose = app().uploadPdf(t.admin(), null);

        assertThat(t.admin().get("/documents?folderId=" + folder).ids()).containsExactly(inFolder);
        assertThat(t.admin().get("/documents?unfiled=true").ids()).containsExactly(loose);
        assertThat(t.admin().get("/documents").ids()).containsExactlyInAnyOrder(inFolder, loose);
    }

    @Test
    void elListadoRechazaParametrosInvalidos() {
        ApiClient admin = app().newTenant().admin();

        for (String query : List.of("size=0", "size=101", "size=500", "page=-1", "folderId=abc",
                "folderId=1&unfiled=true")) {
            assertThat(admin.get("/documents?" + query).status()).as(query).isEqualTo(400);
        }
    }

    // ------------------------------------------------------------------- mover
    @Test
    void moverUnCvEntreCarpetasYDevolverloAGeneral() {
        TestApp.Tenant t = app().newTenant();
        long folder = createFolder(t.admin(), "Destino");
        long doc = app().uploadPdf(t.admin(), null);

        Response moved = t.admin().patchJson("/documents/" + doc + "/folder", obj("folderId", folder));
        assertThat(moved.status()).isEqualTo(200);
        assertThat(moved.field("folderId")).isEqualTo(String.valueOf(folder));
        assertThat(t.admin().get("/documents?folderId=" + folder).ids()).containsExactly(doc);

        Response back = t.admin().patchJson("/documents/" + doc + "/folder", obj("folderId", null));
        assertThat(back.status()).isEqualTo(200);
        assertThat(back.field("folderId")).isNull();
        assertThat(t.admin().get("/documents?unfiled=true").ids()).containsExactly(doc);
    }

    @Test
    void moverAUnaCarpetaInexistenteDa404() {
        TestApp.Tenant t = app().newTenant();
        long doc = app().uploadPdf(t.admin(), null);

        assertThat(t.admin().patchJson("/documents/" + doc + "/folder", obj("folderId", 999_999_999L)).status())
                .isEqualTo(404);
    }

    // ---------------------------------------------------------------- descarga
    @Test
    void laDescargaDevuelveExactamenteElArchivoSubidoComoAdjuntoSinCache() {
        TestApp.Tenant t = app().newTenant();
        byte[] pdf = TestPdfs.unique();
        long doc = t.admin().upload("/documents", "cv-luis.pdf", pdf, null).id();

        Response r = t.admin().get("/documents/" + doc + "/file");

        assertThat(r.status()).isEqualTo(200);
        assertThat(r.bytes()).isEqualTo(pdf);
        assertThat(r.header("Content-Type")).isEqualTo("application/pdf");
        assertThat(r.header("Content-Disposition")).startsWith("attachment").contains("cv-luis.pdf");
        assertThat(r.header("Cache-Control")).contains("no-store");
    }

    @Test
    void cadaDescargaQuedaAuditadaConElIdNuncaElNombre() {
        TestApp.Tenant t = app().newTenant();
        long doc = t.admin().upload("/documents", "cv-maria-lopez.pdf", TestPdfs.unique(), null).id();
        t.admin().get("/documents/" + doc + "/file");

        String details = jdbc.queryForObject("""
                select details from audit_log
                where organization_id = ? and action = 'DOCUMENT_DOWNLOAD' order by id desc limit 1
                """, String.class, t.organizationId());

        assertThat(details).isEqualTo("document=" + doc);
    }

    @Test
    void siFaltaElObjetoEnElAlmacenLaDescargaDa404() {
        TestApp.Tenant t = app().newTenant();
        long doc = app().uploadPdf(t.admin(), null);
        storage.reset(); // simula una incoherencia: la fila existe, el objeto no

        assertThat(t.admin().get("/documents/" + doc + "/file").status()).isEqualTo(404);
    }

    @Test
    void conElAlmacenCaidoLaDescargaDa503() {
        TestApp.Tenant t = app().newTenant();
        long doc = app().uploadPdf(t.admin(), null);
        storage.setFailing(true);

        assertThat(t.admin().get("/documents/" + doc + "/file").status()).isEqualTo(503);
    }

    // ----------------------------------------------------------------- borrado
    @Test
    void borrarEliminaLaFilaYElObjetoYEsIdempotenteEnElResultado() {
        TestApp.Tenant t = app().newTenant();
        long doc = app().uploadPdf(t.admin(), null);
        assertThat(storage.size()).isEqualTo(1);

        assertThat(t.admin().delete("/documents/" + doc).status()).isEqualTo(204);

        assertThat(storage.size()).isZero();
        assertThat(docCount(t.organizationId())).isZero();
        assertThat(t.admin().get("/documents/" + doc + "/file").status()).isEqualTo(404);
        assertThat(t.admin().delete("/documents/" + doc).status()).isEqualTo(404);
    }

    @Test
    void conElAlmacenCaidoElBorradoDa503YConservaLaFilaParaPoderReintentar() {
        TestApp.Tenant t = app().newTenant();
        long doc = app().uploadPdf(t.admin(), null);

        storage.setFailing(true);
        assertThat(t.admin().delete("/documents/" + doc).status()).isEqualTo(503);
        assertThat(docCount(t.organizationId())).isEqualTo(1); // la fila NO se pierde

        storage.setFailing(false);
        assertThat(t.admin().delete("/documents/" + doc).status()).isEqualTo(204);
        assertThat(storage.size()).isZero();
        assertThat(docCount(t.organizationId())).isZero();
    }

    @Test
    void elBorradoSePuedeReintentarSiElObjetoYaNoEstabaEnElAlmacen() {
        // Caso de un borrado interrumpido tras eliminar el objeto: la fila debe poder borrarse igualmente.
        TestApp.Tenant t = app().newTenant();
        long doc = app().uploadPdf(t.admin(), null);
        storage.reset();

        assertThat(t.admin().delete("/documents/" + doc).status()).isEqualTo(204);
        assertThat(docCount(t.organizationId())).isZero();
    }

    @Test
    void elBorradoQuedaAuditadoSinElNombreDelArchivo() {
        TestApp.Tenant t = app().newTenant();
        long doc = t.admin().upload("/documents", "cv-pedro-ruiz.pdf", TestPdfs.unique(), null).id();
        t.admin().delete("/documents/" + doc);

        String details = jdbc.queryForObject("""
                select details from audit_log
                where organization_id = ? and action = 'DOCUMENT_DELETE' order by id desc limit 1
                """, String.class, t.organizationId());

        assertThat(details).isEqualTo("document=" + doc);
    }

    // ------------------------------------------------------------- aislamiento
    @Test
    void unaOrganizacionNoPuedeVerDescargarMoverNiBorrarLosCvsDeOtra() {
        TestApp.Tenant a = app().newTenant();
        TestApp.Tenant b = app().newTenant();
        long folderOfA = createFolder(a.admin(), "De A");
        long docOfA = app().uploadPdf(a.admin(), folderOfA);

        assertThat(b.admin().get("/documents").field("totalItems")).isEqualTo("0");
        assertThat(b.admin().get("/documents/" + docOfA + "/file").status()).isEqualTo(404);
        assertThat(b.admin().patchJson("/documents/" + docOfA + "/folder", obj("folderId", null)).status())
                .isEqualTo(404);
        assertThat(b.admin().delete("/documents/" + docOfA).status()).isEqualTo(404);
        assertThat(b.admin().get("/documents?folderId=" + folderOfA).status()).isEqualTo(404);
        assertThat(b.admin().upload("/documents", "cv.pdf", TestPdfs.unique(), folderOfA).status()).isEqualTo(404);

        long docOfB = app().uploadPdf(b.admin(), null);
        assertThat(a.admin().patchJson("/documents/" + docOfB + "/folder", obj("folderId", folderOfA)).status())
                .isEqualTo(404);

        // Y lo de A sigue intacto tras todos esos intentos.
        assertThat(a.admin().get("/documents/" + docOfA + "/file").status()).isEqualTo(200);
        assertThat(storage.size()).isEqualTo(2);
    }

    @Test
    void lasClavesDelAlmacenSiempreLlevanElPrefijoDeSuOrganizacion() {
        TestApp.Tenant a = app().newTenant();
        TestApp.Tenant b = app().newTenant();
        app().uploadPdf(a.admin(), null);
        app().uploadPdf(b.admin(), null);

        assertThat(storage.keys()).hasSize(2)
                .anyMatch(k -> k.startsWith("org_" + a.organizationId() + "/"))
                .anyMatch(k -> k.startsWith("org_" + b.organizationId() + "/"));
    }
}