package dev.enekoyranzo.talentfinder.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

/**
 * Al arrancar, escribe, lee y borra un objeto de prueba. Si algo falla, la aplicación no
 * arranca: es mejor descubrir un problema de almacén o de permisos aquí que con un cliente.
 * Se desactiva con app.storage.verify-on-startup=false (por ejemplo, en tests).
 */
@Component
@ConditionalOnProperty(name = "app.storage.verify-on-startup", havingValue = "true", matchIfMissing = true)
public class StorageStartupCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StorageStartupCheck.class);

    private final CvStorage storage;

    public StorageStartupCheck(CvStorage storage) {
        this.storage = storage;
    }

    @Override
    public void run(ApplicationArguments args) {
        // La organización 0 no existe: el objeto de prueba nunca colisiona con datos reales.
        String key = CvStorage.newKey(0L);
        byte[] probe = ("probe-" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);

        try {
            storage.put(key, probe, "text/plain");

            byte[] back = storage.get(key)
                    .orElseThrow(() -> new IllegalStateException("El objeto escrito no se puede leer."));
            if (!Arrays.equals(probe, back)) {
                throw new IllegalStateException("El contenido leído no coincide con el escrito.");
            }

            storage.delete(key);

            if (storage.get(key).isPresent()) {
                throw new IllegalStateException("El objeto de prueba sigue existiendo tras borrarlo.");
            }

            log.info("Almacenamiento de CVs verificado: escritura, lectura y borrado correctos.");
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "No se pudo verificar el almacenamiento de CVs. "
                            + "Comprueba que el contenedor 'storage' está levantado y que las claves de secrets/ son correctas.",
                    e);
        }
    }
}