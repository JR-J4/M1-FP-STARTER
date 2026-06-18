package ua.com.javarush.j4.error;

/** Thrown when the input file cannot be read. */
public class UnreadableSourceException extends CryptanalysisException {
    public UnreadableSourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
