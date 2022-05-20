package sharedconfig.core.exceptions;

import lombok.experimental.StandardException;

@StandardException
public class ApplicationException extends Exception {
    public ApplicationException() {
        this(null, null);
    }

    public ApplicationException(String message) {
        this(message, null);
    }

    public ApplicationException(Throwable cause) {
        this(cause != null ? cause.getMessage() : null, cause);
    }

    public ApplicationException(String message, Throwable cause) {
        super(message);
        if (cause != null)
            super.initCause(cause);
    }
}
