package clubflow.command;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
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
        addSyntax("[e/Keyword] [q/Keyword] [n/Name] [v/Venue] [l/Logistics] [d/Date] [by/Deadline]");
    }

    @Override
    public String[] requiredArgIds() {
        return new String[]{};
    }

    @Override
    public String[] optionalArgIds() {
        return new String[]{EventArgumentIds.TAG, QUERY, EventArgumentIds.NAME, EventArgumentIds.VENUE,
            EventArgumentIds.LOGISTICS, EventArgumentIds.DATE, EventArgumentIds.DEADLINE};
    }

    @Override
    public void validateArgs(HashMap<String, String> args) throws CommandParseException {
        Objects.requireNonNull(args, "Arguments must not be null");
        if (args.isEmpty()) {
            throw new CommandParseException("Provide at least one search filter: e/, q/, n/, v/, l/, d/, or by/.");
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
