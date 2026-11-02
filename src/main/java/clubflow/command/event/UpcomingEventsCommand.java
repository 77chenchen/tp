package clubflow.command.event;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import clubflow.Club;
import clubflow.UserInterface;
import clubflow.command.Command;
import clubflow.event.Event;

//@@author camimaigrot
/**
 * Lists events that have not been completed and take place today or later.
 */
public class UpcomingEventsCommand extends Command {
    private final Club club;

    /**
     * Creates the command using the active club's shared event store.
     *
     * @param ui user interface used to display events
     * @param club club containing events to display
     */
    public UpcomingEventsCommand(UserInterface ui, Club club) {
        super(ui);
        this.club = Objects.requireNonNull(club, "Club data must not be null");
        addSyntax("(no arguments)");
    }

    @Override
    public String[] requiredArgIds() {
        return new String[]{};
    }

    @Override
    public String[] optionalArgIds() {
        return new String[]{};
    }

    @Override
    public boolean execute(HashMap<String, String> args) {
        Objects.requireNonNull(args, "Arguments must not be null");
        LocalDate today = LocalDate.now();
        List<Event> upcomingEvents = club.getEvents().stream()
                .filter(event -> !event.isCompleted() && event.getDate() != null)
                .filter(event -> !event.getDate().isBefore(today))
                .sorted(Comparator.comparing(Event::getDate)
                        .thenComparing(Event::getTag, String.CASE_INSENSITIVE_ORDER))
                .toList();

        if (upcomingEvents.isEmpty()) {
            ui.print("No upcoming events found.");
            return false;
        }

        ui.print("Upcoming events (" + upcomingEvents.size() + "):");
        for (Event event : upcomingEvents) {
            String name = event.getName().isBlank() ? "Not set" : event.getName();
            ui.print("[" + event.getTag() + "] " + name + " (" + event.getDate() + ")");
        }
        return false;
    }
}
//@@author
