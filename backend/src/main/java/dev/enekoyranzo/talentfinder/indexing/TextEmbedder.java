package dev.enekoyranzo.talentfinder.indexing;

import java.util.List;

/**
 * Convierte texto en vectores. El resto de la aplicación solo conoce esta interfaz:
 * cambiar de modelo o de proveedor no debe tocar nada más.
 * Los vectores devueltos están normalizados (norma 1).
 */
public interface TextEmbedder {

    /** Identificador del modelo y revisión; se guarda junto a cada vector. */
    String modelId();

    int dimensions();

    /** Vector de una consulta de búsqueda. */
    float[] embedQuery(String text);

    /** Vectores de fragmentos de documentos, en el mismo orden que la entrada. */
    List<float[]> embedPassages(List<String> texts);
}