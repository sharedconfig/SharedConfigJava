package sharedconfig.core;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sharedconfig.core.interfaces.IConfigurationVersionSnapshot;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@AllArgsConstructor
/* package */ class ConfigurationVersionSnapshot implements IConfigurationVersionSnapshot {
    private final @NotNull Long versionId;

    @Getter
    private final @NotNull Map<String, String> variables;

    @Override
    public @NotNull Long getVersionId() {
        return versionId;
    }

    @Override
    public @Nullable Optional<String> getVariable(@NotNull String id) {
        return Optional.ofNullable(variables.get(id));
    }


    private static final ConfigurationVersionSnapshot empty = new ConfigurationVersionSnapshot(-100L, Collections.unmodifiableMap(new HashMap<>(0)));
    public static ConfigurationVersionSnapshot empty() {
        return empty;
    }
}
