package sharedconfig.core;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sharedconfig.core.exceptions.StoreNotLoadedException;
import sharedconfig.core.interfaces.IScopedConfigurationService;
import sharedconfig.core.interfaces.ISharedConfigMonitor;
import sharedconfig.utils.tuples.NameVersionTuple;

import java.util.HashMap;
import java.util.Optional;
import java.util.SortedSet;

@RequiredArgsConstructor
/* package */ class ScopedServiceCashingDecorator<Configuration>
        implements IScopedConfigurationService<Configuration> {
    private final @NotNull ConfigurationEngine engine;
    private final @NotNull IScopedConfigurationService<Configuration> decoratee;

    @Override
    public @NotNull NameVersionTuple getScopeId() throws StoreNotLoadedException {
        return decoratee.getScopeId();
    }

    @Override
    public @NotNull SortedSet<Long> getVersionIds() throws StoreNotLoadedException {
        return decoratee.getVersionIds();
    }

    @Override
    public @Nullable Long getLatestVersionId() {
        return decoratee.getLatestVersionId();
    }

    @Override
    public @NotNull ISharedConfigMonitor<Configuration> getObservedVersion() {
        return decoratee.getObservedVersion();
    }

    private Configuration getLastVersionCache;
    private long getLastVersionVersion = -1;
    @Override
    public Configuration getLastVersion() throws StoreNotLoadedException {
        var store = engine.store;
        if (store == null)
            return decoratee.getLastVersion();

        if (store.getVersion() != getLastVersionVersion) {
            getLastVersionVersion = store.getVersion();
            getLastVersionCache = decoratee.getLastVersion();
        }
        return getLastVersionCache;
    }

    private final HashMap<Long, Optional<Configuration>> getVersionCache = new HashMap<>();
    private long getVersionCacheVersion = -1;
    @Override
    public Optional<Configuration> getVersion(@NotNull Long versionId) throws StoreNotLoadedException {
        var store = engine.store;
        if (store == null)
            return decoratee.getVersion(versionId);

        if (store.getVersion() != getVersionCacheVersion) {
            getVersionCacheVersion = store.getVersion();
            getVersionCache.clear();
            getVersionCache.put(versionId, decoratee.getVersion(versionId));
        } else if (!getVersionCache.containsKey(versionId)) {
            getVersionCache.put(versionId, decoratee.getVersion(versionId));
        }
        return getVersionCache.get(versionId);
    }
}
