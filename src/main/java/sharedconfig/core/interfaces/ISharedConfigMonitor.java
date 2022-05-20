package sharedconfig.core.interfaces;

import org.jetbrains.annotations.NotNull;

public interface ISharedConfigMonitor<T> {
    @NotNull T latest();
}
