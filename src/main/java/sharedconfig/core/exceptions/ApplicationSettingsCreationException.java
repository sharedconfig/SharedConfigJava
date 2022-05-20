package sharedconfig.core.exceptions;

public class ApplicationSettingsCreationException extends Exception {
    public ApplicationSettingsCreationException() {
        this(null, null);
    }

    public ApplicationSettingsCreationException(String message) {
        this(message, null);
    }

    public ApplicationSettingsCreationException(Throwable cause) {
        this(cause != null ? cause.getMessage() : null, cause);
    }

    public ApplicationSettingsCreationException(String message, Throwable cause) {
        super(message);
        if (cause != null)
            super.initCause(cause);
    }
}
