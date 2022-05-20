package sharedconfig.core.exceptions;

public class ConfigurationEngineCreationException extends Exception {
    public ConfigurationEngineCreationException() {
        this(null, null);
    }

    public ConfigurationEngineCreationException(String message) {
        this(message, null);
    }

    public ConfigurationEngineCreationException(Throwable cause) {
        this(cause != null ? cause.getMessage() : null, cause);
    }

    public ConfigurationEngineCreationException(String message, Throwable cause) {
        super(message);
        if (cause != null)
            super.initCause(cause);
    }
}
