package dev.enekoyranzo.talentfinder.user;

import dev.enekoyranzo.talentfinder.support.AbstractIntegrationTest;
import dev.enekoyranzo.talentfinder.support.ApiClient;
import dev.enekoyranzo.talentfinder.support.ApiClient.Csrf;
import dev.enekoyranzo.talentfinder.support.ApiClient.Response;
import dev.enekoyranzo.talentfinder.support.TestApp;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static dev.enekoyranzo.talentfinder.support.ApiClient.obj;
import static org.assertj.core.api.Assertions.assertThat;

class UserManagementIntegrationTest extends AbstractIntegrationTest {

    private long meId(ApiClient c) {
        return Long.parseLong(c.get("/auth/me").field("userId"));
    }

    private String newUserBody(String email, String password, String role) {
        return obj("email", email, "password", password, "firstName", "Eva", "lastName", "Prueba", "role", role);
    }

    private String email() {
        return "u-" + TestApp.unique() + "@talentfinder.test";
    }

    // ------------------------------------------------------------------ altas
    @Test
    void elAdminCreaUnRecruiterYEsteInicaSesion() {
        TestApp.Tenant t = app().newTenant();
        String email = email();

        Response r = t.admin().postJson("/users", newUserBody(email, TestApp.PASSWORD, "RECRUITER"));

        assertThat(r.status()).isEqualTo(201);
        assertThat(r.field("role")).isEqualTo("RECRUITER");
        assertThat(r.field("active")).isEqualTo("true");
        assertThat(app().login(email, TestApp.PASSWORD).get("/auth/me").status()).isEqualTo(200);
    }

    @Test
    void noSePermiteUnEmailDuplicadoSinDistinguirMayusculas() {
        TestApp.Tenant t = app().newTenant();

        Response r = t.admin().postJson("/users", newUserBody(t.adminEmail().toUpperCase(), TestApp.PASSWORD, "RECRUITER"));

        assertThat(r.status()).isEqualTo(409);
    }

    @Test
    void laCreacionRechazaDatosInvalidos() {
        ApiClient admin = app().newTenant().admin();

        assertThat(admin.postJson("/users", newUserBody(email(), "corta-123", "RECRUITER")).status()).isEqualTo(400);
        assertThat(admin.postJson("/users", newUserBody("no-es-email", TestApp.PASSWORD, "RECRUITER")).status())
                .isEqualTo(400);
        assertThat(admin.postJson("/users", newUserBody(email(), TestApp.PASSWORD, "VIEWER")).status()).isEqualTo(400);
        assertThat(admin.postJson("/users", obj("email", email(), "password", TestApp.PASSWORD,
                "firstName", "Eva", "lastName", "X")).status()).isEqualTo(400);
    }

    // ---------------------------------------------------------------- permisos
    @Test
    void unRecruiterNoPuedeGestionarUsuarios() {
        TestApp.Tenant t = app().newTenant();
        ApiClient recruiter = app().addMember(t, Role.RECRUITER).client();

        assertThat(recruiter.get("/users").status()).isEqualTo(403);
        assertThat(recruiter.postJson("/users", newUserBody(email(), TestApp.PASSWORD, "RECRUITER")).status())
                .isEqualTo(403);
        assertThat(recruiter.patchJson("/users/" + meId(t.admin()), obj("firstName", "Hack")).status()).isEqualTo(403);
        assertThat(recruiter.delete("/users/" + meId(t.admin())).status()).isEqualTo(403);
    }

    @Test
    void unRecruiterVeLaListaMinimaDeMiembrosSinEmailNiRol() {
        TestApp.Tenant t = app().newTenant();
        ApiClient recruiter = app().addMember(t, Role.RECRUITER).client();

        Response r = recruiter.get("/users/members");

        assertThat(r.status()).isEqualTo(200);
        assertThat(r.body()).contains("firstName").doesNotContain("email").doesNotContain("role");
    }

    @Test
    void laListaDeMiembrosExcluyeUsuariosInactivos() {
        TestApp.Tenant t = app().newTenant();
        TestApp.Member inactive = app().addMember(t, Role.RECRUITER);
        t.admin().patchJson("/users/" + inactive.id(), obj("active", false));

        Response r = t.admin().get("/users/members");

        assertThat(r.ids()).doesNotContain(inactive.id());
    }

    // ------------------------------------------------------------- aislamiento
    @Test
    void cadaOrganizacionSoloVeSusUsuarios() {
        TestApp.Tenant a = app().newTenant();
        TestApp.Tenant b = app().newTenant();

        String listA = a.admin().get("/users").body();
        String listB = b.admin().get("/users").body();

        assertThat(listA).contains(a.adminEmail()).doesNotContain(b.adminEmail());
        assertThat(listB).contains(b.adminEmail()).doesNotContain(a.adminEmail());
    }

    @Test
    void noSePuedeEditarNiBorrarUsuariosDeOtraOrganizacion() {
        TestApp.Tenant a = app().newTenant();
        TestApp.Tenant b = app().newTenant();
        long victim = meId(b.admin());

        assertThat(a.admin().patchJson("/users/" + victim, obj("firstName", "Hack")).status()).isEqualTo(404);
        assertThat(a.admin().patchJson("/users/" + victim, obj("active", false)).status()).isEqualTo(404);
        assertThat(a.admin().delete("/users/" + victim).status()).isEqualTo(404);

        Response still = b.admin().get("/auth/me");
        assertThat(still.status()).isEqualTo(200);
        assertThat(still.field("firstName")).isEqualTo("Ana");
    }

    // ------------------------------------------------- regla del último admin
    @Test
    void elUnicoAdministradorNoPuedeSerDegradadoDesactivadoNiBorrado() {
        TestApp.Tenant t = app().newTenant();
        long id = meId(t.admin());

        assertThat(t.admin().patchJson("/users/" + id, obj("role", "RECRUITER")).status()).isEqualTo(409);
        assertThat(t.admin().patchJson("/users/" + id, obj("active", false)).status()).isEqualTo(409);
        assertThat(t.admin().delete("/users/" + id).status()).isEqualTo(400);

        assertThat(t.admin().get("/auth/me").field("role")).isEqualTo("ADMIN");
    }

    @Test
    void conDosAdministradoresSePuedeDegradarUnoPeroNoElUltimo() {
        TestApp.Tenant t = app().newTenant();
        TestApp.Member second = app().addMember(t, Role.ADMIN);

        assertThat(t.admin().patchJson("/users/" + second.id(), obj("role", "RECRUITER")).status()).isEqualTo(200);
        assertThat(t.admin().patchJson("/users/" + meId(t.admin()), obj("role", "RECRUITER")).status())
                .isEqualTo(409);
    }

    @Test
    void unAdministradorPuedeDegradarseASiMismoSiQuedaOtro() {
        TestApp.Tenant t = app().newTenant();
        app().addMember(t, Role.ADMIN);

        assertThat(t.admin().patchJson("/users/" + meId(t.admin()), obj("role", "RECRUITER")).status())
                .isEqualTo(200);
        assertThat(t.admin().get("/users").status()).isEqualTo(403);
    }

    @Test
    void dosAdministradoresNoPuedenDegradarseMutuamenteALaVez() throws Exception {
        for (int i = 0; i < 3; i++) {
            TestApp.Tenant t = app().newTenant();
            TestApp.Member other = app().addMember(t, Role.ADMIN);
            long adminId = meId(t.admin());

            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch go = new CountDownLatch(1);
            ExecutorService pool = Executors.newFixedThreadPool(2);
            try {
                Future<Integer> first = pool.submit(() -> demote(t.admin(), other.id(), ready, go));
                Future<Integer> second = pool.submit(() -> demote(other.client(), adminId, ready, go));
                ready.await();
                go.countDown();
                List<Integer> statuses = List.of(first.get(60, TimeUnit.SECONDS), second.get(60, TimeUnit.SECONDS));

                // Exactamente uno lo consigue; el otro se encuentra con que ya no es admin (403) o es el último (409).
                assertThat(statuses).filteredOn(s -> s == 200).hasSize(1);
                assertThat(statuses).filteredOn(s -> s != 200).allMatch(s -> s == 403 || s == 409);
            } finally {
                pool.shutdownNow();
            }

            Integer admins = jdbc.queryForObject(
                    "select count(*) from users where organization_id = ? and role = 'ADMIN' and active",
                    Integer.class, t.organizationId());
            assertThat(admins).isEqualTo(1);
        }
    }

    private int demote(ApiClient actor, long targetId, CountDownLatch ready, CountDownLatch go)
            throws InterruptedException {
        actor.get("/auth/me"); // cada hilo prepara su cookie CSRF antes de la carrera
        ready.countDown();
        go.await();
        return actor.patchJson("/users/" + targetId, obj("role", "RECRUITER")).status();
    }

    // ----------------------------------------------------------------- sesiones
    @Test
    void unUsuarioDesactivadoPierdeLaSesionDeInmediato() {
        TestApp.Tenant t = app().newTenant();
        TestApp.Member member = app().addMember(t, Role.RECRUITER);
        assertThat(member.client().get("/auth/me").status()).isEqualTo(200);

        t.admin().patchJson("/users/" + member.id(), obj("active", false));

        assertThat(member.client().get("/auth/me").status()).isEqualTo(401);
        assertThat(new ApiClient(baseUrl()).postJson("/auth/login",
                obj("email", member.email(), "password", TestApp.PASSWORD), Csrf.NONE).status()).isEqualTo(401);
    }

    @Test
    void unUsuarioBorradoPierdeLaSesionYNoPuedeEntrar() {
        TestApp.Tenant t = app().newTenant();
        TestApp.Member member = app().addMember(t, Role.RECRUITER);

        assertThat(t.admin().delete("/users/" + member.id()).status()).isEqualTo(204);

        assertThat(member.client().get("/auth/me").status()).isEqualTo(401);
        assertThat(new ApiClient(baseUrl()).postJson("/auth/login",
                obj("email", member.email(), "password", TestApp.PASSWORD), Csrf.NONE).status()).isEqualTo(401);
    }

    @Test
    void elCambioDeRolSeAplicaAlInstanteSinVolverAIniciarSesion() {
        TestApp.Tenant t = app().newTenant();
        TestApp.Member member = app().addMember(t, Role.RECRUITER);
        assertThat(member.client().get("/users").status()).isEqualTo(403);

        t.admin().patchJson("/users/" + member.id(), obj("role", "ADMIN"));
        assertThat(member.client().get("/users").status()).isEqualTo(200);

        t.admin().patchJson("/users/" + member.id(), obj("role", "RECRUITER"));
        assertThat(member.client().get("/users").status()).isEqualTo(403);
    }

    // ---------------------------------------------------------- actualización
    @Test
    void laActualizacionParcialConservaElRestoDeLosCampos() {
        TestApp.Tenant t = app().newTenant();
        TestApp.Member member = app().addMember(t, Role.RECRUITER);

        Response r = t.admin().patchJson("/users/" + member.id(), obj("firstName", "Nuevo"));

        assertThat(r.status()).isEqualTo(200);
        assertThat(r.field("firstName")).isEqualTo("Nuevo");
        assertThat(r.field("lastName")).isEqualTo("RECRUITER");
        assertThat(r.field("role")).isEqualTo("RECRUITER");
        assertThat(r.field("active")).isEqualTo("true");
    }

    @Test
    void laActualizacionRechazaNombresEnBlanco() {
        TestApp.Tenant t = app().newTenant();
        TestApp.Member member = app().addMember(t, Role.RECRUITER);

        assertThat(t.admin().patchJson("/users/" + member.id(), obj("firstName", "   ")).status()).isEqualTo(400);
    }

    @Test
    void ningunaRespuestaIncluyeContrasenasNiHashes() {
        TestApp.Tenant t = app().newTenant();
        Response created = t.admin().postJson("/users", newUserBody(email(), TestApp.PASSWORD, "RECRUITER"));
        Response list = t.admin().get("/users");

        for (Response r : List.of(created, list)) {
            assertThat(r.body().toLowerCase()).doesNotContain("password").doesNotContain("hash")
                    .doesNotContain("argon");
        }
    }

    // ---------------------------------------------------------------- auditoría
    @Test
    void laAuditoriaRegistraAltasCambiosYBajasSinDatosPersonales() {
        TestApp.Tenant t = app().newTenant();
        String email = email();
        long id = t.admin().postJson("/users", newUserBody(email, TestApp.PASSWORD, "RECRUITER")).id();
        t.admin().patchJson("/users/" + id, obj("firstName", "Cambiado"));
        t.admin().delete("/users/" + id);

        List<String> actions = jdbc.queryForList(
                "select action from audit_log where organization_id = ? and action like 'USER_%' order by id",
                String.class, t.organizationId());
        List<String> details = jdbc.queryForList(
                "select details from audit_log where organization_id = ? and action like 'USER_%'",
                String.class, t.organizationId());

        assertThat(actions).containsExactly("USER_CREATE", "USER_UPDATE", "USER_DELETE");
        assertThat(details).allSatisfy(d -> assertThat(d).contains("target=" + id)
                .doesNotContain("@").doesNotContain("Cambiado"));
    }
}