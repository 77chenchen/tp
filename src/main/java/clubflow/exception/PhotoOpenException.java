package clubflow.exception;

public class PhotoOpenException extends ClubFlowException{
    /**
     * Creates a photo open exception.
     */
    public PhotoOpenException(String description) {
        super("Photo opening failed", description);
    }
}
