package clubflow.command.event;

import clubflow.command.Command;
import clubflow.command.EventArgumentIds;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import clubflow.Club;
import clubflow.UserInterface;
import clubflow.exception.CommandParseException;
import clubflow.search.EventSearch;

//@@author Eugene3129
/**
 * Validates optional event search filters and displays matching event summaries.
 */
public class FindEventCommand extends Command {
    private static final String QUERY = "q";
    private static final String MIN_BUDGET = "min";
    private static final String MAX_BUDGET = "max";
    private static final String STATUS = "s";
    /** Searches the shared club so results reflect its current events. */
    private final EventSearch eventSearch;

    /**
     * Creates an event search command for the shared club.
     *
     * @param ui user interface used to display matches
     * @param club club containing the events to search
     */
    public FindEventCommand(UserInterface ui, Club club) {
        super(ui);
        this.eventSearch = new EventSearch(ui, club);
        addSyntax("[e/Keyword] [q/Keyword] [n/Name] [v/Venue] [l/Logistics] [d/Date] [by/Deadline]"
                + " [b/Budget] [min/Budget] [max/Budget] [s/completed|pending]");
    }

    @Override
    public String[] requiredArgIds() {
        return new String[]{};
    }

    @Override
    public String[] optionalArgIds() {
        return new String[]{EventArgumentIds.TAG, QUERY, EventArgumentIds.NAME, EventArgumentIds.VENUE,
            EventArgumentIds.LOGISTICS, EventArgumentIds.DATE, EventArgumentIds.DEADLINE,
            EventArgumentIds.BUDGET, MIN_BUDGET, MAX_BUDGET, STATUS};
    }

    @Override
    public void validateArgs(HashMap<String, String> args) throws CommandParseException {
        Objects.requireNonNull(args, "Arguments must not be null");
        if (args.isEmpty()) {
            throw new CommandParseException("Provide at least one search filter: "
                    + "e/, q/, n/, v/, l/, d/, by/, b/, min/, max/, or s/.");
        }
        Set<String> supportedIds = Set.of(optionalArgIds());
        for (var filter : args.entrySet()) {
            String id = Objects.requireNonNull(filter.getKey(), "Argument ID must not be null");
            String value = Objects.requireNonNull(filter.getValue(), "Search value must not be null");
            if (!supportedIds.contains(id)) {
                throw new CommandParseException("Unsupported search filter: " + id + "/.");
            }
            if (value.isBlank()) {
                throw new CommandParseException("Provide a non-blank search value after " + id + "/.");
            }
        }
        validateDate(args, EventArgumentIds.DATE);
        validateDate(args, EventArgumentIds.DEADLINE);
        validateBudgets(args);
        if (args.containsKey(STATUS)) {
            String status = args.get(STATUS).strip().toLowerCase(Locale.ROOT);
            if (!status.equals("completed") && !status.equals("pending")) {
                throw new CommandParseException("Search status must be completed or pending after s/.");
            }
        }
    }

    /** Rejects impossible budget ranges and exact budgets that conflict with supplied bounds. */
    private void validateBudgets(HashMap<String, String> args) throws CommandParseException {
        double minimum = readBudget(args, MIN_BUDGET, 0);
        double maximum = readBudget(args, MAX_BUDGET, Double.MAX_VALUE);
        double exact = readBudget(args, EventArgumentIds.BUDGET, 0);
        if (minimum > maximum) {
            throw new CommandParseException("Minimum budget must not exceed maximum budget.");
        }
        if (args.containsKey(EventArgumentIds.BUDGET) && (exact < minimum || exact > maximum)) {
            throw new CommandParseException("Exact budget must be within the supplied budget bounds.");
        }
    }

    /** Reads a finite non-negative budget, or the default bound when the filter was omitted. */
    private double readBudget(HashMap<String, String> args, String id, double defaultValue)
            throws CommandParseException {
        if (!args.containsKey(id)) {
            return defaultValue;
        }
        try {
            double budget = Double.parseDouble(args.get(id).strip());
            if (Double.isFinite(budget) && budget >= 0) {
                return budget;
            }
        } catch (NumberFormatException e) {
            // Fall through to the shared validation error below for malformed and out-of-range numbers.
        }
        throw new CommandParseException("Search budget must be a finite non-negative number after " + id + "/.");
    }

    /** Validates a supplied date filter without requiring an event tag or another filter. */
    private void validateDate(HashMap<String, String> args, String id) throws CommandParseException {
        if (!args.containsKey(id)) {
            return;
        }
        try {
            LocalDate.parse(args.get(id).strip());
        } catch (DateTimeParseException e) {
            throw new CommandParseException("Search dates must use YYYY-MM-DD format after " + id + "/.");
        }
    }

    @Override
    public boolean execute(HashMap<String, String> args) throws CommandParseException {
        // Validate direct calls too, so an empty or null request cannot search every event.
        validateArgs(args);
        eventSearch.search(args);
        return false;
    }
}
