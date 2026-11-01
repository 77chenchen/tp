package clubflow.command.event;

import clubflow.command.Command;
import clubflow.command.EventArgumentIds;

import clubflow.UserInterface;
import clubflow.Club;
import clubflow.exception.CommandParseException;
import clubflow.exception.EventNotFoundException;
import clubflow.event.Event;

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Consumer;

//@@author Bryan-chua
/**
 * Generates a copyable invitation for an event selected by tag.
 */
public class InviteCommand extends Command {
    private final Club club;
    /** Copies invitation text; tests can supply a substitute without accessing the desktop. */
    private final Consumer<String> clipboardWriter;

    /**
     * Creates a command that generates invitations from saved events.
     *
     * @param ui user interface used to display invitations
     * @param club shared club data containing events
     */
    public InviteCommand(UserInterface ui, Club club) {
        this(ui, club, invitation -> Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(invitation), null));
    }

    /**
     * Creates a command with a replaceable clipboard operation for isolated tests.
     *
     * @param ui user interface used to display invitations
     * @param club shared club data containing events
     * @param clipboardWriter operation that copies text or throws if the clipboard is unavailable
     */
    InviteCommand(UserInterface ui, Club club, Consumer<String> clipboardWriter) {
        super(ui);
        this.club = Objects.requireNonNull(club, "Club data must not be null");
        this.clipboardWriter = Objects.requireNonNull(clipboardWriter, "Clipboard writer must not be null");
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
        Event event = club.findEventByTag(tag)
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
        try {
            clipboardWriter.accept(invitation);

            ui.print("Invitation copied to clipboard!");
        } catch (HeadlessException | IllegalStateException e) {
            ui.print("Could not access the clipboard. Please copy the invitation above manually.");
        }

        return false;
    }
}
