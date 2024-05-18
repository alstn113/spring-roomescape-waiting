package roomescape.exception;

public class AccessDeniedException extends RuntimeException {

    private static final String MESSAGE = "권한이 없습니다.";

    public AccessDeniedException(String message) {
        super(message);
    }
}
