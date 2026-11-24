package dev.enekoyranzo.talentfinder.indexing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Al arrancar, comprueba que el modelo funciona y que entiende español.
 * Si algo falla, la aplicación no arranca: es mejor saberlo aquí que con un cliente buscando.
 * Se desactiva con app.embedding.verify-on-startup=false (tests).
 */
@Component
@ConditionalOnProperty(name = "app.embedding.verify-on-startup", havingValue = "true", matchIfMissing = true)
public class EmbeddingStartupCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingStartupCheck.class);

    private static final double MIN_MARGIN = 0.01;

    private final TextEmbedder embedder;
    private final EmbeddingProperties props;

    public EmbeddingStartupCheck(TextEmbedder embedder, EmbeddingProperties props) {
        this.embedder = embedder;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        float[] query = embedder.embedQuery("programador con experiencia en Java");
        List<float[]> passages = embedder.embedPassages(List.of(
                "Desarrollador backend con cinco años de experiencia en Java y Spring Boot",
                "Cocinero de restaurante especializado en cocina mediterránea"));

        for (float[] v : List.of(query, passages.get(0), passages.get(1))) {
            if (v.length != props.dimensions()) {
                throw fail("dimensiones " + v.length + " en lugar de " + props.dimensions());
            }
            double norm = Math.sqrt(dot(v, v));
            if (Math.abs(norm - 1.0) > 0.01) {
                throw fail("los vectores no están normalizados (norma " + norm + ")");
            }
        }

        double relevant = dot(query, passages.get(0));
        double irrelevant = dot(query, passages.get(1));
        if (relevant < irrelevant + MIN_MARGIN) {
            throw fail("el modelo no distingue un perfil de Java de uno de cocina (similitudes "
                    + relevant + " y " + irrelevant + ")");
        }

        log.info("Modelo de embeddings verificado ({}, {} dimensiones; similitud relevante {} frente a irrelevante {}).",
                embedder.modelId(), props.dimensions(), String.format("%.3f", relevant), String.format("%.3f", irrelevant));
    }

    private static IllegalStateException fail(String reason) {
        return new IllegalStateException("El modelo de embeddings no pasa la verificación: " + reason
                + ". Comprueba que ejecutaste 'bash scripts/fetch-models.sh' y que los archivos no están dañados.");
    }

    private static double dot(float[] a, float[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += (double) a[i] * b[i];
        }
        return sum;
    }
}