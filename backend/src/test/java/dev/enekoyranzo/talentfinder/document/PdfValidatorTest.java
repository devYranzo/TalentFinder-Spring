package dev.enekoyranzo.talentfinder.document;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.PDJavascriptNameTreeNode;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionJavaScript;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfValidatorTest {

    private final PdfValidator validator = new PdfValidator();

    @FunctionalInterface
    private interface Customizer {
        void apply(PDDocument doc) throws IOException;
    }

    // ------------------------------------------------------------- ayudas
    private static byte[] pdf(int pages) throws IOException {
        return pdf(pages, doc -> {
        });
    }

    private static byte[] pdf(int pages, Customizer customizer) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int i = 0; i < pages; i++) {
                doc.addPage(new PDPage());
            }
            customizer.apply(doc);
            doc.save(out);
            return out.toByteArray();
        }
    }

    private void assertRejected(byte[] content, String expectedCode) {
        assertThatThrownBy(() -> validator.validate(content))
                .isInstanceOfSatisfying(InvalidDocumentException.class,
                        e -> assertThat(e.getCode()).isEqualTo(expectedCode));
    }

    // -------------------------------------------------------------- válidos
    @Test
    void aceptaUnPdfValido() throws IOException {
        byte[] content = pdf(1);

        assertThatCode(() -> validator.validate(content)).doesNotThrowAnyException();
    }

    @Test
    void aceptaExactamenteElMaximoDePaginas() throws IOException {
        byte[] content = pdf(PdfValidator.MAX_PAGES);

        assertThatCode(() -> validator.validate(content)).doesNotThrowAnyException();
    }

    @Test
    void aceptaPdfConArbolesDeNombresVacios() throws IOException {
        // Algunos generadores crean estos árboles aunque no contengan nada. No es contenido peligroso.
        byte[] content = pdf(1, doc -> {
            PDDocumentNameDictionary names = new PDDocumentNameDictionary(doc.getDocumentCatalog());
            names.setEmbeddedFiles(new PDEmbeddedFilesNameTreeNode());
            names.setJavascript(new PDJavascriptNameTreeNode());
            doc.getDocumentCatalog().setNames(names);
        });

        assertThatCode(() -> validator.validate(content)).doesNotThrowAnyException();
    }

    // ------------------------------------------------------------ rechazos
    @Test
    void rechazaMasDelMaximoDePaginas() throws IOException {
        assertRejected(pdf(PdfValidator.MAX_PAGES + 1), "TOO_MANY_PAGES");
    }

    @Test
    void rechazaContenidoNuloVacioOMuyCorto() {
        assertRejected(null, "NOT_PDF");
        assertRejected(new byte[0], "NOT_PDF");
        assertRejected(new byte[]{'%', 'P', 'D'}, "NOT_PDF");
    }

    @Test
    void rechazaUnArchivoQueNoEmpiezaPorLaCabeceraPdf() {
        assertRejected("hola, esto no es un pdf".getBytes(StandardCharsets.UTF_8), "NOT_PDF");
        // Cabecera de un ZIP: un .pdf que en realidad es otra cosa.
        assertRejected(new byte[]{'P', 'K', 3, 4, 0, 0, 0, 0}, "NOT_PDF");
    }

    @Test
    void rechazaCabeceraPdfConContenidoCorrupto() {
        assertRejected("%PDF-1.7\nesto no es la estructura de un pdf".getBytes(StandardCharsets.UTF_8), "NOT_PDF");
    }

    @Test
    void rechazaPdfCifrado() throws IOException {
        byte[] content = pdf(1, doc -> {
            StandardProtectionPolicy policy =
                    new StandardProtectionPolicy("clave-propietario", "clave-usuario", new AccessPermission());
            policy.setEncryptionKeyLength(128);
            doc.protect(policy);
        });

        assertRejected(content, "ENCRYPTED");
    }

    @Test
    void rechazaPdfConJavaScript() throws IOException {
        byte[] content = pdf(1, doc -> {
            PDJavascriptNameTreeNode scripts = new PDJavascriptNameTreeNode();
            scripts.setNames(Map.of("inicio", new PDActionJavaScript("app.alert('hola');")));

            PDDocumentNameDictionary names = new PDDocumentNameDictionary(doc.getDocumentCatalog());
            names.setJavascript(scripts);
            doc.getDocumentCatalog().setNames(names);
        });

        assertRejected(content, "ACTIVE_CONTENT");
    }

    @Test
    void rechazaPdfConArchivosIncrustados() throws IOException {
        byte[] content = pdf(1, doc -> {
            PDEmbeddedFile embedded = new PDEmbeddedFile(doc,
                    new ByteArrayInputStream("contenido adjunto".getBytes(StandardCharsets.UTF_8)));
            PDComplexFileSpecification spec = new PDComplexFileSpecification();
            spec.setFile("adjunto.txt");
            spec.setEmbeddedFile(embedded);

            PDEmbeddedFilesNameTreeNode files = new PDEmbeddedFilesNameTreeNode();
            files.setNames(Map.of("adjunto.txt", spec));

            PDDocumentNameDictionary names = new PDDocumentNameDictionary(doc.getDocumentCatalog());
            names.setEmbeddedFiles(files);
            doc.getDocumentCatalog().setNames(names);
        });

        assertRejected(content, "EMBEDDED_FILES");
    }
}