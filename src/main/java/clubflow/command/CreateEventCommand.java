package clubflow.command;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;

import clubflow.ClubData;
import clubflow.UserInterface;
import clubflow.event.Event;
import clubflow.exception.CommandParseException;

/**
 * Creates an event and stores it in the shared club data.
 */
public class CreateEventCommand extends Command {
    /** Shared store used by all event commands in the current ClubFlow session. */
    private final ClubData clubData;

    /**
     * Creates a command that adds events to the supplied data store.
     *
     * @param ui user interface used to display command results
     * @param clubData shared club data in which events are stored
     */
    public CreateEventCommand(UserInterface ui, ClubData clubData) {
        super(ui);
        this.clubData = clubData;
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
        if (args.get(EventArgumentIds.TAG).isBlank()) {
            throw new CommandParseException("An event tag must be provided after e/.");
        }

        // Validate formats here so malformed user input becomes a friendly command error.
        validateDate(args, EventArgumentIds.DATE, "Event date");
        validateDate(args, EventArgumentIds.DEADLINE, "Deadline");
        validateBudget(args);
    }

    @Override
    public boolean execute(HashMap<String, String> args) {
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

        // ClubData performs the case-insensitive duplicate-tag check.
        if (clubData.addEvent(event)) {
            ui.print("Created event: " + event.getTag());
        } else {
            ui.print("Event already exists: " + event.getTag());
        }

        return false;
    }

    /**
     * Validates an optional ISO-8601 date argument.
     *
     * @param args parsed command arguments
     * @param argId ID of the date argument
     * @param description user-facing name of the date
     * @throws CommandParseException if the date is not in YYYY-MM-DD format
     */
    private void validateDate(HashMap<String, String> args, String argId, String description)
            throws CommandParseException {
        if (!args.containsKey(argId)) {
            return;
        }

        try {
            LocalDate.parse(args.get(argId));
        } catch (DateTimeParseException e) {
            throw new CommandParseException(description + " must use YYYY-MM-DD format.");
        }
    }

    /**
     * Validates the optional event budget.
     *
     * @param args parsed command arguments
     * @throws CommandParseException if the budget is not a finite, non-negative number
     */
    private void validateBudget(HashMap<String, String> args) throws CommandParseException {
        if (!args.containsKey(EventArgumentIds.BUDGET)) {
            return;
        }

        try {
            double budget = Double.parseDouble(args.get(EventArgumentIds.BUDGET));
            if (!Double.isFinite(budget) || budget < 0) {
                throw new CommandParseException("Budget must be a non-negative number.");
            }
        } catch (NumberFormatException e) {
            throw new CommandParseException("Budget must be a non-negative number.");
        }
    }
}
