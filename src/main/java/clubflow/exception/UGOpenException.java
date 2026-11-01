package clubflow.exception;

public class UGOpenException extends ClubFlowException{
    /**
     * Creates a user guide open exception.
     */
    public UGOpenException(String description) {
        super("Failed to open user guide on browser", description);
    }
}
