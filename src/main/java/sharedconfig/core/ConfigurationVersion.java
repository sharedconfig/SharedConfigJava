package sharedconfig.core;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Класс хранящий полную информации о конфигурационном changeset'e
 */
/* package */ class ConfigurationVersion implements Comparator<ConfigurationVersion>, Comparable<ConfigurationVersion> {
    /** Сырые конфигурационные данные (из prepared.vars.xml) */
    @Getter
    private final @NotNull ConfigurationStoreItem storeItem;

    /** Информация о версии */
    @Getter
    private @NotNull Long version;

    /** Помечен ли changeset на удаление */
    @Getter @Setter
    private boolean isDeprecated;

    public ConfigurationVersion(@NotNull ConfigurationStoreItem storeItem, @NotNull Long version) {
        this.storeItem = storeItem;
        this.version = version;
        this.isDeprecated = false;
    }

    @Override
    public int compare(ConfigurationVersion x, ConfigurationVersion y) {
        if (x == y) return 0;
        if (null == y) return 1;
        if (null == x) return -1;

        if (y.isDeprecated && !x.isDeprecated) {
            return -1;
        }
        if (x.isDeprecated && !y.isDeprecated) {
            return 1;
        }
        return Long.compare(x.version, y.version);
    }

    @Override
    public int compareTo(@NotNull ConfigurationVersion other) {
        return compare(this, other);
    }

}
