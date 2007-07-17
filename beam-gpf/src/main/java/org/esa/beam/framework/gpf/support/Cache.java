package org.esa.beam.framework.gpf.support;

import java.util.HashMap;
import java.util.Map;

/**
 * A generic cache. The data is stored as key-value pairs.
 *
 * @param <ID>
 * the type of a unique id to identify the cache
 * @param <K>
 * the type of the key object, to identify cached data
 * @param <V>
 * the type of cached data
 */
public class Cache<ID, K, V> {

    private final ID id;

    private Map<K, V> map;

    /**
     * Creates a new <code>Cache</code> instance with the given id.
     *
     * @param id a unique id for the <code>Cache</code>
     */
    public Cache(ID id) {
        this.id = id;
        this.map = new HashMap<K, V>();
    }

    /**
     * Returns the <code>Cache</code>'s identifier.
     *
     * @return the id of this <code>Cache</code>
     */
    public ID getId() {
        return id;
    }

    /**
     * Returns the cached data object to which the specified key is mapped, or
     * <code>null</code> if this cache contains no mapping for the key.
     *
     * @param key the key whose associated cached value is to be returned
     * @return the cached value to which the specified key is mapped, or null if
     *         this map contains no mapping for the key
     */
    public V getValue(K key) {
        return map.get(key);
    }

    /**
     * Stores a key-value pair inside this cache.
     *
     * @param key   the key associated with the stored value
     * @param value the value to be stored
     * @return the value of the stored object
     */
    public V setValue(K key, V value) {
        return map.put(key, value);
    }

    /**
     * Removes a key-value pair from this cache.
     *
     * @param key the key associated with the stored value
     * @return the old value, or <code>null</code>
     */
    public V removeValue(K key) {
        return map.remove(key);
    }

    /**
     * Removes all of the key-value pairs stored in this cache; The cache will
     * be empty after this call returns.
     */
    public void dispose() {
        map.clear();
    }
}
