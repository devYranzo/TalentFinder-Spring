package dev.enekoyranzo.talentfinder.support;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class TestPdfs {

    private TestPdfs() {
    }

    /** PDF válido de una página, distinto en cada llamada (el título cambia, así que el hash también). */
    public static byte[] unique() {
        return build(false);
    }

    /** PDF cifrado con contraseña: la aplicación debe rechazarlo. */
    public static byte[] encrypted() {
        return build(true);
    }

    /** Empieza como un PDF pero no lo es: solo sirve para probar el límite de tamaño. */
    public static byte[] oversized(int totalBytes) {
        byte[] data = new byte[totalBytes];
        byte[] header = "%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(header, 0, data, 0, header.length);
        return data;
    }

    private static byte[] build(boolean encrypt) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            doc.addPage(new PDPage());
            doc.getDocumentInformation().setTitle(UUID.randomUUID().toString());
            if (encrypt) {
                StandardProtectionPolicy policy =
                        new StandardProtectionPolicy("propietario", "usuario", new AccessPermission());
                policy.setEncryptionKeyLength(128);
                doc.protect(policy);
            }
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}