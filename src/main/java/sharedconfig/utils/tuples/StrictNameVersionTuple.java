package sharedconfig.utils.tuples;

import org.jetbrains.annotations.NotNull;
import sharedconfig.utils.tuples.NameVersionTuple;

public class StrictNameVersionTuple extends NameVersionTuple {
    public StrictNameVersionTuple(@NotNull String name, @NotNull String version) {
        super(name, version);
    }

    @Override
    public @NotNull String getName() {
        assert super.getName() != null;
        return super.getName();
    }

    @Override
    public @NotNull String getVersion() {
        assert super.getVersion() != null;
        return super.getVersion();
    }
}
