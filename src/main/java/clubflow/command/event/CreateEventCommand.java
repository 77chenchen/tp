package clubflow.command.event;

import clubflow.command.Command;
import clubflow.command.EventArgumentIds;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Objects;

import clubflow.Club;
import clubflow.UserInterface;
import clubflow.event.Event;
import clubflow.exception.CommandParseException;
import clubflow.exception.DuplicateEventException;

//@@author Eugene3129
/**
 * Creates an event and stores it in the shared club data.
 */
public class CreateEventCommand extends Command {
    /** Shared store used by all event commands in the current ClubFlow session. */
    private final Club club;

    /**
     * Creates a command that adds events to the supplied data store.
     *
     * @param ui user interface used to display command results
     * @param club shared club data in which events are stored
     */
    public CreateEventCommand(UserInterface ui, Club club) {
        super(ui);
        this.club = Objects.requireNonNull(club, "Club data must not be null");
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

    /**
     * Validates event details and rejects the field separator used by the save file format.
     *
     * @param args parsed event arguments
     * @throws CommandParseException if a required value, format, or separator is invalid
     */
    @Override
    public void validateArgs(HashMap<String, String> args) throws CommandParseException {
        EventArgumentValidator.validate(args);
    }

    @Override
    public boolean execute(HashMap<String, String> args) throws DuplicateEventException {
        Objects.requireNonNull(args, "Arguments must not be null");
        assert args.containsKey(EventArgumentIds.TAG) : "Validated arguments must contain an event tag";
        // Optional dates use null when unspecified; an omitted budget defaults to zero.
        LocalDate date = args.containsKey(EventArgumentIds.DATE)
                ? LocalDate.parse(args.get(EventArgumentIds.DATE))
                : null;

        LocalDate deadline = args.containsKey(EventArgumentIds.DEADLINE)
                ? LocalDate.parse(args.get(EventArgumentIds.DEADLINE))
                : null;

        double budget = args.containsKey(EventArgumentIds.BUDGET)
                ? Double.parseDouble(args.get(EventArgumentIds.BUDGET))
                : 0.0;

        Event event = new Event(
                args.get(EventArgumentIds.TAG),
                args.getOrDefault(EventArgumentIds.NAME, ""),
                date,
                deadline,
                budget,
                args.getOrDefault(EventArgumentIds.VENUE, ""),
                args.getOrDefault(EventArgumentIds.LOGISTICS, "")
        );

        // Club performs the case-insensitive duplicate-tag check.
        if (!club.addEvent(event)) {
            throw new DuplicateEventException(event.getTag());
        }
        ui.print("Created event: " + event.getTag());

        return false;
    }
}
