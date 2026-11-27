package dev.enekoyranzo.talentfinder.indexing;

/** Un fragmento listo para guardar: posición, página, texto y su vector. */
public record NewChunk(int index, int page, String content, float[] embedding) {
}