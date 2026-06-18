package ua.com.javarush.j4.error;

/** Base type for all expected, cleanly-reported failures in the cryptanalyzer. */
public class CryptanalysisException extends RuntimeException {
    public CryptanalysisException(String message) {
        super(message);
    }

    public CryptanalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
