package clubflow.command;

import java.util.HashMap;

import clubflow.ClubData;
import clubflow.UserInterface;
import clubflow.exception.CommandParseException;

/**
 * Deletes an event selected by its tag.
 */
public class DeleteEventCommand extends Command {
    /** Shared store from which this command removes events. */
    private final ClubData clubData;

    /**
     * Creates a command that deletes events from the supplied data store.
     *
     * @param ui user interface used to display command results
     * @param clubData shared club data from which events are deleted
     */
    public DeleteEventCommand(UserInterface ui, ClubData clubData) {
        super(ui);
        this.clubData = clubData;
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
    public boolean execute(HashMap<String, String> args) {
        String tag = args.get(EventArgumentIds.TAG);

        if (clubData.removeEventByTag(tag)) {
            ui.print("Deleted event: " + tag);
        } else {
            ui.print("Event not found: " + tag);
        }

        return false;
    }
}
