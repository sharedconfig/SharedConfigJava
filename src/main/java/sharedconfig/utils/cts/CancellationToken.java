package sharedconfig.utils.cts;

import lombok.Getter;

import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ограниченная реализация CancellationToken из .NET
 */
public final class CancellationToken {
    private static final CancellationToken none = new CancellationToken();
    public static CancellationToken none() {
        return none;
    }

    private final AtomicBoolean cancellationRequested;

    /* package */ CancellationToken() {
        cancellationRequested = new AtomicBoolean(false);
    }

    /**
     * @return {@code true} if the cancellation was requested from the source, {@code false} otherwise.
     */
    public boolean isCancellationRequested() {
        return cancellationRequested.get();
    }

    /**
     * @throws CancellationException if this token has had cancellation requested.
     * May be used to stop execution of a thread or runnable.
     */
    public void throwIfCancellationRequested() throws CancellationException {
        if (cancellationRequested.get()) {
            throw new CancellationException();
        }
    }

    /* package */ boolean tryCancel() {
        if (cancellationRequested.get()) {
            return false;
        }

        cancellationRequested.set(true);
        return true;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%s@%s[cancellationRequested=%s]",
                getClass().getName(),
                Integer.toHexString(hashCode()),
                Boolean.toString(cancellationRequested.get()));
    }
}
