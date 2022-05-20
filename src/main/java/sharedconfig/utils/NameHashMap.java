package sharedconfig.utils;

import org.jetbrains.annotations.NotNull;
import sharedconfig.utils.collections.FkHashMap;
import sharedconfig.utils.tuples.NameHashTuple;
import sharedconfig.utils.tuples.NameVersionTuple;
import sharedconfig.utils.tuples.StrictNameVersionTuple;

import java.util.List;

public class NameHashMap<V> {
    private final FkHashMap<NameHashTuple, String, V> map = new FkHashMap<>();

    public V put(@NotNull String name, @NotNull String hash, @NotNull V value) {
        var key = new NameHashTuple(name, hash);
        return this.put(key, value);
    }

    public V put(@NotNull NameHashTuple nameHash, @NotNull V value) {
        return this.map.put(nameHash, nameHash.getName(), value);
    }

    public V get(@NotNull NameHashTuple nameHash) {
        return this.map.get(nameHash);
    }

    public @NotNull List<V> getByName(@NotNull String name) {
        return this.map.getByFk(name);
    }
}
