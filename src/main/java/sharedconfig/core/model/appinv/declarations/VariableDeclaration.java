package sharedconfig.core.model.appinv.declarations;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sharedconfig.utils.tuples.NameHashTuple;

import java.util.Objects;

public class VariableDeclaration {
    @Getter private final @NotNull NameHashTuple id;
    @Getter private final @Nullable String description;
    @Getter private final @Nullable String defaultValue;

    public VariableDeclaration(@NotNull String name, @NotNull String hash, @Nullable String description, @Nullable String defaultValue) {
        this.id = new NameHashTuple(name, hash);
        this.description = description;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return this.id.getName();
    }

    public String getHash() {
        return this.id.getHash();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VariableDeclaration that = (VariableDeclaration) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
