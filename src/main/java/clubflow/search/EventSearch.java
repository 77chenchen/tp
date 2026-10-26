package clubflow.search;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import clubflow.Club;
import clubflow.UserInterface;
import clubflow.event.Event;

/**
 * Filters current events by text, dates, budgets, and completion status and displays matching summaries.
 */
public class EventSearch {
    /** Interface used to display search results. */
    private final UserInterface ui;

    /** Shared store queried on each search so results reflect the current events. */
    private final Club club;

    /**
     * Creates an event search handler.
     *
     * @param ui user interface used to display matches
     * @param club shared club data containing the events to search
     */
    public EventSearch(UserInterface ui, Club club) {
        this.ui = Objects.requireNonNull(ui, "User interface must not be null");
        this.club = Objects.requireNonNull(club, "Club data must not be null");
    }

    //@@author Eugene3129
    /**
     * Finds events satisfying every supplied filter, preserving store order.
     * Text filters use case-insensitive substrings; date filters use exact dates.
     * Budget bounds are inclusive, and status reflects whether an event is marked completed.
     *
     * @param filters non-empty filters validated by the event search command; keys and values must not be null
     */
    public void search(Map<String, String> filters) {
        Map<String, String> checkedFilters = Map.copyOf(Objects.requireNonNull(filters, "Filters must not be null"));
        if (checkedFilters.isEmpty()) {
            throw new IllegalArgumentException("At least one search filter is required");
        }
        // Compile filters once, including parsing dates, rather than once per event.
        List<Predicate<Event>> predicates = checkedFilters.entrySet().stream()
                .map(filter -> createFilter(filter.getKey(), filter.getValue()))
                .toList();
        List<Event> matches = club.getEvents().stream()
                .filter(event -> predicates.stream().allMatch(predicate -> predicate.test(event)))
                .toList();

        if (matches.isEmpty()) {
            ui.print("No matching events found.");
            return;
        }

        ui.print("Found " + matches.size() + " matching event(s).");
        for (Event event : matches) {
            String name = event.getName().isBlank() ? "Not set" : event.getName();
            ui.print("[" + event.getTag() + "] " + name);
        }
    }

    /** Creates a matcher for one filter; e/ retains its existing tag-or-name behavior. */
    private Predicate<Event> createFilter(String id, String value) {
        if (value.isBlank()) {
            throw new IllegalArgumentException("Search values must not be blank");
        }
        String keyword = value.strip().toLowerCase(Locale.ROOT);
        return switch (id) {
        case "e" -> event -> contains(event.getTag(), keyword) || contains(event.getName(), keyword);
        case "q" -> event -> contains(event.getTag(), keyword) || contains(event.getName(), keyword)
                || contains(event.getVenue(), keyword) || contains(event.getLogistics(), keyword);
        case "n" -> event -> contains(event.getName(), keyword);
        case "v" -> event -> contains(event.getVenue(), keyword);
        case "l" -> event -> contains(event.getLogistics(), keyword);
        case "d" -> {
            LocalDate date = LocalDate.parse(keyword);
            yield event -> date.equals(event.getDate());
        }
        case "by" -> {
            LocalDate deadline = LocalDate.parse(keyword);
            yield event -> deadline.equals(event.getDeadline());
        }
        case "b" -> {
            double budget = Double.parseDouble(keyword);
            yield event -> event.getBudget() == budget;
        }
        case "min" -> {
            double minimum = Double.parseDouble(keyword);
            yield event -> event.getBudget() >= minimum;
        }
        case "max" -> {
            double maximum = Double.parseDouble(keyword);
            yield event -> event.getBudget() <= maximum;
        }
        case "s" -> {
            boolean completed = switch (keyword) {
            case "completed" -> true;
            case "pending" -> false;
            default -> throw new IllegalArgumentException("Search status must be completed or pending");
            };
            yield event -> event.isCompleted() == completed;
        }
        default -> throw new IllegalArgumentException("Unsupported search filter: " + id);
        };
    }

    /** Matches a text field against an already normalized keyword independently of the default locale. */
    private boolean contains(String field, String keyword) {
        return field.toLowerCase(Locale.ROOT).contains(keyword);
    }
}
