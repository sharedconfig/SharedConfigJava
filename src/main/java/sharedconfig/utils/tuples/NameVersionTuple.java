package sharedconfig.utils.tuples;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;
import sharedconfig.helpers.StringHelper;

import java.util.Objects;

@Getter
@AllArgsConstructor
@ToString
public class NameVersionTuple {
    private final @Nullable String name;
    private final @Nullable String version;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass() && !getClass().isAssignableFrom(o.getClass()))
            return false;
        NameVersionTuple that = (NameVersionTuple) o;
        return StringHelper.equalsIgnoreCase(name, that.name) && StringHelper.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(StringHelper.hashCodeIgnoreCase(name), version);
    }
}
