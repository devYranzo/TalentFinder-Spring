package dev.enekoyranzo.talentfinder.seed;

import dev.enekoyranzo.talentfinder.organization.Organization;
import dev.enekoyranzo.talentfinder.organization.OrganizationRepository;
import dev.enekoyranzo.talentfinder.user.Role;
import dev.enekoyranzo.talentfinder.user.User;
import dev.enekoyranzo.talentfinder.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea datos de prueba SOLO con el perfil "dev". Es idempotente: se puede ejecutar
 * en cada arranque sin duplicar nada. No registra nunca la contraseña.
 */
@Component
@Profile("dev")
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    static final String ORG_NAME = "Organización de prueba";
    static final String ADMIN_EMAIL = "admin@talentfinder.test";
    static final String RECRUITER_EMAIL = "recruiter@talentfinder.test";
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final Environment env;
    private final OrganizationRepository organizations;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String password;

    public DevDataSeeder(Environment env,
                         OrganizationRepository organizations,
                         UserRepository users,
                         PasswordEncoder encoder,
                         @Value("${app.dev-seed.password:}") String password) {
        this.env = env;
        this.organizations = organizations;
        this.users = users;
        this.encoder = encoder;
        this.password = password == null ? "" : password.trim();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (env.acceptsProfiles(Profiles.of("prod", "production"))) {
            throw new IllegalStateException(
                    "El seeder de desarrollo no puede ejecutarse con el perfil de producción.");
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            log.warn("Seeder de desarrollo omitido: falta secrets/dev_user_password.txt "
                    + "o tiene menos de {} caracteres.", MIN_PASSWORD_LENGTH);
            return;
        }

        User admin = users.findByEmailIgnoreCase(ADMIN_EMAIL).orElse(null);

        if (admin == null) {
            if (organizations.existsByNameIgnoreCase(ORG_NAME)) {
                log.warn("Seeder de desarrollo omitido: ya existe la organización '{}' "
                        + "pero no su usuario administrador.", ORG_NAME);
                return;
            }

            Organization org = organizations.saveAndFlush(new Organization(ORG_NAME));
            admin = users.saveAndFlush(new User(
                    org.getId(), ADMIN_EMAIL, encoder.encode(password),
                    "Admin", "Prueba", Role.ADMIN));
            log.info("Seeder de desarrollo: creado el usuario {}", ADMIN_EMAIL);
        }

        if (!users.existsByEmailIgnoreCase(RECRUITER_EMAIL)) {
            users.saveAndFlush(new User(
                    admin.getOrganizationId(), RECRUITER_EMAIL, encoder.encode(password),
                    "Recruiter", "Prueba", Role.RECRUITER));
            log.info("Seeder de desarrollo: creado el usuario {}", RECRUITER_EMAIL);
        }
    }
}