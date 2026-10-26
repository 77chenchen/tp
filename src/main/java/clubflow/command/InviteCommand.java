package clubflow.command;

import clubflow.UserInterface;
import clubflow.ClubData;
import clubflow.exception.CommandParseException;
import clubflow.exception.EventNotFoundException;
import clubflow.event.Event;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Objects;

/**
 * Generates a copyable invitation for an event selected by tag.
 */
public class InviteCommand extends Command {
    private final ClubData clubData;

    /**
     * Creates a command that generates invitations from saved events.
     *
     * @param ui user interface used to display invitations
     * @param clubData shared club data containing events
     */
    public InviteCommand(UserInterface ui, ClubData clubData) {
        super(ui);
        this.clubData = Objects.requireNonNull(clubData, "Club data must not be null");
        addSyntax("e/Tag");
    }

    @Override 
    public String[] requiredArgIds() {
        return new String[]{EventArgumentIds.TAG};
    }

    @Override 
    public String[] optionalArgIds() {
        return new String[]{};
    }

    @Override
    public void validateArgs(HashMap<String, String> args) throws CommandParseException {
        boolean hasEventTag = args.containsKey(EventArgumentIds.TAG);

        if (hasEventTag && args.get(EventArgumentIds.TAG).isBlank()) {
            throw new CommandParseException("An event tag must be provided after e/.");
        }
    }

    @Override 
    public boolean execute(HashMap<String, String> args) throws EventNotFoundException {
        String tag = args.get(EventArgumentIds.TAG);
        Event event = clubData.findEventByTag(tag)
            .orElseThrow(() -> new EventNotFoundException(tag));

        String name = event.getName();
        LocalDate date = event.getDate();
        String venue = event.getVenue();
        String invitation = """
            You're invited to: %s!

            Date: %s
            Location: %s

            Join us and have some fun!
            """.formatted(
            name.isBlank() ? "[Add event name]" : name,
            date == null ? "[Add date]" : date.toString(),
            venue.isBlank() ? "[Add venue]" : venue
        );
        ui.printWithoutPrefix(invitation);
        return false;
    }
}
