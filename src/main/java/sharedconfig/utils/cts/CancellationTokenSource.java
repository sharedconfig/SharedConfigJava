package sharedconfig.utils.cts;

import java.util.Locale;

public class CancellationTokenSource {
    private final CancellationToken token;

    /**
     * Create a new {@code CancellationTokenSource}.
     */
    public CancellationTokenSource() {
        token = new CancellationToken();
    }

    /**
     * @return {@code true} if cancellation has been requested for this {@code CancellationTokenSource}.
     */
    public boolean isCancellationRequested() {
        return token.isCancellationRequested();
    }

    /**
     * @return the token that can be passed to asynchronous method to control cancellation.
     */
    public CancellationToken getToken() {
        return token;
    }

    /**
     * Cancels the token if it has not already been cancelled.
     */
    public void cancel() {
        token.tryCancel();
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%s@%s[cancellationRequested=%s]",
                getClass().getName(),
                Integer.toHexString(hashCode()),
                Boolean.toString(isCancellationRequested()));
    }
}
