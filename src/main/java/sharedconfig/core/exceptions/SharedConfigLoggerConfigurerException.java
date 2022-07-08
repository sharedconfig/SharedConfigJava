package sharedconfig.core.exceptions;

import lombok.experimental.StandardException;

@StandardException
public class SharedConfigLoggerConfigurerException extends Exception {
    public SharedConfigLoggerConfigurerException() {
        this(null, null);
    }

    public SharedConfigLoggerConfigurerException(String message) {
        this(message, null);
    }

    public SharedConfigLoggerConfigurerException(Throwable cause) {
        this(cause != null ? cause.getMessage() : null, cause);
    }

    public SharedConfigLoggerConfigurerException(String message, Throwable cause) {
        super(message);
        if (cause != null)
            super.initCause(cause);
    }
}
