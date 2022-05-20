package sharedconfig.core;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import sharedconfig.core.interfaces.IScopedConfigurationService;
import sharedconfig.core.interfaces.ISharedConfigMonitor;

/* package */ class SharedConfigMonitor<T> implements ISharedConfigMonitor<T> {
    private final @NotNull IScopedConfigurationService<T> service;

    public SharedConfigMonitor(@NotNull IScopedConfigurationService<T> service) {
        this.service = service;
    }

    private T latest = null;
    private Long latestVersion = null;
    @SneakyThrows
    @Override
    public @NotNull T latest() {
        var newLatestVersion = service.getLatestVersionId();
        if (latestVersion == null || !latestVersion.equals(newLatestVersion)) {
            latestVersion = newLatestVersion;
            latest = service.getLastVersion();
        }
        return latest;
    }
}
