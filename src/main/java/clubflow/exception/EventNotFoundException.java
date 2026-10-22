package clubflow.exception;

/**
 * Reports that a command refers to an event tag that is not currently stored.
 */
public class EventNotFoundException extends ClubFlowException {
    /**
     * Creates an error identifying the requested event tag.
     *
     * @param tag tag that could not be found
     */
    public EventNotFoundException(String tag) {
        super("Event not found", tag);
    }
}
