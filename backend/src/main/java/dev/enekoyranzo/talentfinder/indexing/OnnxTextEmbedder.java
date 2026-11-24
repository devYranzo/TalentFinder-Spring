package dev.enekoyranzo.talentfinder.indexing;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/** Implementación real: modelo E5 multilingüe ejecutado en el propio proceso (ONNX). */
@Component
@ConditionalOnProperty(name = "app.embedding.provider", havingValue = "onnx", matchIfMissing = true)
public class OnnxTextEmbedder implements TextEmbedder {

    // Los modelos E5 exigen estos prefijos; Spring AI no los añade.
    private static final String QUERY_PREFIX = "query: ";
    private static final String PASSAGE_PREFIX = "passage: ";

    private final EmbeddingModel model;
    private final EmbeddingProperties props;

    public OnnxTextEmbedder(EmbeddingModel model, EmbeddingProperties props) {
        this.model = model;
        this.props = props;
    }

    @Override
    public String modelId() {
        return props.modelId();
    }

    @Override
    public int dimensions() {
        return props.dimensions();
    }

    @Override
    public float[] embedQuery(String text) {
        try {
            return checked(normalize(model.embed(QUERY_PREFIX + text)));
        } catch (EmbeddingException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new EmbeddingException("No se pudo calcular el embedding de la consulta.", e);
        }
    }

    @Override
    public List<float[]> embedPassages(List<String> texts) {
        if (texts.isEmpty()) {
            return List.of();
        }
        try {
            List<String> prefixed = texts.stream().map(t -> PASSAGE_PREFIX + t).toList();
            List<float[]> raw = model.embed(prefixed);
            if (raw.size() != texts.size()) {
                throw new EmbeddingException("El modelo devolvió un número de vectores distinto al pedido.");
            }
            return raw.stream().map(v -> checked(normalize(v))).toList();
        } catch (EmbeddingException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new EmbeddingException("No se pudieron calcular los embeddings de los fragmentos.", e);
        }
    }

    private float[] checked(float[] vector) {
        if (vector.length != props.dimensions()) {
            throw new EmbeddingException("El modelo devolvió " + vector.length
                    + " dimensiones y se esperaban " + props.dimensions() + ".");
        }
        return vector;
    }

    static float[] normalize(float[] v) {
        double sum = 0;
        for (float x : v) {
            sum += (double) x * x;
        }
        double norm = Math.sqrt(sum);
        if (norm == 0) {
            return v;
        }
        float[] out = new float[v.length];
        for (int i = 0; i < v.length; i++) {
            out[i] = (float) (v[i] / norm);
        }
        return out;
    }
}