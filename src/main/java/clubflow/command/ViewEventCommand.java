package clubflow.command;

import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

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
        this.clubData = Objects.requireNonNull(clubData, "Club data must not be null");
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
        Objects.requireNonNull(args, "Arguments must not be null");
        assert args.containsKey(EventArgumentIds.ALL) ^ args.containsKey(EventArgumentIds.TAG)
                : "Validated arguments must select one view mode";
        if (args.containsKey(EventArgumentIds.ALL)) {
            if (clubData.getEvents().isEmpty()) {
                ui.print("No events found.");
                return false;
            }

            ui.print("Displaying all events.");
            for (Event event : clubData.getEvents()) {
                printEvent(event);
            }
        } else {
            clubData.findEventByTag(args.get(EventArgumentIds.TAG))
                    .ifPresentOrElse(this::printEvent, () -> ui.print("Event not found."));
        }

        return false;
    }

    /**
     * Prints an event with labelled details and placeholders for missing values.
     *
     * @param event event to display
     */
    private void printEvent(Event event) {
        ui.print("[" + event.getTag() + "] " + displayText(event.getName()));
        ui.print("  Status:    " + (event.isCompleted() ? "Completed" : "Not completed"));
        ui.print("  Date:      "
                + (event.getDate() == null ? "Not set" : event.getDate()));
        ui.print("  Deadline:  "
                + (event.getDeadline() == null ? "Not set" : event.getDeadline()));
        ui.print(String.format(Locale.ROOT, "  Budget:    $%.2f", event.getBudget()));
        ui.print("  Venue:     " + displayText(event.getVenue()));
        ui.print("  Logistics: " + displayText(event.getLogistics()));
    }

    /**
     * Replaces an unspecified text field with a readable placeholder.
     *
     * @param value non-null event field
     * @return field value or a placeholder
     */
    private String displayText(String value) {
        return value.isBlank() ? "Not set" : value;
    }
}
