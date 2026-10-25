package clubflow.command;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Objects;

import clubflow.ClubData;
import clubflow.UserInterface;
import clubflow.event.Event;
import clubflow.exception.CommandParseException;
import clubflow.exception.EventNotFoundException;

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
        addSyntax("e/Tag [n/Name] [d/YYYY-MM-DD] [by/YYYY-MM-DD] [b/Budget] [v/Venue] [l/Logistics]");
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
    public boolean execute(HashMap<String, String> args) throws EventNotFoundException {
        Objects.requireNonNull(args, "Arguments must not be null");
        assert args.containsKey(EventArgumentIds.TAG) : "Validated arguments must contain an event tag";
        String tag = args.get(EventArgumentIds.TAG);
        Event original = clubData.findEventByTag(tag).orElseThrow(() -> new EventNotFoundException(tag));

        // Use supplied values where present, keeping the original values for omitted fields.
        String name = args.getOrDefault(EventArgumentIds.NAME, original.getName());
        LocalDate date = args.containsKey(EventArgumentIds.DATE)
                ? LocalDate.parse(args.get(EventArgumentIds.DATE)) : original.getDate();
        LocalDate deadline = args.containsKey(EventArgumentIds.DEADLINE)
                ? LocalDate.parse(args.get(EventArgumentIds.DEADLINE)) : original.getDeadline();
        double budget = args.containsKey(EventArgumentIds.BUDGET)
                ? Double.parseDouble(args.get(EventArgumentIds.BUDGET)) : original.getBudget();
        String venue = args.getOrDefault(EventArgumentIds.VENUE, original.getVenue());
        String logistics = args.getOrDefault(EventArgumentIds.LOGISTICS, original.getLogistics());

        // Build the replacement first so every supplied field is applied together.
        Event updated = new Event(original.getTag(), name, date, deadline,
                budget, venue, logistics, original.isCompleted());
        boolean updatedSuccessfully = clubData.updateEvent(updated);
        assert updatedSuccessfully : "The selected event must still exist when editing occurs";
        ui.print("Updated event: " + updated.getTag());
        return false;
    }
}
