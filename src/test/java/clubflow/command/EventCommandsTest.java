package clubflow.command;

import clubflow.command.event.CreateEventCommand;
import clubflow.command.event.ClearEventCommand;
import clubflow.command.event.DeleteEventCommand;
import clubflow.command.event.ViewEventCommand;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import clubflow.Club;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import clubflow.UserInterface;
import clubflow.event.Event;
import clubflow.member.Member;
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

    //@@author Eugene3129
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
    void eventCommands_sharedClub_createViewAndDeleteEvent() throws ClubFlowException {
        Club club = new Club();
        CommandParser parser = createEventCommandParser(club);

        parser.parse("createEvent e/XMAS n/\"Christmas Party\" d/2026-12-25 "
                + "by/2026-12-20 b/500 v/\"Main Hall\" l/\"Tables and chairs\"");

        Event event = club.findEventByTag("XMAS").orElseThrow();
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
        assertEquals(0, club.getEvents().size());

        output.reset();
        EventNotFoundException error = assertThrows(EventNotFoundException.class,
                () -> parser.parse("viewEvent e/XMAS"));
        assertEquals("Event not found: XMAS", error.getMessage());
        assertEquals(List.of(), printedLines());

        output.reset();
        assertFalse(parser.parse("viewEvent all"));
        assertEquals(List.of("[TEST] No events found."), printedLines());
    }

    //@@author
    @Test
    void deleteEvent_unknownTag_throwsAndPreservesExistingEvents() throws ClubFlowException {
        Club club = new Club();
        CommandParser parser = createEventCommandParser(club);
        parser.parse("createEvent e/XMAS n/\"Christmas Party\"");
        parser.parse("createEvent e/AGM n/Meeting");
        List<Event> existingEvents = club.getEvents();
        output.reset();

        EventNotFoundException error = assertThrows(EventNotFoundException.class,
                () -> parser.parse("deleteEvent e/UNKNOWN"));

        assertEquals("Event not found: UNKNOWN", error.getMessage());
        assertEquals(List.of(), printedLines());
        assertEquals(existingEvents, club.getEvents());
    }

    //@@author Eugene3129
    @Test
    void createEvent_invalidValues_rejectsCommand() {
        Club club = new Club();
        CommandParser parser = createEventCommandParser(club);

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
        for (String field : List.of("d/", "by/", "b/", "d/\"\"", "by/\"\"", "b/\"\"")) {
            assertThrows(CommandParseException.class, () -> parser.parse("createEvent e/XMAS " + field));
        }
        assertEquals(0, club.getEvents().size());
    }

    //@@author
    @Test
    void createEvent_duplicateTags_throwsAndPreservesExistingEvents() throws ClubFlowException {
        Club club = new Club();
        CommandParser parser = createEventCommandParser(club);

        parser.parse("createEvent e/XMAS n/Original d/2026-12-25 "
                + "by/2026-12-20 b/500 v/Hall l/Chairs");
        club.updateEvent(club.getEvents().getFirst().withCompleted(true));
        parser.parse("createEvent e/AGM n/Meeting");
        List<Event> existingEvents = club.getEvents();
        output.reset();

        for (String tag : List.of("XMAS", "xmas", "XmAs")) {
            DuplicateEventException error = assertThrows(DuplicateEventException.class,
                    () -> parser.parse("createEvent e/" + tag + " n/Duplicate b/999"));

            assertEquals("Event already exists: " + tag, error.getMessage());
            assertEquals(existingEvents, club.getEvents());
        }
        assertEquals(List.of(), printedLines());
    }

    @Test
    void createEvent_sameNameWithDifferentTags_allowsBothEvents() throws ClubFlowException {
        Club club = new Club();
        CommandParser parser = createEventCommandParser(club);

        parser.parse("createEvent e/XMAS n/Party");
        parser.parse("createEvent e/NYE n/Party");

        assertEquals(List.of("XMAS", "NYE"), club.getEvents().stream().map(Event::getTag).toList());
    }

    @Test
    void clearEvent_allDetails_clearsFieldsResetsStatusAndPreservesEventRelationships() throws ClubFlowException {
        Club club = new Club();
        CommandParser parser = createEventCommandParser(club);
        Event original = new Event("XMAS", "Party", LocalDate.of(2026, 12, 25),
                LocalDate.of(2026, 12, 20), 500, "Hall", "Chairs", true);
        original.loadPhoto("poster");
        club.addEvent(original);
        Event other = new Event("OTHER", "Meeting", null, null, 0, "", "");
        club.addEvent(other);
        Member member = new Member("Alice", "alice", "", List.of());
        club.addMember(member);
        club.assignMemberToEvent("XMAS", member, "Host");
        club.markSaved();

        assertFalse(parser.parse("CLEAREVENT E/xmas"));

        Event cleared = club.findEventByTag("XMAS").orElseThrow();
        assertEquals("XMAS", cleared.getTag());
        assertEquals("", cleared.getName());
        assertNull(cleared.getDate());
        assertNull(cleared.getDeadline());
        assertEquals(0, cleared.getBudget());
        assertEquals("", cleared.getVenue());
        assertEquals("", cleared.getLogistics());
        assertFalse(cleared.isCompleted());
        assertEquals(List.of("poster"), cleared.getPhotoNames());
        assertEquals(List.of(cleared, other), club.getEvents());
        assertSame(cleared, club.getAssignmentsForEvent("XMAS").getFirst().getEvent());
        assertSame(member, club.getAssignmentsForEvent("XMAS").getFirst().getMember());
        assertEquals("Host", club.getAssignmentsForEvent("XMAS").getFirst().getRole());
        assertTrue(club.hasUnsavedChanges());
        assertEquals(List.of("[TEST] Cleared event details: XMAS"), printedLines());
    }

    @Test
    void clearEvent_invalidRequests_leaveExistingEventsUnchanged() {
        Club club = new Club();
        CommandParser parser = createEventCommandParser(club);
        Event original = new Event("XMAS", "Party", null, null, 500, "Hall", "Chairs");
        club.addEvent(original);
        club.markSaved();

        for (String input : List.of("clearEvent", "clearEvent e/", "clearEvent e/\"   \"",
                "clearEvent e/XMAS e/XMAS", "clearEvent e/XMAS n/New", "clearEvent all")) {
            assertThrows(CommandParseException.class, () -> parser.parse(input), input);
        }
        EventNotFoundException error = assertThrows(EventNotFoundException.class,
                () -> parser.parse("clearEvent e/UNKNOWN"));

        assertEquals("Event not found: UNKNOWN", error.getMessage());
        assertEquals(List.of(original), club.getEvents());
        assertFalse(club.hasUnsavedChanges());
        assertEquals(List.of(), printedLines());
    }

    @Test
    void clearEvent_alreadyEmptyEvent_canBeClearedRepeatedly() throws ClubFlowException {
        Club club = new Club();
        CommandParser parser = createEventCommandParser(club);
        club.addEvent(new Event("DRAFT", "", null, null, 0, "", ""));

        assertFalse(parser.parse("clearEvent e/DRAFT"));
        assertFalse(parser.parse("clearEvent e/DRAFT"));

        Event cleared = club.findEventByTag("DRAFT").orElseThrow();
        assertFalse(cleared.isCompleted());
        assertEquals(1, club.getEvents().size());
        assertEquals(List.of("[TEST] Cleared event details: DRAFT", "[TEST] Cleared event details: DRAFT"),
                printedLines());
    }

    /**
     * Creates a parser whose event commands all share the supplied data store.
     *
     * @param club data store shared by the event commands
     * @return parser configured with create, view, delete, and clear event commands
     */
    private CommandParser createEventCommandParser(Club club) {
        UserInterface ui = new UserInterface("[TEST] ", "> ");
        CommandParser parser = new CommandParser(ui);
        parser.register("createEvent", new CreateEventCommand(ui, club));
        parser.register("viewEvent", new ViewEventCommand(ui, club));
        parser.register("deleteEvent", new DeleteEventCommand(ui, club));
        parser.register("clearEvent", new ClearEventCommand(ui, club));
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
