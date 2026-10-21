package clubflow.command;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

import clubflow.ClubData;
import clubflow.UserInterface;
import clubflow.event.Event;
import clubflow.exception.CommandParseException;

/**
 * Updates supplied details of an event selected by its tag, preserving omitted fields.
 */
public class EditEventCommand extends Command {
    /** Shared store containing the events that can be edited. */
    private final ClubData clubData;

    /**
     * Creates a command for editing existing events.
     *
     * @param ui user interface used to display the result
     * @param clubData shared club data containing the events to edit
     */
    public EditEventCommand(UserInterface ui, ClubData clubData) {
        super(ui);
        this.clubData = Objects.requireNonNull(clubData, "Club data must not be null");
    }

    @Override
    public String[] requiredArgIds() {
        return new String[]{EventArgumentIds.TAG};
    }

    @Override
    public String[] optionalArgIds() {
        return new String[]{
            EventArgumentIds.NAME,
            EventArgumentIds.DATE,
            EventArgumentIds.DEADLINE,
            EventArgumentIds.BUDGET,
            EventArgumentIds.VENUE,
            EventArgumentIds.LOGISTICS
        };
    }

    @Override
    public void validateArgs(HashMap<String, String> args) throws CommandParseException {
        EventArgumentValidator.validate(args);
        if (args.size() == 1) {
            throw new CommandParseException("Provide at least one event detail to edit: n/, d/, by/, b/, v/, or l/.");
        }
    }

    @Override
    public boolean execute(HashMap<String, String> args) {
        Objects.requireNonNull(args, "Arguments must not be null");
        assert args.containsKey(EventArgumentIds.TAG) : "Validated arguments must contain an event tag";
        Optional<Event> existingEvent = clubData.findEventByTag(args.get(EventArgumentIds.TAG));
        if (existingEvent.isEmpty()) {
            ui.print("Event not found: " + args.get(EventArgumentIds.TAG));
            return false;
        }

        Event original = existingEvent.get();
        // Build the replacement first so every supplied field is applied together.
        Event updated = new Event(
                original.getTag(),
                args.getOrDefault(EventArgumentIds.NAME, original.getName()),
                args.containsKey(EventArgumentIds.DATE)
                        ? LocalDate.parse(args.get(EventArgumentIds.DATE)) : original.getDate(),
                args.containsKey(EventArgumentIds.DEADLINE)
                        ? LocalDate.parse(args.get(EventArgumentIds.DEADLINE)) : original.getDeadline(),
                args.containsKey(EventArgumentIds.BUDGET)
                        ? Double.parseDouble(args.get(EventArgumentIds.BUDGET)) : original.getBudget(),
                args.getOrDefault(EventArgumentIds.VENUE, original.getVenue()),
                args.getOrDefault(EventArgumentIds.LOGISTICS, original.getLogistics())
        );
        boolean updatedSuccessfully = clubData.updateEvent(updated);
        assert updatedSuccessfully : "The selected event must still exist when editing occurs";
        ui.print("Updated event: " + updated.getTag());
        return false;
    }
}
