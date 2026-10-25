package clubflow.exception;

public class PhotoDeleteException extends ClubFlowException{
    /**
     * Creates a photo delete exception.
     */
    public PhotoDeleteException(String description) {
        super("Photo deleting failed", description);
    }
}
