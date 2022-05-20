package sharedconfig.utils;

import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sharedconfig.helpers.StringHelper;
import sharedconfig.utils.tuples.Tuple2;

import java.util.Optional;
import java.util.TreeMap;

public class NodeItemAttributes extends TreeMap<String, String> {
    public NodeItemAttributes() {
        super(String.CASE_INSENSITIVE_ORDER);
    }

    public String put(@Nullable String regionName, @NotNull String localName, @Nullable String value) {
        var name = localName;
        if (!StringHelper.isNullOrEmpty(regionName)) {
            name = regionName + ":" + name;
        }
        return this.put(name, value);
    }

    public Optional<String> tryGet(@NotNull String name) {
        return this.containsKey(name) ? Optional.ofNullable(this.get(name)) : Optional.empty();
    }
}
