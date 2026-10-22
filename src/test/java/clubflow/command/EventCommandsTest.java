package clubflow.command;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import clubflow.ClubData;
import clubflow.UserInterface;
import clubflow.event.Event;
import clubflow.exception.ClubFlowException;
import clubflow.exception.CommandParseException;
import clubflow.exception.DuplicateEventException;
import clubflow.exception.EventNotFoundException;

/**
 * Tests event creation, validation, and the create-view-delete workflow using shared club data.
 */
class EventCommandsTest {
    private final PrintStream originalOutput = System.out;
    private ByteArrayOutputStream output;
    private PrintStream capturedOutput;

    @BeforeEach
    void redirectOutput() {
        output = new ByteArrayOutputStream();
        capturedOutput = new PrintStream(output, true, StandardCharsets.UTF_8);
        System.setOut(capturedOutput);
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOutput);
        capturedOutput.close();
    }

    @Test
    void eventCommands_sharedClubData_createViewAndDeleteEvent() throws ClubFlowException {
        ClubData clubData = new ClubData();
        CommandParser parser = createEventCommandParser(clubData);

        parser.parse("createEvent e/XMAS n/\"Christmas Party\" d/2026-12-25 "
                + "by/2026-12-20 b/500 v/\"Main Hall\" l/\"Tables and chairs\"");

        Event event = clubData.findEventByTag("XMAS").orElseThrow();
        assertEquals("Christmas Party", event.getName());
        assertEquals(LocalDate.of(2026, 12, 25), event.getDate());
        assertEquals(LocalDate.of(2026, 12, 20), event.getDeadline());
        assertEquals(500.0, event.getBudget());
        // Isolate each command's output so an earlier response cannot satisfy a later assertion.
        output.reset();
        assertFalse(parser.parse("viewEvent e/XMAS"));
        assertTrue(printedLines().contains("[TEST] [XMAS] Christmas Party"));

        output.reset();
        assertFalse(parser.parse("viewEvent all"));
        assertTrue(printedLines().contains("[TEST] Displaying all events."));
        assertTrue(printedLines().contains("[TEST] [XMAS] Christmas Party"));

        parser.parse("deleteEvent e/XMAS");
        assertEquals(0, clubData.getEvents().size());

        output.reset();
        EventNotFoundException error = assertThrows(EventNotFoundException.class,
                () -> parser.parse("viewEvent e/XMAS"));
        assertEquals("Event not found: XMAS", error.getMessage());
        assertEquals(List.of(), printedLines());

        output.reset();
        assertFalse(parser.parse("viewEvent all"));
        assertEquals(List.of("[TEST] No events found."), printedLines());
    }

    @Test
    void deleteEvent_unknownTag_throwsAndPreservesExistingEvents() throws ClubFlowException {
        ClubData clubData = new ClubData();
        CommandParser parser = createEventCommandParser(clubData);
        parser.parse("createEvent e/XMAS n/\"Christmas Party\"");
        parser.parse("createEvent e/AGM n/Meeting");
        List<Event> existingEvents = clubData.getEvents();
        output.reset();

        EventNotFoundException error = assertThrows(EventNotFoundException.class,
                () -> parser.parse("deleteEvent e/UNKNOWN"));

        assertEquals("Event not found: UNKNOWN", error.getMessage());
        assertEquals(List.of(), printedLines());
        assertEquals(existingEvents, clubData.getEvents());
    }

    @Test
    void createEvent_invalidValues_rejectsCommand() {
        ClubData clubData = new ClubData();
        CommandParser parser = createEventCommandParser(clubData);

        assertAll(
                () -> assertThrows(CommandParseException.class,
                        () -> parser.parse("createEvent e/")),
                () -> assertThrows(CommandParseException.class,
                        () -> parser.parse("createEvent e/XMAS d/25-12-2026")),
                () -> assertThrows(CommandParseException.class,
                        () -> parser.parse("createEvent e/XMAS by/tomorrow")),
                () -> assertThrows(CommandParseException.class,
                        () -> parser.parse("createEvent e/XMAS b/free")),
                () -> assertThrows(CommandParseException.class,
                        () -> parser.parse("createEvent e/XMAS b/-1")),
                () -> assertThrows(CommandParseException.class,
                        () -> parser.parse("deleteEvent e/"))
        );
        assertEquals(0, clubData.getEvents().size());
    }

    @Test
    void createEvent_duplicateTags_throwsAndPreservesExistingEvents() throws ClubFlowException {
        ClubData clubData = new ClubData();
        CommandParser parser = createEventCommandParser(clubData);

        parser.parse("createEvent e/XMAS n/Original d/2026-12-25 "
                + "by/2026-12-20 b/500 v/Hall l/Chairs");
        clubData.updateEvent(clubData.getEvents().getFirst().withCompleted(true));
        parser.parse("createEvent e/AGM n/Meeting");
        List<Event> existingEvents = clubData.getEvents();
        output.reset();

        for (String tag : List.of("XMAS", "xmas", "XmAs")) {
            DuplicateEventException error = assertThrows(DuplicateEventException.class,
                    () -> parser.parse("createEvent e/" + tag + " n/Duplicate b/999"));

            assertEquals("Event already exists: " + tag, error.getMessage());
            assertEquals(existingEvents, clubData.getEvents());
        }
        assertEquals(List.of(), printedLines());
    }

    @Test
    void createEvent_sameNameWithDifferentTags_allowsBothEvents() throws ClubFlowException {
        ClubData clubData = new ClubData();
        CommandParser parser = createEventCommandParser(clubData);

        parser.parse("createEvent e/XMAS n/Party");
        parser.parse("createEvent e/NYE n/Party");

        assertEquals(List.of("XMAS", "NYE"), clubData.getEvents().stream().map(Event::getTag).toList());
    }

    /**
     * Creates a parser whose event commands all share the supplied data store.
     *
     * @param clubData data store shared by the event commands
     * @return parser configured with create, view, and delete event commands
     */
    private CommandParser createEventCommandParser(ClubData clubData) {
        UserInterface ui = new UserInterface("[TEST] ", "> ");
        CommandParser parser = new CommandParser(ui);
        parser.register("createEvent", new CreateEventCommand(ui, clubData));
        parser.register("viewEvent", new ViewEventCommand(ui, clubData));
        parser.register("deleteEvent", new DeleteEventCommand(ui, clubData));
        return parser;
    }

    /**
     * Returns captured output as lines independently of the platform's line endings.
     *
     * @return lines printed since the last output reset
     */
    private List<String> printedLines() {
        return output.toString(StandardCharsets.UTF_8).lines().toList();
    }
}
