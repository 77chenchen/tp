package clubflow.exception;

public class PhotoRenameException extends ClubFlowException{
    /**
     * Creates a photo rename exception.
     */
    public PhotoRenameException(String description) {
        super("Photo rename failed", description);
    }
}
