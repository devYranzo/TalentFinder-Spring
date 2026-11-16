package dev.enekoyranzo.talentfinder.document;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Comprobaciones sobre un PDF subido por un usuario. No sustituye a un antivirus.
 * Nunca renderiza ni ejecuta el contenido.
 */
@Component
public class PdfValidator {

    static final int MAX_PAGES = 50;

    private static final byte[] PDF_HEADER = {'%', 'P', 'D', 'F', '-'};

    public void validate(byte[] content) {
        if (content == null || content.length < PDF_HEADER.length) {
            throw new InvalidDocumentException("NOT_PDF", "El archivo no es un PDF válido.");
        }
        for (int i = 0; i < PDF_HEADER.length; i++) {
            if (content[i] != PDF_HEADER[i]) {
                throw new InvalidDocumentException("NOT_PDF", "El archivo no es un PDF válido.");
            }
        }

        try (PDDocument doc = Loader.loadPDF(content)) {
            if (doc.isEncrypted()) {
                throw new InvalidDocumentException("ENCRYPTED", "No se admiten PDFs cifrados.");
            }

            int pages = doc.getNumberOfPages();
            if (pages < 1) {
                throw new InvalidDocumentException("EMPTY", "El PDF no tiene páginas.");
            }
            if (pages > MAX_PAGES) {
                throw new InvalidDocumentException("TOO_MANY_PAGES",
                        "El PDF supera el máximo de " + MAX_PAGES + " páginas.");
            }

            PDDocumentNameDictionary names = doc.getDocumentCatalog().getNames();
            if (names != null && names.getJavaScript() != null) {
                throw new InvalidDocumentException("ACTIVE_CONTENT", "No se admiten PDFs con código activo.");
            }
            if (names != null && names.getEmbeddedFiles() != null) {
                throw new InvalidDocumentException("EMBEDDED_FILES", "No se admiten PDFs con archivos incrustados.");
            }
        } catch (InvalidPasswordException e) {
            throw new InvalidDocumentException("ENCRYPTED", "No se admiten PDFs cifrados.");
        } catch (IOException e) {
            throw new InvalidDocumentException("NOT_PDF", "El archivo no es un PDF válido.");
        }
    }
}