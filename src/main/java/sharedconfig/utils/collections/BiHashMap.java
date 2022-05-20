package sharedconfig.utils.collections;

import lombok.val;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Set;

/**
 * Двунаправленный HashMap.
 * Для каждого K1 существует только один K2, для каждого K2 существует только один K1
 * При возникновении дубликатов старые отношения K1-K2 перезатираются, например пара (1,2) будет стерта при дабвлении (1,3) или (0,2)
 */
public class BiHashMap<K1, K2> {
    private final HashMap<K1, K2> hashMapKV = new HashMap<>();
    private final HashMap<K2, K1> hashMapVK = new HashMap<>();

    public boolean containsKey(K1 key) {
        return hashMapKV.containsKey(key);
    }
    public boolean containsValue(K2 value) {
        return hashMapVK.containsKey(value);
    }

    public void putKV(K1 key, K2 value) {
        // remove old mapping
        val prevValue = hashMapKV.remove(key);
        val prevKey = hashMapVK.remove(value);
        if (prevKey != null) {
            hashMapKV.remove(prevKey);
        }
        if (prevValue != null) {
            hashMapVK.remove(prevValue);
        }

        // add new mapping
        hashMapKV.put(key, value);
        hashMapVK.put(value, key);
    }

    public void putVK(K2 value, K1 key) {
        putKV(key, value);
    }

    public @Nullable K2 getByKey(K1 key) {
        return hashMapKV.get(key);
    }

    public @Nullable K1 getByValue(K2 value) {
        return hashMapVK.get(value);
    }

    public int size() {
        return hashMapKV.size();
    }

    public Set<K1> key1Set() {
        return hashMapKV.keySet();
    }

    public Set<K2> key2Set() {
        return hashMapVK.keySet();
    }
}
