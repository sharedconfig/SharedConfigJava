package sharedconfig.core;

import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sharedconfig.core.exceptions.StoreNotLoadedException;
import sharedconfig.core.interfaces.IScopedConfigurationService;
import sharedconfig.core.interfaces.IConfigurationVersionSnapshot;
import sharedconfig.core.interfaces.ISharedConfigMonitor;
import sharedconfig.utils.Version;
import sharedconfig.utils.tuples.NameVersionTuple;
import sharedconfig.utils.tuples.StrictNameVersionTuple;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

@Log4j2
/* package */ class BlockScopedConfigurationService<Configuration>
        implements IScopedConfigurationService<Configuration>  {
    private final @NotNull ConfigurationEngine engine;
    private final @NotNull String blockName;
    private final @Nullable String blockVersion;
    private final @NotNull Function<@NotNull IConfigurationVersionSnapshot, @NotNull Configuration> builder;
    private @Nullable StrictNameVersionTuple blockId = null;
    private @Nullable Boolean blockExists = null;

    public BlockScopedConfigurationService(@NotNull ConfigurationEngine engine, @NotNull String blockName, @Nullable String blockVersion, @NotNull Function<@NotNull IConfigurationVersionSnapshot, @NotNull Configuration> builder) {
        this.engine = engine;
        this.blockName = blockName;
        this.blockVersion = blockVersion;
        this.builder = builder;
    }

    private void ensureBlockInfoLoaded() throws StoreNotLoadedException {
        if (blockId != null || this.blockExists != null)
            return;

        val store = engine.store;
        if (store == null || store.getVersion() == 0)
            throw new StoreNotLoadedException("Store not loaded");

        if (blockVersion != null) {
            var blockId = new StrictNameVersionTuple(blockName, blockVersion);
            if (store.getApplicationInvDeclaration().getBlocks().containsKey(blockId)) {
                this.blockId = blockId;
                this.blockExists = true;
            } else {
                this.blockExists = false;
            }
        } else {
            var sameNameBlocks = store.getApplicationInvDeclaration().getBlocks().getByName(this.blockName);
            var latestBlockVersion = sameNameBlocks.stream()
                    .map(b -> new Version(b.getId().getVersion()))
                    .max(Comparator.naturalOrder())
                    .orElse(null);
            if (latestBlockVersion != null) {
                this.blockId = new StrictNameVersionTuple(blockName, latestBlockVersion.toString());
                this.blockExists = true;
            } else {
                this.blockExists = false;
            }
        }
    }

    @Override
    public @NotNull NameVersionTuple getScopeId() throws StoreNotLoadedException {
        ensureBlockInfoLoaded();
        if (Boolean.TRUE.equals(this.blockExists)) {
            assert this.blockId != null;
            return this.blockId;
        }
        throw new IllegalStateException(String.format("Couldn't find block with name='%s' and version='%s' or store hasn't been loaded correctly", this.blockName, this.blockVersion));
    }

    @Override
    public @NotNull SortedSet<Long> getVersionIds() throws StoreNotLoadedException {
        var store = engine.store;
        if (store == null) {
            log.warn("Unsuccessful attempt to get versions list - configuration information hasn't been loaded");
            return Collections.emptySortedSet();
        }

        return Collections.unmodifiableSortedSet(store.actualChangesetIds);
    }

    @Override
    public @NotNull ISharedConfigMonitor<Configuration> getObservedVersion() {
        return new SharedConfigMonitor<>(this);
    }

    @Override
    public @Nullable Long getLatestVersionId() {
        var store = engine.store;
        if (store == null) {
            return null;
        }
        return store.getVersion();
    }

    private ConfigurationVersionSnapshot toScopedSnapshot(@Nullable ConfigurationVersion confVersion) {
        if (confVersion == null)
            return null;

        var chagesetId = confVersion.getVersion();
        var allVersionVarValues = confVersion.getStoreItem().getVariables();
        var store = engine.store;
        var resultVars = new HashMap<String, String>();

        assert store != null;
        assert this.blockId != null;
        for (var blockVariable : store.getApplicationInvDeclaration().getBlocks().get(this.blockId).getVariables().values()) {
            var blockVariableValue = allVersionVarValues.get(blockVariable.getId());
            if (blockVariableValue != null) {
                resultVars.put(blockVariable.getName(), blockVariableValue.getValue());
            }
        }

        return new ConfigurationVersionSnapshot(chagesetId, Collections.unmodifiableMap(resultVars));
    }

    @Override
    public Configuration getLastVersion() throws StoreNotLoadedException {
        ensureBlockInfoLoaded();
        if (!Boolean.TRUE.equals(this.blockExists)) {
            log.warn("Configuration information hasn't been loaded or block not exists. Returning default version");
            return builder.apply(ConfigurationVersionSnapshot.empty());
        }

        var store = engine.store; assert store != null;
        if (store.maxChangesetId == null) {
            log.warn("Unsuccessful attempt to get last version - no changeset found. Returning default version");
            return builder.apply(ConfigurationVersionSnapshot.empty());
        }

        var version = toScopedSnapshot(store.getVersions().get(store.maxChangesetId));
        if (version == null) {
            version = ConfigurationVersionSnapshot.empty();
        }
        return builder.apply(version);
    }

    @Override
    public Optional<Configuration> getVersion(@NotNull Long versionId) throws StoreNotLoadedException {
        ensureBlockInfoLoaded();
        if (!Boolean.TRUE.equals(this.blockExists)) {
            log.warn("Configuration information hasn't been loaded or block not exists");
            return Optional.empty();
        }

        var store = engine.store; assert store != null;
        var version = toScopedSnapshot(store.getVersions().get(versionId));
        if (version == null) {
            return Optional.empty();
        }
        return Optional.of(builder.apply(version));
    }
}
