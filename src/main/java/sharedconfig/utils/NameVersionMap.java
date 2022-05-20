package sharedconfig.utils;

import org.jetbrains.annotations.NotNull;
import sharedconfig.utils.collections.FkHashMap;
import sharedconfig.utils.tuples.NameVersionTuple;
import sharedconfig.utils.tuples.StrictNameVersionTuple;

import java.util.Collection;
import java.util.List;

public class NameVersionMap<V> {
    private final FkHashMap<NameVersionTuple, String, V> map = new FkHashMap<>();

    public V put(@NotNull String name, @NotNull String version, @NotNull V value) {
        var key = new StrictNameVersionTuple(name, version);
        return this.put(key, value);
    }

    public V put(@NotNull NameVersionTuple nameVersion, @NotNull V value) {
        return this.map.put(nameVersion, nameVersion.getName(), value);
    }

    public boolean containsKey(NameVersionTuple key) { return this.map.containsKey(key); }

    public V get(@NotNull NameVersionTuple nameVersion) {
        return this.map.get(nameVersion);
    }

    public @NotNull List<V> getByName(@NotNull String name) {
        return this.map.getByFk(name);
    }

    public Collection<V> values() {
        return this.map.values();
    }
}
