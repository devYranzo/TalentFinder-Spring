package dev.enekoyranzo.talentfinder.support;

import dev.enekoyranzo.talentfinder.indexing.TextEmbedder;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Embebedor de tests: bolsa de palabras con hash a 384 dimensiones, normalizado.
 * Dos textos que comparten palabras tienen similitud alta. No entiende significado, solo vocabulario.
 */
public class FakeTextEmbedder implements TextEmbedder {

    public static final int DIMENSIONS = 384;
    private static final Pattern WORD = Pattern.compile("\\p{L}{2,}");

    @Override
    public String modelId() {
        return "fake-bag-of-words";
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }

    @Override
    public float[] embedQuery(String text) {
        return embed(text);
    }

    @Override
    public List<float[]> embedPassages(List<String> texts) {
        return texts.stream().map(FakeTextEmbedder::embed).toList();
    }

    static float[] embed(String text) {
        String plain = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        float[] v = new float[DIMENSIONS];
        Matcher m = WORD.matcher(plain);
        while (m.find()) {
            v[Math.floorMod(m.group().hashCode(), DIMENSIONS)] += 1f;
        }
        double sum = 0;
        for (float x : v) {
            sum += (double) x * x;
        }
        double norm = Math.sqrt(sum);
        if (norm > 0) {
            for (int i = 0; i < v.length; i++) {
                v[i] = (float) (v[i] / norm);
            }
        }
        return v;
    }
}