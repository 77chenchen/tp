package clubflow.search;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import clubflow.Club;
import clubflow.UserInterface;
import clubflow.event.Event;

/**
 * Searches current event tags and names and displays matching event summaries.
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

    /**
     * Finds case-insensitive substring matches in event tags or names, preserving store order.
     *
     * @param keyword non-blank search text validated and trimmed by the find command
     */
    public void search(String keyword) {
        String normalizedKeyword = Objects.requireNonNull(keyword, "Search keyword must not be null")
                .toLowerCase(Locale.ROOT);
        List<Event> matches = club.getEvents().stream()
                .filter(event -> event.getTag().toLowerCase(Locale.ROOT).contains(normalizedKeyword)
                        || event.getName().toLowerCase(Locale.ROOT).contains(normalizedKeyword))
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
}
