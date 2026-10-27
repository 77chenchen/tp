package clubflow.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import clubflow.Club;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import clubflow.UserInterface;
import clubflow.completion.CompletionHandler;
import clubflow.completion.EventCompletion;
import clubflow.event.Event;
import clubflow.exception.ClubFlowException;
import clubflow.exception.CommandParseException;
import clubflow.exception.EventNotFoundException;

//@@author Eugene3129
/**
 * Tests completion dispatch, event updates, idempotent requests, and invalid input through the parser.
 */
class CompletionCommandTest {
    private final PrintStream originalOutput = System.out;
    private ByteArrayOutputStream output;
    private PrintStream capturedOutput;
    private UserInterface ui;
    private Club club;
    private CommandParser parser;
    private Event originalEvent;

    @BeforeEach
    void setUp() {
        output = new ByteArrayOutputStream();
        capturedOutput = new PrintStream(output, true, StandardCharsets.UTF_8);
        System.setOut(capturedOutput);
        ui = new UserInterface("[TEST] ", "> ");
        club = new Club();
        originalEvent = new Event("XMAS", "Christmas Party", LocalDate.of(2026, 12, 25),
                LocalDate.of(2026, 12, 20), 500, "Main Hall", "Tables and chairs");
        club.addEvent(originalEvent);
        EventCompletion eventCompletion = new EventCompletion(ui, club);
        Map<String, CompletionHandler> handlers = Map.of("e", eventCompletion::setCompleted);
        parser = new CommandParser(ui);
        parser.register("mark", new CompletionCommand(ui, handlers, true));
        parser.register("unmark", new CompletionCommand(ui, handlers, false));
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOutput);
        capturedOutput.close();
    }

    @Test
    void mark_caseInsensitiveTag_preservesDetailsOrderAndSnapshots() throws ClubFlowException {
        Event other = new Event("OTHER", "Other event", null, null, 0, "", "");
        club.addEvent(other);
        List<Event> snapshot = club.getEvents();

        assertFalse(parser.parse("MARK E/xmas"));

        Event marked = club.getEvents().getFirst();
        assertTrue(marked.isCompleted());
        assertEquals("XMAS", marked.getTag());
        assertEquals(originalEvent.getName(), marked.getName());
        assertEquals(originalEvent.getDate(), marked.getDate());
        assertEquals(originalEvent.getDeadline(), marked.getDeadline());
        assertEquals(originalEvent.getBudget(), marked.getBudget());
        assertEquals(originalEvent.getVenue(), marked.getVenue());
        assertEquals(originalEvent.getLogistics(), marked.getLogistics());
        assertEquals(2, club.getEvents().size());
        assertSame(other, club.getEvents().get(1));
        assertEquals(List.of(originalEvent, other), snapshot);
        assertFalse(snapshot.getFirst().isCompleted());
        assertEquals(List.of("[TEST] Marked event as completed: XMAS"), printedLines());
    }

    @Test
    void unmark_completedEvent_setsNotCompleted() throws ClubFlowException {
        parser.parse("mark e/XMAS");
        output.reset();

        assertFalse(parser.parse("UNMARK E/xmas"));

        Event unmarked = club.findEventByTag("XMAS").orElseThrow();
        assertFalse(unmarked.isCompleted());
        assertEquals(originalEvent.getName(), unmarked.getName());
        assertEquals(originalEvent.getDate(), unmarked.getDate());
        assertEquals(List.of("[TEST] Marked event as not completed: XMAS"), printedLines());
    }

    @Test
    void completion_repeatedCommands_keepRequestedState() throws ClubFlowException {
        parser.parse("mark e/XMAS");
        Event marked = club.findEventByTag("XMAS").orElseThrow();

        parser.parse("mark e/XMAS");

        assertSame(marked, club.findEventByTag("XMAS").orElseThrow());
        assertTrue(marked.isCompleted());
        parser.parse("unmark e/XMAS");
        Event unmarked = club.findEventByTag("XMAS").orElseThrow();

        parser.parse("unmark e/XMAS");

        assertSame(unmarked, club.findEventByTag("XMAS").orElseThrow());
        assertFalse(unmarked.isCompleted());
    }

    @Test
    void completion_unknownTag_throwsWithoutChangingEventsOrPrintingErrors() {
        EventNotFoundException markError = assertThrows(EventNotFoundException.class,
                () -> parser.parse("mark e/UNKNOWN"));
        EventNotFoundException unmarkError = assertThrows(EventNotFoundException.class,
                () -> parser.parse("unmark e/UNKNOWN"));

        assertEquals("Event not found: UNKNOWN", markError.getMessage());
        assertEquals("Event not found: UNKNOWN", unmarkError.getMessage());
        assertEquals(List.of(originalEvent), club.getEvents());
        assertEquals(List.of(), printedLines());
    }

    @Test
    void completion_invalidArguments_rejectsBeforeChangingEvents() {
        for (String command : List.of("mark", "unmark")) {
            for (String arguments : List.of("", " e/", " e/\"\"", " e/\"   \"", " m/1",
                    " e/XMAS e/OTHER", " e/XMAS n/Changed", " all")) {
                String input = command + arguments;
                assertThrows(CommandParseException.class, () -> parser.parse(input), input);
            }
        }

        assertSame(originalEvent, club.findEventByTag("XMAS").orElseThrow());
        assertFalse(originalEvent.isCompleted());
        assertEquals(List.of(), printedLines());
    }

    @Test
    void completion_afterDataReplacement_usesCurrentEvents() throws ClubFlowException {
        Club loaded = new Club();
        loaded.addEvent(new Event("NEW", "Loaded event", null, null, 0, "", ""));
        club.replaceWith(loaded);

        parser.parse("mark e/NEW");

        assertTrue(club.findEventByTag("NEW").orElseThrow().isCompleted());
        assertTrue(club.findEventByTag("XMAS").isEmpty());
    }

    @Test
    void completion_customTypes_dispatchesIdentifierAndBothStates() throws ClubFlowException {
        List<Map.Entry<String, Boolean>> updates = new ArrayList<>();
        List<String> unusedUpdates = new ArrayList<>();
        Map<String, CompletionHandler> handlers = Map.of(
                "x", (identifier, completed) -> unusedUpdates.add(identifier),
                "y", (identifier, completed) -> updates.add(Map.entry(identifier, completed)));
        CommandParser customParser = new CommandParser(ui);
        customParser.register("mark", new CompletionCommand(ui, handlers, true));
        customParser.register("unmark", new CompletionCommand(ui, handlers, false));

        assertFalse(customParser.parse("mark y/\"Other Item\""));
        assertFalse(customParser.parse("unmark y/\"Other Item\""));

        assertEquals(List.of(Map.entry("Other Item", true), Map.entry("Other Item", false)), updates);
        assertEquals(List.of(), unusedUpdates);
    }

    @Test
    void completion_multipleSupportedTypes_rejectsBeforeDispatch() {
        List<String> updates = new ArrayList<>();
        Map<String, CompletionHandler> handlers = Map.of(
                "x", (identifier, completed) -> updates.add(identifier),
                "y", (identifier, completed) -> updates.add(identifier));
        CommandParser customParser = new CommandParser(ui);
        customParser.register("mark", new CompletionCommand(ui, handlers, true));

        assertThrows(CommandParseException.class, () -> customParser.parse("mark x/one y/two"));

        assertEquals(List.of(), updates);
    }

    @Test
    void completion_changedHandlerMap_keepsRegisteredTypes() throws ClubFlowException {
        List<String> updates = new ArrayList<>();
        Map<String, CompletionHandler> handlers = new HashMap<>();
        handlers.put("x", (identifier, completed) -> updates.add(identifier));
        CommandParser customParser = new CommandParser(ui);
        customParser.register("mark", new CompletionCommand(ui, handlers, true));
        handlers.clear();

        customParser.parse("mark x/item");

        assertEquals(List.of("item"), updates);
        assertThrows(IllegalArgumentException.class, () -> new CompletionCommand(ui, Map.of(), true));
    }

    @Test
    void completion_handlerFailure_propagatesApplicationExceptionUnchanged() {
        ClubFlowException failure = new EventNotFoundException("item");
        CompletionHandler handler = (identifier, completed) -> {
            throw failure;
        };
        CommandParser customParser = new CommandParser(ui);
        customParser.register("mark", new CompletionCommand(ui, Map.of("x", handler), true));

        ClubFlowException actual = assertThrows(ClubFlowException.class, () -> customParser.parse("mark x/item"));

        assertSame(failure, actual);
        assertEquals(List.of(), printedLines());
    }

    /**
     * Returns captured output without depending on platform line endings.
     *
     * @return lines printed by completion handlers
     */
    private List<String> printedLines() {
        return output.toString(StandardCharsets.UTF_8).lines().toList();
    }
}
