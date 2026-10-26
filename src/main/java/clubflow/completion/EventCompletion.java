package clubflow.completion;

import java.util.Objects;

import clubflow.Club;
import clubflow.UserInterface;
import clubflow.event.Event;
import clubflow.exception.EventNotFoundException;

/**
 * Handles event completion changes without changing event details or list positions.
 */
public class EventCompletion {
    /** Interface used to display completion results. */
    private final UserInterface ui;

    /** Shared store queried on each request so handlers work after edits and loads. */
    private final Club club;

    /**
     * Creates the event handler shared by mark and unmark commands.
     *
     * @param ui user interface used to display the result
     * @param club shared club data containing the events to update
     */
    public EventCompletion(UserInterface ui, Club club) {
        this.ui = Objects.requireNonNull(ui, "User interface must not be null");
        this.club = Objects.requireNonNull(club, "Club data must not be null");
    }

    /**
     * Sets an event's completion state using a case-insensitive tag lookup.
     * Repeating mark or unmark leaves the event in the requested state.
     *
     * @param tag tag identifying the event
     * @param completed desired completion state
     * @throws EventNotFoundException if the requested event does not exist
     */
    public void setCompleted(String tag, boolean completed) throws EventNotFoundException {
        Event event = club.findEventByTag(tag).orElseThrow(() -> new EventNotFoundException(tag));
        if (event.isCompleted() != completed) {
            boolean updated = club.updateEvent(event.withCompleted(completed));
            assert updated : "The selected event must still exist when marking occurs";
        }
        ui.print("Marked event as " + (completed ? "completed: " : "not completed: ") + event.getTag());
    }
}
