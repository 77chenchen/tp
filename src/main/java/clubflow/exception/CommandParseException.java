package clubflow.exception;

public class CommandParseException extends ClubFlowException {
    public CommandParseException(String description) {
        super("Command has incorrect syntax", description);
    }
}
