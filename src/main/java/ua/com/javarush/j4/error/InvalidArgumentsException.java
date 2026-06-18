package ua.com.javarush.j4.error;

/** Thrown when CLI arguments or configuration are invalid. */
public class InvalidArgumentsException extends CryptanalysisException {
    public InvalidArgumentsException(String message) {
        super(message);
    }
}
