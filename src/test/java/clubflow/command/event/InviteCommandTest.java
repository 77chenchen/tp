package clubflow.command.event;

import clubflow.Club;
import clubflow.UserInterface;
import clubflow.event.Event;
import clubflow.exception.CommandParseException;
import clubflow.exception.EventNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.HeadlessException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests invitation text and clipboard outcomes without changing the system clipboard.
 */
class InviteCommandTest {
    private Club club;
    private RecordingUi ui;
    private List<String> copiedText;
    private InviteCommand command;

    @BeforeEach
    void setUp() {
        club = new Club();
        ui = new RecordingUi();
        copiedText = new ArrayList<>();
        command = new InviteCommand(ui, club, copiedText::add);
    }

    @Test
    void execute_existingEvent_copiesExactInvitationAndKeepsEventUnchanged() throws Exception {
        Event event = new Event("CAMP", "Club Camp", LocalDate.of(2026, 12, 15),
                null, 100, "East Coast Park", "Bring water");
        club.addEvent(event);
        club.markSaved();

        assertFalse(command.execute(arguments("CAMP")));

        String expected = """
            You're invited to: Club Camp!

            Date: 2026-12-15
            Location: East Coast Park

            Join us and have some fun!
            """;
        assertEquals(List.of(expected), copiedText);
        assertEquals(expected, ui.invitation);
        assertEquals(List.of("Invitation copied to clipboard!"), ui.messages);
        assertSame(event, club.findEventByTag("CAMP").orElseThrow());
        assertFalse(club.hasUnsavedChanges());
    }

    @Test
    void execute_missingOptionalDetails_copiesPlaceholders() throws Exception {
        club.addEvent(new Event("CAMP", "", null, null, 0, "", ""));

        command.execute(arguments("CAMP"));

        String invitation = copiedText.getFirst();
        assertTrue(invitation.contains("You're invited to: [Add event name]!"));
        assertTrue(invitation.contains("Date: [Add date]"));
        assertTrue(invitation.contains("Location: [Add venue]"));
        assertEquals(invitation, ui.invitation);
    }

    @Test
    void execute_headlessEnvironment_printsInvitationAndFallback() throws Exception {
        assertClipboardFailure(new HeadlessException());
    }

    @Test
    void execute_busyClipboard_printsInvitationAndFallback() throws Exception {
        assertClipboardFailure(new IllegalStateException("Clipboard is busy"));
    }

    @Test
    void execute_unknownTag_doesNotPrintOrCopyInvitation() {
        assertThrows(EventNotFoundException.class, () -> command.execute(arguments("UNKNOWN")));

        assertTrue(copiedText.isEmpty());
        assertEquals("", ui.invitation);
        assertTrue(ui.messages.isEmpty());
    }

    @Test
    void validateArgs_blankTag_rejectsInputWithoutCopying() {
        assertThrows(CommandParseException.class, () -> command.validateArgs(arguments("  ")));

        assertTrue(copiedText.isEmpty());
    }

    /** Verifies a failed copy preserves the printed invitation and never reports success. */
    private void assertClipboardFailure(RuntimeException failure) throws Exception {
        club.addEvent(new Event("CAMP", "Club Camp", null, null, 0, "", ""));
        InviteCommand failingCommand = new InviteCommand(ui, club, text -> {
            // The invitation must already be visible before attempting to copy it.
            assertEquals(ui.invitation, text);
            throw failure;
        });

        assertFalse(failingCommand.execute(arguments("CAMP")));

        assertTrue(ui.invitation.contains("You're invited to: Club Camp!"));
        assertEquals(List.of("Could not access the clipboard. Please copy the invitation above manually."),
                ui.messages);
    }

    private HashMap<String, String> arguments(String tag) {
        return new HashMap<>(Map.of("e", tag));
    }

    /** Records invitation output separately from prefixed status messages. */
    private static class RecordingUi extends UserInterface {
        private String invitation = "";
        private final List<String> messages = new ArrayList<>();

        RecordingUi() {
            super("[TEST] ", "> ");
        }

        @Override
        public void printWithoutPrefix(String text) {
            invitation = text;
        }

        @Override
        public void print(String message) {
            messages.add(message);
        }
    }
}
