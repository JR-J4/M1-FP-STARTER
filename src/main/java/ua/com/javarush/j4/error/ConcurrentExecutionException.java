package ua.com.javarush.j4.error;

/** Thrown when a task submitted to a {@code TaskExecutor} fails. */
public class ConcurrentExecutionException extends CryptanalysisException {
    public ConcurrentExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
