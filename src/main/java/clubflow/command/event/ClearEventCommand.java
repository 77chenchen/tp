package clubflow.command.event;

import clubflow.Club;
import clubflow.UserInterface;
import clubflow.command.Command;
import clubflow.command.EventArgumentIds;
import clubflow.event.Event;
import clubflow.exception.CommandParseException;
import clubflow.exception.EventNotFoundException;

import java.util.HashMap;
import java.util.Objects;

/**
 * Clears an event's editable details and resets it to pending, retaining its identity, photos, and assignments.
 */
public class ClearEventCommand extends Command {
    /** Shared store containing the event to clear. */
    private final Club club;

    /**
     * Creates a command that resets event details to their draft defaults.
     *
     * @param ui user interface used to display the result
     * @param club shared club data containing the event
     */
    public ClearEventCommand(UserInterface ui, Club club) {
        super(ui);
        this.club = Objects.requireNonNull(club, "Club data must not be null");
        addSyntax("e/Tag");
    }

    @Override
    public String[] requiredArgIds() {
        return new String[]{EventArgumentIds.TAG};
    }

    @Override
    public String[] optionalArgIds() {
        return new String[]{};
    }

    @Override
    public void validateArgs(HashMap<String, String> args) throws CommandParseException {
        EventArgumentValidator.validate(args);
    }

    @Override
    public boolean execute(HashMap<String, String> args) throws EventNotFoundException {
        Objects.requireNonNull(args, "Arguments must not be null");
        assert args.containsKey(EventArgumentIds.TAG) : "Validated arguments must contain an event tag";
        String tag = args.get(EventArgumentIds.TAG);
        Event original = club.findEventByTag(tag).orElseThrow(() -> new EventNotFoundException(tag));
        Event cleared = new Event(original.getTag(), "", null, null, 0, "", "", false);

        // Updating through the club preserves photos, assignments, and list position, and triggers autosave.
        boolean updatedSuccessfully = club.updateEvent(cleared);
        assert updatedSuccessfully : "The selected event must still exist when clearing occurs";
        ui.print("Cleared event details: " + cleared.getTag());
        return false;
    }
}
