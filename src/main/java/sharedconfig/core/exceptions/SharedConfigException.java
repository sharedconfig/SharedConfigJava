package sharedconfig.core.exceptions;

import lombok.experimental.StandardException;

@StandardException
public class SharedConfigException extends Exception {
    public SharedConfigException() {
        this(null, null);
    }

    public SharedConfigException(String message) {
        this(message, null);
    }

    public SharedConfigException(Throwable cause) {
        this(cause != null ? cause.getMessage() : null, cause);
    }

    public SharedConfigException(String message, Throwable cause) {
        super(message);
        if (cause != null)
            super.initCause(cause);
    }
}
