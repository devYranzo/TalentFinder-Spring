package dev.enekoyranzo.talentfinder.support;

import dev.enekoyranzo.talentfinder.storage.CvStorage;
import dev.enekoyranzo.talentfinder.storage.StorageException;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Almacén falso para tests: guarda los objetos en memoria y permite simular que el almacén
 * está caído. Equivale a cómo se comporta el real (borrar algo que no existe no es un error).
 */
public class InMemoryCvStorage implements CvStorage {

    private final ConcurrentHashMap<String, byte[]> objects = new ConcurrentHashMap<>();
    private volatile boolean failing;

    @Override
    public void put(String key, byte[] content, String contentType) {
        failIfDown();
        objects.put(key, content.clone());
    }

    @Override
    public Optional<byte[]> get(String key) {
        failIfDown();
        byte[] content = objects.get(key);
        return Optional.ofNullable(content == null ? null : content.clone());
    }

    @Override
    public void delete(String key) {
        failIfDown();
        objects.remove(key);
    }

    // ---------------------------------------------------------- ayudas de test
    /** Claves de todos los objetos guardados ahora mismo. */
    public Set<String> keys() {
        return Set.copyOf(objects.keySet());
    }

    public int size() {
        return objects.size();
    }

    /** true = cualquier operación lanza StorageException, como si el almacén estuviera caído. */
    public void setFailing(boolean failing) {
        this.failing = failing;
    }

    public void reset() {
        objects.clear();
        failing = false;
    }

    private void failIfDown() {
        if (failing) {
            throw new StorageException("Almacén simulado como caído.");
        }
    }
}