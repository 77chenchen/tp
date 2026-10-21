package clubflow.exception;

/**
 * Reports that event creation would reuse a tag already stored by the application.
 */
public class DuplicateEventException extends ClubFlowException {
    /**
     * Creates an error identifying the tag supplied for the duplicate event.
     *
     * @param tag tag that is already in use, compared case-insensitively
     */
    public DuplicateEventException(String tag) {
        super("Event already exists", tag);
    }
}
