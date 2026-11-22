package dev.enekoyranzo.talentfinder.document;

import dev.enekoyranzo.talentfinder.support.AbstractIntegrationTest;
import dev.enekoyranzo.talentfinder.support.ApiClient;
import dev.enekoyranzo.talentfinder.support.ApiClient.Response;
import dev.enekoyranzo.talentfinder.support.TestApp;
import dev.enekoyranzo.talentfinder.user.Role;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.enekoyranzo.talentfinder.support.ApiClient.obj;
import static org.assertj.core.api.Assertions.assertThat;

class FolderIntegrationTest extends AbstractIntegrationTest {

    private long createFolder(ApiClient c, String name) {
        Response r = c.postJson("/folders", obj("name", name));
        assertThat(r.status()).as("crear carpeta " + name).isEqualTo(201);
        return r.id();
    }

    // ------------------------------------------------------------------ crear
    @Test
    void crearUnaCarpetaLaDevuelveVacia() {
        ApiClient admin = app().newTenant().admin();

        Response r = admin.postJson("/folders", obj("name", "Backend Python"));

        assertThat(r.status()).isEqualTo(201);
        assertThat(r.field("name")).isEqualTo("Backend Python");
        assertThat(r.field("documentCount")).isEqualTo("0");
    }

    @Test
    void noSePermitenNombresDuplicadosSinDistinguirMayusculas() {
        ApiClient admin = app().newTenant().admin();
        createFolder(admin, "Backend");

        assertThat(admin.postJson("/folders", obj("name", "BACKEND")).status()).isEqualTo(409);
        assertThat(admin.postJson("/folders", obj("name", "  backend  ")).status()).isEqualTo(409);
    }

    @Test
    void generalEsUnNombreReservado() {
        ApiClient admin = app().newTenant().admin();

        assertThat(admin.postJson("/folders", obj("name", "General")).status()).isEqualTo(400);
        assertThat(admin.postJson("/folders", obj("name", "gEnErAl")).status()).isEqualTo(400);
    }

    @Test
    void rechazaNombresVaciosOExcesivamenteLargos() {
        ApiClient admin = app().newTenant().admin();

        assertThat(admin.postJson("/folders", obj("name", "")).status()).isEqualTo(400);
        assertThat(admin.postJson("/folders", obj("name", "    ")).status()).isEqualTo(400);
        assertThat(admin.postJson("/folders", obj("name", "x".repeat(101))).status()).isEqualTo(400);
        // Solo caracteres invisibles de formato: tras sanearlo queda vacío.
        assertThat(admin.postJson("/folders", obj("name", "\u200B\u200B")).status()).isEqualTo(400);
    }

    @Test
    void elNombreSeSaneaDeCaracteresInvisiblesYDeControl() {
        ApiClient admin = app().newTenant().admin();

        Response r = admin.postJson("/folders", obj("name", "Dev\u200Bops\u0007"));

        assertThat(r.status()).isEqualTo(201);
        assertThat(r.field("name")).isEqualTo("Devops");
    }

    // ----------------------------------------------------------------- listado
    @Test
    void elListadoIncluyeElRecuentoPorCarpetaYLosDocumentosSinCarpeta() {
        TestApp.Tenant t = app().newTenant();
        long folder = createFolder(t.admin(), "Con CVs");
        app().uploadPdf(t.admin(), folder);
        app().uploadPdf(t.admin(), folder);
        app().uploadPdf(t.admin(), null);

        Response r = t.admin().get("/folders");

        assertThat(r.status()).isEqualTo(200);
        assertThat(r.field("unfiledCount")).isEqualTo("1");
        assertThat(r.field("documentCount")).isEqualTo("2");
    }

    @Test
    void elListadoVieneOrdenadoPorNombre() {
        ApiClient admin = app().newTenant().admin();
        long c = createFolder(admin, "Carpeta C");
        long a = createFolder(admin, "Carpeta A");
        long b = createFolder(admin, "Carpeta B");

        assertThat(admin.get("/folders").ids()).containsExactly(a, b, c);
    }

    // ---------------------------------------------------------------- renombrar
    @Test
    void renombrarFuncionaYPuedeCambiarSoloMayusculasDelPropioNombre() {
        ApiClient admin = app().newTenant().admin();
        long id = createFolder(admin, "Backend");

        assertThat(admin.patchJson("/folders/" + id, obj("name", "BACKEND")).status()).isEqualTo(200);
        Response r = admin.patchJson("/folders/" + id, obj("name", "Frontend"));
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.field("name")).isEqualTo("Frontend");
    }

    @Test
    void noSePuedeRenombrarAUnNombreQueYaExiste() {
        ApiClient admin = app().newTenant().admin();
        createFolder(admin, "Uno");
        long dos = createFolder(admin, "Dos");

        assertThat(admin.patchJson("/folders/" + dos, obj("name", "uno")).status()).isEqualTo(409);
        assertThat(admin.patchJson("/folders/" + dos, obj("name", "General")).status()).isEqualTo(400);
    }

    // ------------------------------------------------------------------ borrar
    @Test
    void borrarUnaCarpetaNoBorraSusCvsLosPasaAGeneral() {
        TestApp.Tenant t = app().newTenant();
        long folder = createFolder(t.admin(), "Temporal");
        long doc = app().uploadPdf(t.admin(), folder);

        assertThat(t.admin().delete("/folders/" + folder).status()).isEqualTo(204);

        Response unfiled = t.admin().get("/documents?unfiled=true");
        assertThat(unfiled.ids()).contains(doc);
        assertThat(t.admin().get("/documents/" + doc + "/file").status()).isEqualTo(200);
        assertThat(t.admin().get("/documents?folderId=" + folder).status()).isEqualTo(404);
        assertThat(t.admin().delete("/folders/" + folder).status()).isEqualTo(404);
    }

    // ------------------------------------------------------------- aislamiento
    @Test
    void unaOrganizacionNoVeNiTocaLasCarpetasDeOtra() {
        TestApp.Tenant a = app().newTenant();
        TestApp.Tenant b = app().newTenant();
        long folder = createFolder(a.admin(), "Privada de A");

        Response listB = b.admin().get("/folders");
        assertThat(listB.field("unfiledCount")).isEqualTo("0");
        assertThat(listB.body()).doesNotContain("Privada de A");
        assertThat(b.admin().patchJson("/folders/" + folder, obj("name", "Hackeada")).status()).isEqualTo(404);
        assertThat(b.admin().delete("/folders/" + folder).status()).isEqualTo(404);
        assertThat(a.admin().get("/folders").body()).contains("Privada de A");
    }

    @Test
    void elMismoNombreDeCarpetaPuedeExistirEnOrganizacionesDistintas() {
        ApiClient a = app().newTenant().admin();
        ApiClient b = app().newTenant().admin();

        createFolder(a, "Backend");

        assertThat(b.postJson("/folders", obj("name", "Backend")).status()).isEqualTo(201);
    }

    // ------------------------------------------------------------------ permisos
    @Test
    void unRecruiterPuedeGestionarCarpetas() {
        TestApp.Tenant t = app().newTenant();
        ApiClient recruiter = app().addMember(t, Role.RECRUITER).client();

        long id = createFolder(recruiter, "Del recruiter");

        assertThat(recruiter.patchJson("/folders/" + id, obj("name", "Renombrada")).status()).isEqualTo(200);
        assertThat(recruiter.get("/folders").status()).isEqualTo(200);
        assertThat(recruiter.delete("/folders/" + id).status()).isEqualTo(204);
    }

    // ------------------------------------------------------------------ auditoría
    @Test
    void laAuditoriaGuardaElIdDeLaCarpetaNuncaSuNombre() {
        TestApp.Tenant t = app().newTenant();
        long id = createFolder(t.admin(), "Candidatos de Ana Garcia");
        t.admin().patchJson("/folders/" + id, obj("name", "Candidatos de Luis Perez"));
        t.admin().delete("/folders/" + id);

        List<String> details = jdbc.queryForList(
                "select details from audit_log where organization_id = ? and action like 'FOLDER_%'",
                String.class, t.organizationId());

        assertThat(details).hasSize(3);
        assertThat(details).allSatisfy(d -> assertThat(d).contains("folder=" + id)
                .doesNotContainIgnoringCase("garcia").doesNotContainIgnoringCase("perez"));
    }
}