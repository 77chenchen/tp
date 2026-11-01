package clubflow.command.event;

import clubflow.Club;
import clubflow.SaveLoadManager;
import clubflow.UserInterface;
import clubflow.event.Event;
import clubflow.exception.CommandParseException;
import clubflow.exception.EventNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.HeadlessException;
import java.time.LocalDate;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests invitation text and clipboard outcomes without changing the system clipboard.
 */
class InviteCommandTest {
    @TempDir
    Path testDirectory;

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
        Event original = new Event("CAMP", "", null, null, 0, "", "");
        club.addEvent(original);
        club.markSaved();
        ui.responses.add(" n ");

        command.execute(arguments("CAMP"));

        String invitation = copiedText.getFirst();
        assertTrue(invitation.contains("You're invited to: [Add event name]!"));
        assertTrue(invitation.contains("Date: [Add date]"));
        assertTrue(invitation.contains("Location: [Add venue]"));
        assertEquals(invitation, ui.invitation);
        assertTrue(ui.messages.contains(
                "Warning: invitation is missing event name, event date, event venue."));
        assertSame(original, club.findEventByTag("CAMP").orElseThrow());
        assertFalse(club.hasUnsavedChanges());
        assertFalse(invitation.contains("Warning:"));
        assertTrue(ui.responses.isEmpty());
    }

    @Test
    void execute_acceptMissingDetails_updatesEventAndCopiesCompletedInvitation() throws Exception {
        club.addEvent(new Event("CAMP", "", null, LocalDate.of(2026, 12, 1),
                250, "", "Bring water", true));
        club.markSaved();
        ui.responses.addAll(List.of(" y ", " Club Camp ", " 2026-12-15 ", " Main Hall "));

        assertFalse(command.execute(arguments("CAMP")));

        Event updated = club.findEventByTag("CAMP").orElseThrow();
        assertEquals("Club Camp", updated.getName());
        assertEquals(LocalDate.of(2026, 12, 15), updated.getDate());
        assertEquals("Main Hall", updated.getVenue());
        assertEquals(LocalDate.of(2026, 12, 1), updated.getDeadline());
        assertEquals(250, updated.getBudget());
        assertEquals("Bring water", updated.getLogistics());
        assertTrue(updated.isCompleted());
        assertTrue(club.hasUnsavedChanges());
        assertEquals(List.of(ui.invitation), copiedText);
        assertTrue(ui.invitation.contains("You're invited to: Club Camp!"));
        assertTrue(ui.invitation.contains("Date: 2026-12-15"));
        assertTrue(ui.invitation.contains("Location: Main Hall"));
        assertFalse(ui.invitation.contains("[Add"));
        assertFalse(ui.invitation.contains("Warning:"));
        assertFalse(ui.invitation.contains("Updated event:"));
        assertEquals(1, ui.messages.stream().filter("Updated event: CAMP"::equals).count());
        assertTrue(ui.responses.isEmpty());

        Path saveFile = testDirectory.resolve("club.cf");
        SaveLoadManager persistence = new SaveLoadManager();
        persistence.save(club, saveFile);
        Event restored = persistence.load(saveFile).findEventByTag("CAMP").orElseThrow();
        assertEquals(updated.getName(), restored.getName());
        assertEquals(updated.getDate(), restored.getDate());
        assertEquals(updated.getVenue(), restored.getVenue());
        assertTrue(restored.isCompleted());
    }

    @Test
    void execute_onlyVenueMissing_promptsOnlyForVenueAndPreservesExistingDetails() throws Exception {
        club.addEvent(new Event("CAMP", "Club Camp", LocalDate.of(2026, 12, 15),
                null, 0, "", ""));
        ui.responses.addAll(List.of("Y", "Main Hall"));

        command.execute(arguments("CAMP"));

        Event updated = club.findEventByTag("CAMP").orElseThrow();
        assertEquals("Club Camp", updated.getName());
        assertEquals(LocalDate.of(2026, 12, 15), updated.getDate());
        assertEquals("Main Hall", updated.getVenue());
        assertFalse(ui.messages.stream().anyMatch(message -> message.startsWith("Enter event name")));
        assertFalse(ui.messages.stream().anyMatch(message -> message.startsWith("Enter event date")));
        assertTrue(ui.responses.isEmpty());
    }

    @Test
    void execute_invalidConfirmation_repromptsBeforeDeclining() throws Exception {
        club.addEvent(new Event("CAMP", "", null, null, 0, "", ""));
        club.markSaved();
        ui.responses.addAll(List.of("maybe", "", "N"));

        command.execute(arguments("CAMP"));

        assertEquals(2, ui.messages.stream().filter("Please enter Y or N."::equals).count());
        assertFalse(club.hasUnsavedChanges());
        assertEquals(1, copiedText.size());
        assertTrue(ui.responses.isEmpty());
    }

    @Test
    void execute_invalidFieldValues_repromptsUntilAllValuesAreValid() throws Exception {
        club.addEvent(new Event("CAMP", "", null, null, 0, "", ""));
        ui.responses.addAll(List.of("Y", " ", "Bad|Name", "Club Camp",
                "not-a-date", "2026-02-30", "2026-12-15", "", "Bad|Venue", "Main Hall"));

        command.execute(arguments("CAMP"));

        Event updated = club.findEventByTag("CAMP").orElseThrow();
        assertEquals("Club Camp", updated.getName());
        assertEquals(LocalDate.of(2026, 12, 15), updated.getDate());
        assertEquals("Main Hall", updated.getVenue());
        assertTrue(ui.messages.contains("Event name cannot be blank."));
        assertTrue(ui.messages.contains("Event name cannot contain |."));
        assertTrue(ui.messages.contains("Event venue cannot be blank."));
        assertTrue(ui.messages.contains("Event venue cannot contain |."));
        assertEquals(2, ui.messages.stream()
                .filter("Please enter a valid date in YYYY-MM-DD format."::equals).count());
        assertEquals(1, copiedText.size());
        assertTrue(ui.responses.isEmpty());
    }

    @Test
    void execute_inputEndsDuringEditing_doesNotPartiallyUpdateOrCopy() {
        Event original = new Event("CAMP", "", null, null, 0, "", "");
        club.addEvent(original);
        club.markSaved();
        ui.responses.addAll(List.of("Y", "Club Camp"));
        ui.endInputWhenResponsesExhausted = true;

        assertThrows(NoSuchElementException.class, () -> command.execute(arguments("CAMP")));

        assertSame(original, club.findEventByTag("CAMP").orElseThrow());
        assertFalse(club.hasUnsavedChanges());
        assertTrue(copiedText.isEmpty());
        assertEquals("", ui.invitation);
    }

    @Test
    void execute_copyFailsAfterEditing_keepsUpdatedEventAndPrintedInvitation() throws Exception {
        club.addEvent(new Event("CAMP", "Club Camp", LocalDate.of(2026, 12, 15),
                null, 0, "", ""));
        club.markSaved();
        ui.responses.addAll(List.of("Y", "Main Hall"));
        InviteCommand failingCommand = new InviteCommand(ui, club, text -> {
            assertEquals(ui.invitation, text);
            assertTrue(text.contains("Location: Main Hall"));
            throw new IllegalStateException("Clipboard is busy");
        });

        assertFalse(failingCommand.execute(arguments("CAMP")));

        assertEquals("Main Hall", club.findEventByTag("CAMP").orElseThrow().getVenue());
        assertTrue(club.hasUnsavedChanges());
        assertTrue(ui.messages.contains(
                "Could not access the clipboard. Please copy the invitation above manually."));
        assertFalse(ui.messages.contains("Invitation copied to clipboard!"));
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
        ui.responses.add("N");
        InviteCommand failingCommand = new InviteCommand(ui, club, text -> {
            // The invitation must already be visible before attempting to copy it.
            assertEquals(ui.invitation, text);
            throw failure;
        });

        assertFalse(failingCommand.execute(arguments("CAMP")));

        assertTrue(ui.invitation.contains("You're invited to: Club Camp!"));
        assertTrue(ui.messages.contains("Warning: invitation is missing event date, event venue."));
        assertTrue(ui.messages.contains(
                "Could not access the clipboard. Please copy the invitation above manually."));
        assertFalse(ui.messages.contains("Invitation copied to clipboard!"));
    }

    private HashMap<String, String> arguments(String tag) {
        return new HashMap<>(Map.of("e", tag));
    }

    /** Records invitation output separately from prefixed status messages. */
    private static class RecordingUi extends UserInterface {
        private String invitation = "";
        private final List<String> messages = new ArrayList<>();
        private final ArrayDeque<String> responses = new ArrayDeque<>();
        private boolean endInputWhenResponsesExhausted;

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

        @Override
        public String prompt() {
            if (responses.isEmpty() && !endInputWhenResponsesExhausted) {
                throw new AssertionError("Unexpected prompt: provide a scripted response in the test");
            }
            return responses.removeFirst();
        }
    }
}
