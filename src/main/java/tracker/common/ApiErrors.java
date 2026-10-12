package tracker.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Turns errors thrown by the API into {@code {"error": "..."}} so the page can show the message.
 */
@RestControllerAdvice
public class ApiErrors {

    /**
     * The body of an error response.
     *
     * @param error a message the page can show to the user
     */
    public record ApiError(String error) {
    }

    /**
     * Answers a {@link ResponseStatusException} with its status code and reason.
     *
     * @param e the exception a controller or service threw
     * @return a response with the exception's status and its reason as the error message
     */
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiError> statusError(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(new ApiError(e.getReason()));
    }
}
