package io.konveyor.demo.inheritance;

/**
 * Custom exception extending Exception - for testing inheritance of exceptions.
 */
public class CustomException extends Exception {

    private static final long serialVersionUID = 1L;

    public CustomException() {
        super();
    }

    public CustomException(String message) {
        super(message);
    }

    public CustomException(String message, Throwable cause) {
        super(message, cause);
    }
}
