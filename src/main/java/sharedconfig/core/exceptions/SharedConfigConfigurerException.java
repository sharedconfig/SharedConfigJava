package sharedconfig.core.exceptions;

import lombok.experimental.StandardException;

@StandardException
public class SharedConfigConfigurerException extends Exception {
    public SharedConfigConfigurerException() {
        this(null, null);
    }

    public SharedConfigConfigurerException(String message) {
        this(message, null);
    }

    public SharedConfigConfigurerException(Throwable cause) {
        this(cause != null ? cause.getMessage() : null, cause);
    }

    public SharedConfigConfigurerException(String message, Throwable cause) {
        super(message);
        if (cause != null)
            super.initCause(cause);
    }
}
