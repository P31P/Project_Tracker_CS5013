package tracker.llm;

/**
 * The language model could not be asked or gave no usable answer: it is turned off, unreachable, too slow, or
 * sent something that isn't a chat reply.
 */
public class LlmException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message what went wrong
     */
    public LlmException(String message) {
        super(message);
    }

    /**
     * Creates the exception with its cause.
     *
     * @param message what went wrong
     * @param cause the error that caused it
     */
    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
