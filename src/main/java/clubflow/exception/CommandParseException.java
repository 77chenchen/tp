package clubflow.exception;

/**
 * Represents an exception thrown when the command text cannot be parsed into a command object.
 */
public class CommandParseException extends ClubFlowException {
    public CommandParseException(String description) {
        super("Command has incorrect syntax", description);
    }
}
