package sharedconfig.utils.collections;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Мапа с возможностью доступа по индексируемому внешнему ключу
 */
public class FkHashMap<K, Fk, V> {
    private final HashMap<K, V> map = new HashMap<>();
    private final HashMap<Fk, List<V>> groups = new HashMap<>();

    public V put(K key, Fk foreignKey, V value) {
        var existingValue = this.map.get(key);
        if (existingValue != null) {
            this.groups.get(foreignKey).remove(existingValue);
        }

        var result = this.map.put(key, value);
        this.groups.computeIfAbsent(foreignKey, (__ -> new ArrayList<>())).add(value);
        return result;
    }

    public boolean containsKey(K key) {
        return this.map.containsKey(key);
    }

    public V get(K key) {
        return this.map.get(key);
    }

    public @NotNull List<V> getByFk(Fk key) {
        var result = this.groups.get(key);
        return result == null ? Collections.emptyList() : Collections.unmodifiableList(result);
    }

    public Collection<V> values() {
        return this.map.values();
    }

    public Set<K> keySet() {
        return this.map.keySet();
    }
}
