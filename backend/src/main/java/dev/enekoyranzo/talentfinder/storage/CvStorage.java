package dev.enekoyranzo.talentfinder.storage;

import java.util.Optional;
import java.util.UUID;

/**
 * Almacén de los PDFs de los CVs. No sabe nada de organizaciones, usuarios ni permisos:
 * quien lo llame debe haber comprobado antes que el usuario puede acceder a esa clave.
 */
public interface CvStorage {

    /** Clave nueva para un documento de una organización. */
    static String newKey(Long organizationId) {
        return "org_" + organizationId + "/" + UUID.randomUUID();
    }

    void put(String key, byte[] content, String contentType);

    Optional<byte[]> get(String key);

    /** Idempotente: borrar una clave que no existe no es un error. */
    void delete(String key);
}