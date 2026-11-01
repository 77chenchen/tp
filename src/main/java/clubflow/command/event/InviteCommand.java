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
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.ArrayList;
import java.util.List;

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
        List<String> missingFields = new ArrayList<>();
        
        if (name.isBlank()) {
            missingFields.add("event name");
        }
        if (date == null) {
            missingFields.add("event date");
        }
        if (venue.isBlank()) {
            missingFields.add("event venue");
        }

        if (!missingFields.isEmpty()) {
            ui.print("Warning: invitation is missing " + String.join(", ", missingFields) + ".");
            ui.print("Would you like to fill them in? (Y/N)");
            String response = ui.prompt().strip();
            while (!response.equalsIgnoreCase("y") && !response.equalsIgnoreCase("n")) {
                ui.print("Please enter Y or N.");
                response = ui.prompt().strip();
            }

            if (response.equalsIgnoreCase("y")) {
                for (String missing : missingFields) {
                    if (missing.equals("event name")) {
                        while (true) {
                            ui.print("Enter event name: ");
                            name = ui.prompt().strip();

                            if (name.isBlank()) {
                                ui.print("Event name cannot be blank.");
                            } else if (name.contains("|")) {
                                ui.print("Event name cannot contain |.");
                            } else {
                                break;
                            }
                        }
                    } else if (missing.equals("event venue")) {
                        while (true) {
                            ui.print("Enter event venue: ");
                            venue = ui.prompt().strip();

                            if (venue.isBlank()) {
                                ui.print("Event venue cannot be blank.");
                            } else if (venue.contains("|")) {
                                ui.print("Event venue cannot contain |.");
                            } else {
                                break;
                            }
                        }
                    } else if (missing.equals("event date")) {
                        while (true) {
                            ui.print("Enter event date (YYYY-MM_DD): ");
                            try {
                                date = LocalDate.parse(ui.prompt().strip());
                                break;
                            } catch (DateTimeParseException e) {
                                ui.print("Please enter a valid date in YYYY-MM-DD format.");
                            }
                        }
                    }
                }
                Event updated = new Event(
                    event.getTag(), name, date, event.getDeadline(),
                    event.getBudget(), venue, event.getLogistics(),
                    event.isCompleted()
                );
                boolean updatedSuccessfully = club.updateEvent(updated);
                assert updatedSuccessfully : "The selected event must still exist";
                ui.print("Updated event: " + updated.getTag());
            }
        }
        
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
