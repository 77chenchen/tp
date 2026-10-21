package clubflow.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import clubflow.ClubData;
import clubflow.SaveLoadManager;
import clubflow.UserInterface;
import clubflow.event.Event;
import clubflow.exception.CommandParseException;

/**
 * Tests partial event updates, validation, and persistence through the command parser.
 */
class EditEventCommandTest {
    private final PrintStream originalOutput = System.out;
    private ByteArrayOutputStream output;
    private PrintStream capturedOutput;
    private ClubData clubData;
    private CommandParser parser;
    private Event originalEvent;

    @BeforeEach
    void setUp() {
        output = new ByteArrayOutputStream();
        capturedOutput = new PrintStream(output, true, StandardCharsets.UTF_8);
        System.setOut(capturedOutput);
        clubData = new ClubData();
        originalEvent = new Event("XMAS", "Christmas Party", LocalDate.of(2026, 12, 25),
                LocalDate.of(2026, 12, 20), 500, "Main Hall", "Tables and chairs");
        clubData.addEvent(originalEvent);
        UserInterface ui = new UserInterface("[TEST] ", "> ");
        parser = new CommandParser(ui);
        parser.register("editEvent", new EditEventCommand(ui, clubData));
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOutput);
        capturedOutput.close();
    }

    @Test
    void editEvent_oneField_preservesOmittedDetailsAndOriginalTag() throws CommandParseException {
        assertFalse(parser.parse("EDITEVENT E/xmas v/\"New Hall\""));

        Event updated = clubData.findEventByTag("XMAS").orElseThrow();
        assertEquals("New Hall", updated.getVenue());
        assertEquals("XMAS", updated.getTag());
        assertEquals(originalEvent.getName(), updated.getName());
        assertEquals(originalEvent.getDate(), updated.getDate());
        assertEquals(originalEvent.getDeadline(), updated.getDeadline());
        assertEquals(originalEvent.getBudget(), updated.getBudget());
        assertEquals(originalEvent.getLogistics(), updated.getLogistics());
        assertEquals(List.of("[TEST] Updated event: XMAS"), printedLines());
    }

    @Test
    void editEvent_allDetails_updatesEverySuppliedField() throws CommandParseException {
        assertFalse(parser.parse("editEvent e/XMAS n/\"Winter Dinner\" d/2026-12-26 "
                + "by/2026-12-21 b/625.50 v/Restaurant l/\"Book tables\""));

        Event updated = clubData.findEventByTag("XMAS").orElseThrow();
        assertEquals("Winter Dinner", updated.getName());
        assertEquals(LocalDate.of(2026, 12, 26), updated.getDate());
        assertEquals(LocalDate.of(2026, 12, 21), updated.getDeadline());
        assertEquals(625.50, updated.getBudget());
        assertEquals("Restaurant", updated.getVenue());
        assertEquals("Book tables", updated.getLogistics());
        assertEquals(1, clubData.getEvents().size());
    }

    @Test
    void editEvent_draftEvent_preservesUnspecifiedDefaults() throws CommandParseException {
        clubData.addEvent(new Event("DRAFT", "", null, null, 0, "", ""));

        assertFalse(parser.parse("editEvent e/DRAFT n/Planning"));

        Event updated = clubData.findEventByTag("DRAFT").orElseThrow();
        assertEquals("Planning", updated.getName());
        assertNull(updated.getDate());
        assertNull(updated.getDeadline());
        assertEquals(0, updated.getBudget());
        assertEquals("", updated.getVenue());
        assertEquals("", updated.getLogistics());
    }

    @Test
    void editEvent_emptyTextAndZeroBudget_clearsSelectedDetails() throws CommandParseException {
        assertFalse(parser.parse("editEvent e/XMAS n/\"\" v/\"\" l/\"\" b/0"));

        Event updated = clubData.findEventByTag("XMAS").orElseThrow();
        assertEquals("", updated.getName());
        assertEquals("", updated.getVenue());
        assertEquals("", updated.getLogistics());
        assertEquals(0, updated.getBudget());
        assertEquals(originalEvent.getDate(), updated.getDate());
        assertEquals(originalEvent.getDeadline(), updated.getDeadline());
    }

    @Test
    void editEvent_unknownTag_reportsMissingEventWithoutAddingOne() throws CommandParseException {
        assertFalse(parser.parse("editEvent e/UNKNOWN n/New"));

        assertEquals(List.of(originalEvent), clubData.getEvents());
        assertEquals(List.of("[TEST] Event not found: UNKNOWN"), printedLines());
    }

    @Test
    void editEvent_invalidField_doesNotApplyOtherChanges() {
        for (String field : List.of("d/2026-02-30", "d/\"\"", "by/tomorrow", "by/\"\"",
                "b/free", "b/-1", "b/NaN", "b/Infinity", "b/1e309", "b/\"\"",
                "v/Bad|Venue", "l/Bad|Notes")) {
            String input = "editEvent e/XMAS n/Changed " + field;

            assertThrows(CommandParseException.class, () -> parser.parse(input), input);
            assertSame(originalEvent, clubData.findEventByTag("XMAS").orElseThrow());
        }
        assertThrows(CommandParseException.class, () -> parser.parse("editEvent e/XMAS n/Bad|Name"));
        assertSame(originalEvent, clubData.findEventByTag("XMAS").orElseThrow());
        assertEquals(List.of(), printedLines());
    }

    @Test
    void editEvent_missingDuplicateOrUnsupportedArguments_rejectsRequest() {
        for (String input : List.of("editEvent", "editEvent n/Changed", "editEvent e/ n/Changed",
                "editEvent e/\"   \" n/Changed", "editEvent e/XMAS", "editEvent e/XMAS all",
                "editEvent e/XMAS n/First n/Second", "editEvent e/XMAS x/Unknown")) {
            assertThrows(CommandParseException.class, () -> parser.parse(input), input);
        }

        assertEquals(List.of(originalEvent), clubData.getEvents());
        assertEquals(List.of(), printedLines());
    }

    @Test
    void editEvent_multipleEvents_preservesOrderAndOtherEvents() throws CommandParseException {
        Event other = new Event("OTHER", "Other", null, null, 0, "", "");
        clubData.addEvent(other);

        parser.parse("editEvent e/XMAS n/Changed");

        assertEquals("XMAS", clubData.getEvents().getFirst().getTag());
        assertEquals("Changed", clubData.getEvents().getFirst().getName());
        assertSame(other, clubData.getEvents().get(1));
    }

    @Test
    void editEvent_saveAndLoad_preservesUpdatedDetails(@TempDir Path directory)
            throws CommandParseException, IOException {
        parser.parse("editEvent e/XMAS n/Dinner v/Restaurant b/600");
        SaveLoadManager storage = new SaveLoadManager();
        Path saveFile = directory.resolve("edited.cf");

        storage.save(clubData, saveFile);
        Event loaded = storage.load(saveFile).findEventByTag("XMAS").orElseThrow();

        assertEquals("Dinner", loaded.getName());
        assertEquals("Restaurant", loaded.getVenue());
        assertEquals(600, loaded.getBudget());
        assertEquals(originalEvent.getDate(), loaded.getDate());
        assertEquals(originalEvent.getDeadline(), loaded.getDeadline());
        assertEquals(originalEvent.getLogistics(), loaded.getLogistics());
    }

    /**
     * Returns output lines independently of the operating system's line endings.
     *
     * @return lines printed by the user interface
     */
    private List<String> printedLines() {
        return output.toString(StandardCharsets.UTF_8).lines().toList();
    }
}
