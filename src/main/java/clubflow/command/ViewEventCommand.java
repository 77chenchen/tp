package clubflow.command;

import java.util.HashMap;

import clubflow.ClubData;
import clubflow.UserInterface;
import clubflow.event.Event;
import clubflow.exception.CommandParseException;

/**
 * Displays one event selected by tag or all events.
 */
public class ViewEventCommand extends Command {
    /** Shared store queried by this command. */
    private final ClubData clubData;

    /**
     * Creates a command for viewing events.
     *
     * @param ui user interface used to display event information
     * @param clubData shared club data containing the events to display
     */
    public ViewEventCommand(UserInterface ui, ClubData clubData) {
        super(ui);
        this.clubData = clubData;
    }

    @Override
    public String[] requiredArgIds() {
        return new String[]{};
    }

    @Override
    public String[] optionalArgIds() {
        return new String[]{EventArgumentIds.TAG};
    }

    @Override
    public String[] flagArgIds() {
        return new String[]{EventArgumentIds.ALL};
    }

    @Override
    public void validateArgs(HashMap<String, String> args) throws CommandParseException {
        boolean hasEventTag = args.containsKey(EventArgumentIds.TAG);
        boolean requestsAll = args.containsKey(EventArgumentIds.ALL);

        // Equal values mean that either neither option or both options were supplied.
        if (hasEventTag == requestsAll) {
            throw new CommandParseException("Specify either e/EVENT_TAG or all, but not both.");
        }

        if (hasEventTag && args.get(EventArgumentIds.TAG).isBlank()) {
            throw new CommandParseException("An event tag must be provided after e/.");
        }
    }

    @Override
    public boolean execute(HashMap<String, String> args) {
        if (args.containsKey(EventArgumentIds.ALL)) {
            ui.print("Displaying all events.");
            for (Event event : clubData.getEvents()) {
                ui.print(event.getTag() + ": " + event.getName());
            }
        } else {
            clubData.findEventByTag(args.get(EventArgumentIds.TAG))
                    .ifPresentOrElse(
                            event -> ui.print(event.getTag() + ": " + event.getName()),
                            () -> ui.print("Event not found.")
                );
        }

        return false;
    }
}
