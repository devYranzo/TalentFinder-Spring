package dev.enekoyranzo.talentfinder.indexing;

import dev.enekoyranzo.talentfinder.support.FakeTextEmbedder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmbeddingStartupCheckTest {

    private static final EmbeddingProperties PROPS =
            new EmbeddingProperties("fake", "fake-bag-of-words", FakeTextEmbedder.DIMENSIONS, true);

    /** Embebedor configurable para provocar cada tipo de fallo. */
    private static TextEmbedder broken(int dims, boolean normalized, boolean constant) {
        return new TextEmbedder() {
            private final FakeTextEmbedder real = new FakeTextEmbedder();

            private float[] fix(float[] v) {
                float[] out = new float[dims];
                if (constant) {
                    out[0] = 1f;
                    return out;
                }
                System.arraycopy(v, 0, out, 0, Math.min(v.length, dims));
                if (!normalized) {
                    for (int i = 0; i < out.length; i++) {
                        out[i] *= 5f;
                    }
                }
                return out;
            }

            @Override public String modelId() { return "roto"; }
            @Override public int dimensions() { return dims; }
            @Override public float[] embedQuery(String text) { return fix(real.embedQuery(text)); }
            @Override public List<float[]> embedPassages(List<String> texts) {
                return real.embedPassages(texts).stream().map(this::fix).toList();
            }
        };
    }

    @Test
    void unEmbebedorCorrectoPasaLaVerificacion() {
        EmbeddingStartupCheck check = new EmbeddingStartupCheck(new FakeTextEmbedder(), PROPS);

        assertThatCode(() -> check.run(null)).doesNotThrowAnyException();
    }

    @Test
    void fallaSiLasDimensionesNoCoinciden() {
        EmbeddingStartupCheck check = new EmbeddingStartupCheck(broken(100, true, false), PROPS);

        assertThatThrownBy(() -> check.run(null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("dimensiones");
    }

    @Test
    void fallaSiLosVectoresNoEstanNormalizados() {
        EmbeddingStartupCheck check = new EmbeddingStartupCheck(
                broken(FakeTextEmbedder.DIMENSIONS, false, false), PROPS);

        assertThatThrownBy(() -> check.run(null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("normalizados");
    }

    @Test
    void fallaSiElModeloNoDistingueTemas() {
        EmbeddingStartupCheck check = new EmbeddingStartupCheck(
                broken(FakeTextEmbedder.DIMENSIONS, true, true), PROPS);

        assertThatThrownBy(() -> check.run(null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("no distingue");
    }
}