package sharedconfig.utils.tuples;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import sharedconfig.helpers.StringHelper;

import java.util.Objects;

@Getter @AllArgsConstructor
@ToString
public class NameHashTuple {
    private final @NotNull String name;
    private final @NotNull String hash;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NameHashTuple that = (NameHashTuple) o;
        return StringHelper.equalsIgnoreCase(name, that.name) && StringHelper.equalsIgnoreCase(hash, that.hash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(StringHelper.hashCodeIgnoreCase(name), StringHelper.hashCodeIgnoreCase(hash));
    }
}
