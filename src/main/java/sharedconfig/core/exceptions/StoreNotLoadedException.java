package sharedconfig.core.exceptions;

public class StoreNotLoadedException extends Exception {
    public StoreNotLoadedException() {
        this(null, null);
    }

    public StoreNotLoadedException(String message) {
        this(message, null);
    }

    public StoreNotLoadedException(Throwable cause) {
        this(cause != null ? cause.getMessage() : null, cause);
    }

    public StoreNotLoadedException(String message, Throwable cause) {
        super(message);
        if (cause != null)
            super.initCause(cause);
    }
}
