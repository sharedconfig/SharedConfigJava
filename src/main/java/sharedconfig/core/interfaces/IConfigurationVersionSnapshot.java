package sharedconfig.core.interfaces;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

public interface IConfigurationVersionSnapshot {
    @NotNull Long getVersionId();
    @NotNull Map<String, String> getVariables();
    Optional<String> getVariable(@NotNull String id);
}


