package clubflow.exception;

public class PhotoLoadException extends ClubFlowException{
    /**
     * Creates a photo load exception.
     *
     * @param description A detail description of what happened.
     */
    public PhotoLoadException(String description) {
        super("Photo loading failed", description);
    }
}
