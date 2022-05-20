package sharedconfig.core.interfaces;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sharedconfig.core.exceptions.StoreNotLoadedException;
import sharedconfig.utils.tuples.NameVersionTuple;

import java.util.Optional;
import java.util.SortedSet;

public interface IScopedConfigurationService<Configuration> {
    /**
     * Получить идентификатор скоупа
     */
    @NotNull NameVersionTuple getScopeId() throws StoreNotLoadedException;

    /**
     * Получить список актуальных загруженных версий
     */
    @NotNull SortedSet<Long> getVersionIds() throws StoreNotLoadedException;

    /**
     * Получить последнюю актуальную версию     *
     */
    @Nullable Long getLatestVersionId();

    /**
     * Получить обозреваемую конфигурационную версию
     */
    @NotNull ISharedConfigMonitor<Configuration> getObservedVersion();

    /**
     * Получить последнюю доступную версию конфигурации
     */
    Configuration getLastVersion() throws StoreNotLoadedException;

    /**
     * Получить конкретную версию конфигурации
     */
    Optional<Configuration> getVersion(@NotNull Long versionId) throws StoreNotLoadedException;
}
