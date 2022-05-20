package sharedconfig.core;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sharedconfig.core.exceptions.StoreNotLoadedException;
import sharedconfig.core.interfaces.IScopedConfigurationService;
import sharedconfig.core.interfaces.IConfigurationVersionSnapshot;
import sharedconfig.core.interfaces.ISharedConfigMonitor;
import sharedconfig.utils.tuples.NameVersionTuple;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

@AllArgsConstructor
@Log4j2
/* package */ class ApplicationScopedConfigurationService<Configuration>
        implements IScopedConfigurationService<Configuration> {
    private final @NotNull ConfigurationEngine engine;
    private final @NotNull NameVersionTuple applicationId;
    private final @NotNull Function<@NotNull IConfigurationVersionSnapshot, @NotNull Configuration> builder;

    @Override
    public @NotNull NameVersionTuple getScopeId() {
        return applicationId;
    }

    @Override
    public @NotNull SortedSet<Long> getVersionIds() {
        var store = engine.store;
        if (store == null) {
            log.warn("Unsuccessful attempt to get versions list - configuration information hasn't been loaded");
            return Collections.emptySortedSet();
        }

        return Collections.unmodifiableSortedSet(store.actualChangesetIds);
    }

    @Override
    public @Nullable Long getLatestVersionId() {
        var store = engine.store;
        if (store == null) {
            return null;
        }
        return store.getVersion();
    }

    @Override
    public @NotNull ISharedConfigMonitor<Configuration> getObservedVersion() {
        return new SharedConfigMonitor<>(this);
    }

    private ConfigurationVersionSnapshot toScopedSnapshot(@Nullable ConfigurationVersion confVersion) {
        if (confVersion == null)
            return null;

        var chagesetId = confVersion.getVersion();
        var allVersionVarValues = confVersion.getStoreItem().getVariables();
        var store = engine.store; assert store != null;
        var resultVars = new HashMap<String, String>();

        // отбираем переменные уровня application
        for (var appVariable : store.getApplicationInvDeclaration().getApplication().getVariables().values()) {
            var appVariableValue = allVersionVarValues.get(appVariable.getId());
            if (appVariableValue != null) {
                resultVars.put(appVariable.getName(), appVariableValue.getValue());
            }
        }

        // отбираем все переменные уровня блока без конфликта имен
        for (var block : store.getApplicationInvDeclaration().getBlocks().values()) {
            for (var blockVariable : block.getVariables().values()) {
                var blockVariableValue = allVersionVarValues.get(blockVariable.getId());
                if (blockVariableValue != null && !resultVars.containsKey(blockVariable.getName())) {
                    resultVars.put(blockVariable.getName(), blockVariableValue.getValue());
                }
            }
        }

        return new ConfigurationVersionSnapshot(chagesetId, Collections.unmodifiableMap(resultVars));
    }


    @Override
    public Configuration getLastVersion() throws StoreNotLoadedException {
        var store = engine.store;
        if (store == null) {
            log.warn("Unsuccessful attempt to get last version - configuration information hasn't been loaded. Returning default");
            return builder.apply(ConfigurationVersionSnapshot.empty());
        }

        if (store.maxChangesetId == null) {
            log.warn("Unsuccessful attempt to get last version - no changeset found. Returning default");
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
        var store = engine.store;
        if (store == null) {
            log.warn("Unsuccessful attempt to get '{}' version - configuration information hasn't been loaded", versionId);
            return Optional.empty();
        }

        var version = toScopedSnapshot(store.getVersions().get(versionId));
        if (version == null) {
            return Optional.empty();
        }
        return Optional.of(builder.apply(version));
    }
}
