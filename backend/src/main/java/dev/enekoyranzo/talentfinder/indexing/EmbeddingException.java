package dev.enekoyranzo.talentfinder.indexing;

/** Fallo al calcular un embedding. El mensaje nunca incluye el texto: puede ser un CV. */
public class EmbeddingException extends RuntimeException {

    public EmbeddingException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmbeddingException(String message) {
        super(message);
    }
}