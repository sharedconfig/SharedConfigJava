package sharedconfig.utils.collections;

import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

/**
 * HashMap с возможностью доступа по двум типам ключей. Дубликаты ключей запрещены.
 * K1 считается primary-ключем и обладает наибольшей скоростью получения значения
 */
public class BiKeyHashMap <K1, K2, V> {
    private final BiHashMap<K2, K1> k2k1Map = new BiHashMap<>();
    private final HashMap<K1, V> hashMap = new HashMap<>();

    public boolean containsKey1(K1 key) {
        return hashMap.containsKey(key);
    }

    public boolean containsKey2(K2 key) {
        var k1 = k2k1Map.getByKey(key);
        if (k1 == null)
            return false;
        return hashMap.containsKey(k1);
    }

    public @Nullable V put(@NotNull K1 key1, @NotNull K2 key2, V value) {

        val prevK1 = k2k1Map.getByKey(key2);
        if (prevK1 != null) {
            throw new IllegalStateException(String.format("Mapping [%s]-[%s] exists", prevK1, key2));
        }
        val prevK2 = k2k1Map.getByValue(key1);
        if (prevK2 != null) {
            throw new IllegalStateException(String.format("Mapping [%s]-[%s] exists", key1, prevK2));
        }

        k2k1Map.putKV(key2, key1);
        return hashMap.put(key1, value);
    }

    public @Nullable V getByKey1(@Nullable K1 key) {
        return hashMap.get(key);
    }

    public @Nullable V getByKey2(@Nullable K2 key) {
        var k1 = k2k1Map.getByKey(key);
        if (k1 == null)
            return null;
        return hashMap.get(k1);
    }

    public int size() {
        return hashMap.size();
    }

    public Set<K1> keySet1() {
        return hashMap.keySet();
    }

    public Set<K2> keySet2() {
        return k2k1Map.key1Set();
    }

    public Collection<V> values() {
        return hashMap.values();
    }

    public HashMap<K1, V> getHashMap() { return hashMap; }
}
