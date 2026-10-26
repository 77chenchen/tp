package clubflow.command;

import java.util.HashMap;
import java.util.Objects;

import clubflow.Club;
import clubflow.UserInterface;
import clubflow.exception.CommandParseException;
import clubflow.exception.EventNotFoundException;

/**
 * Deletes an event selected by its tag.
 */
public class DeleteEventCommand extends Command {
    /** Shared store from which this command removes events. */
    private final Club club;

    /**
     * Creates a command that deletes events from the supplied data store.
     *
     * @param ui user interface used to display command results
     * @param club shared club data from which events are deleted
     */
    public DeleteEventCommand(UserInterface ui, Club club) {
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
        if (args.get(EventArgumentIds.TAG).isBlank()) {
            throw new CommandParseException("An event tag must be provided after e/.");
        }
    }

    @Override
    public boolean execute(HashMap<String, String> args) throws EventNotFoundException {
        Objects.requireNonNull(args, "Arguments must not be null");
        assert args.containsKey(EventArgumentIds.TAG) : "Validated arguments must contain an event tag";
        String tag = args.get(EventArgumentIds.TAG);

        if (!club.removeEventByTag(tag)) {
            throw new EventNotFoundException(tag);
        }
        ui.print("Deleted event: " + tag);

        return false;
    }
}
