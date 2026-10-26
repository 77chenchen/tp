package clubflow.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import clubflow.Club;
import clubflow.UserInterface;
import clubflow.event.Event;
import clubflow.exception.ClubFlowException;
import clubflow.exception.CommandParseException;
import clubflow.search.EventSearch;

//@@author Eugene3129
/**
 * Checks event search matching and argument validation through the parser.
 */
class FindEventCommandTest {
    private final PrintStream originalOutput = System.out;
    private ByteArrayOutputStream output;
    private PrintStream capturedOutput;
    private Club club;
    private CommandParser parser;

    @BeforeEach
    void setUp() {
        output = new ByteArrayOutputStream();
        capturedOutput = new PrintStream(output, true, StandardCharsets.UTF_8);
        System.setOut(capturedOutput);
        club = new Club();
        UserInterface ui = new UserInterface("[TEST] ", "> ");
        parser = new CommandParser(ui);
        parser.register("findEvent", new FindEventCommand(ui, club));
        parser.registerAlias("findEvent", "fe");
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOutput);
        capturedOutput.close();
    }

    @Test
    void findEvent_shortAlias_returnsSameMatches() throws ClubFlowException {
        club.addEvent(new Event("XMAS", "Christmas Party", null, null, 0, "", ""));

        assertFalse(parser.parse("findEvent e/party"));
        List<String> expected = printedLines();
        output.reset();
        assertFalse(parser.parse("fe e/party"));

        assertEquals(expected, printedLines());
    }

    @Test
    void findEvent_partialTag_ignoresCaseAndHandlesMissingName() throws ClubFlowException {
        club.addEvent(new Event("XMAS", "", null, null, 0, "", ""));
        club.addEvent(new Event("OTHER", "Other event", null, null, 0, "", ""));

        assertFalse(parser.parse("findEvent e/mAs"));

        assertEquals(List.of("[TEST] Found 1 matching event(s).", "[TEST] [XMAS] Not set"), printedLines());
    }

    @Test
    void findEvent_partialName_returnsEachMatchOnceInInsertionOrder() throws ClubFlowException {
        club.addEvent(new Event("PARTY", "Christmas Party", null, null, 0, "", ""));
        club.addEvent(new Event("AGM", "Annual meeting", null, null, 0, "", ""));
        club.addEvent(new Event("NYE", "New Year Party", null, null, 0, "", ""));
        List<Event> originalEvents = club.getEvents();
        club.markSaved();

        assertFalse(parser.parse("findEvent e/pArT"));

        assertEquals(List.of(
                "[TEST] Found 2 matching event(s).",
                "[TEST] [PARTY] Christmas Party",
                "[TEST] [NYE] New Year Party"
        ), printedLines());
        assertEquals(originalEvents, club.getEvents());
        assertFalse(club.hasUnsavedChanges());
    }

    @Test
    void findEvent_quotedPhrase_matchesContiguousTextAndTrimsOuterWhitespace() throws ClubFlowException {
        club.addEvent(new Event("XMAS", "Christmas Party", null, null, 0, "", ""));
        club.addEvent(new Event("OTHER", "Christmas Dinner Party", null, null, 0, "", ""));

        assertFalse(parser.parse("findEvent e/\"  christmas party  \""));

        assertEquals(List.of("[TEST] Found 1 matching event(s).", "[TEST] [XMAS] Christmas Party"), printedLines());
    }

    @Test
    void findEvent_noMatch_printsNotFoundMessage() throws ClubFlowException {
        club.addEvent(new Event("AGM", "Meeting", null, null, 0, "Party Hall", "party supplies"));

        assertFalse(parser.parse("findEvent e/party"));

        assertEquals(List.of("[TEST] No matching events found."), printedLines());
    }

    @Test
    void findEvent_emptyStore_printsNotFoundMessage() throws ClubFlowException {
        assertFalse(parser.parse("findEvent e/party"));

        assertEquals(List.of("[TEST] No matching events found."), printedLines());
    }

    @Test
    void findEvent_changedStore_usesCurrentEvents() throws ClubFlowException {
        club.addEvent(new Event("OLD", "Party", null, null, 0, "", ""));
        parser.parse("findEvent e/party");
        output.reset();
        club.removeEventByTag("OLD");
        club.addEvent(new Event("NEW", "Party", null, null, 0, "", ""));

        assertFalse(parser.parse("findEvent e/party"));

        assertEquals(List.of("[TEST] Found 1 matching event(s).", "[TEST] [NEW] Party"), printedLines());
    }

    @Test
    void findEvent_turkishLocale_matchesUsingLocaleIndependentCase() throws ClubFlowException {
        club.addEvent(new Event("MEETING", "", null, null, 0, "", ""));
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));

            assertFalse(parser.parse("FINDEVENT E/meeting"));

            assertEquals(List.of("[TEST] Found 1 matching event(s).", "[TEST] [MEETING] Not set"), printedLines());
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    void findEvent_textFilterWithoutTag_matchesSelectedField() throws ClubFlowException {
        club.addEvent(new Event("XMAS", "Christmas Party", null, null, 0, "Main Hall", "Chairs"));
        club.addEvent(new Event("OTHER", "Meeting", null, null, 0, "Office", "Projector"));

        for (String filter : List.of("n/\"  christmas party  \"", "v/\" MAIN HALL \"", "l/cHaIrS",
                "q/xMaS", "q/christmas", "q/hall", "q/chairs")) {
            output.reset();
            assertFalse(parser.parse("fe " + filter));

            assertEquals(List.of("[TEST] Found 1 matching event(s).", "[TEST] [XMAS] Christmas Party"),
                    printedLines(), filter);
        }
    }

    @Test
    void findEvent_nameFilter_doesNotMatchTagOrOtherFields() throws ClubFlowException {
        club.addEvent(new Event("PARTY", "Meeting", null, null, 0, "Party Hall", "Party supplies"));
        club.addEvent(new Event("OTHER", "Birthday Party", null, null, 0, "", ""));

        assertFalse(parser.parse("fe n/party"));

        assertEquals(List.of("[TEST] Found 1 matching event(s).", "[TEST] [OTHER] Birthday Party"), printedLines());
    }

    @Test
    void findEvent_dateFilters_matchExactDatesAndSkipUnsetDates() throws ClubFlowException {
        club.addEvent(new Event("XMAS", "Party", LocalDate.of(2026, 12, 25),
                LocalDate.of(2026, 12, 1), 0, "", ""));
        club.addEvent(new Event("OTHER", "Meeting", LocalDate.of(2026, 12, 26),
                LocalDate.of(2026, 12, 2), 0, "", ""));
        club.addEvent(new Event("DRAFT", "Draft", null, null, 0, "", ""));

        for (String filter : List.of("d/2026-12-25", "by/2026-12-01", "d/\" 2026-12-25 \" by/2026-12-01")) {
            output.reset();
            assertFalse(parser.parse("fe " + filter));

            assertEquals(List.of("[TEST] Found 1 matching event(s).", "[TEST] [XMAS] Party"), printedLines(), filter);
        }
    }

    @Test
    void findEvent_combinedFilters_requireEveryMatchWithoutChangingEvents() throws ClubFlowException {
        LocalDate date = LocalDate.of(2026, 12, 25);
        LocalDate deadline = LocalDate.of(2026, 12, 1);
        club.addEvent(new Event("MATCH", "Party", date, deadline, 0, "Main Hall", "Chairs"));
        club.addEvent(new Event("VENUE", "Party", date, deadline, 0, "Office", "Chairs"));
        club.addEvent(new Event("DATE", "Party", date.plusDays(1), deadline, 0, "Main Hall", "Chairs"));
        club.addEvent(new Event("LOGISTICS", "Party", date, deadline, 0, "Main Hall", "Projector"));
        List<Event> originalEvents = club.getEvents();
        club.markSaved();

        assertFalse(parser.parse("fe q/party n/party v/hall l/chairs d/2026-12-25 by/2026-12-01"));

        assertEquals(List.of("[TEST] Found 1 matching event(s).", "[TEST] [MATCH] Party"), printedLines());
        assertEquals(originalEvents, club.getEvents());
        assertFalse(club.hasUnsavedChanges());
        output.reset();

        assertFalse(parser.parse("fe e/party v/office"));

        assertEquals(List.of("[TEST] Found 1 matching event(s).", "[TEST] [VENUE] Party"), printedLines());
    }

    @Test
    void findEvent_blankFilter_rejectsEvenWhenAnotherFilterIsValid() {
        for (String id : List.of("e", "q", "n", "v", "l", "d", "by")) {
            String otherFilter = id.equals("e") ? "q/party" : "e/party";
            for (String value : List.of("", "\"\"", "\"   \"")) {
                assertThrows(CommandParseException.class, () -> parser.parse("fe " + id + "/" + value));
                assertThrows(CommandParseException.class,
                        () -> parser.parse("fe " + otherFilter + " " + id + "/" + value));
            }
        }

        assertEquals(List.of(), printedLines());
    }

    @Test
    void findEvent_invalidDates_rejectsEvenWhenStoreIsEmpty() {
        for (String filter : List.of("d/2026-02-29", "by/2026-13-01", "d/25-12-2026", "by/not-a-date")) {
            assertThrows(CommandParseException.class, () -> parser.parse("fe " + filter), filter);
        }

        assertEquals(List.of(), printedLines());
    }

    @Test
    void findEvent_directCalls_rejectNullArgumentsAndValuesAndEmptyFilters() {
        UserInterface ui = new UserInterface("[TEST] ", "> ");
        FindEventCommand command = new FindEventCommand(ui, club);
        EventSearch search = new EventSearch(ui, club);
        HashMap<String, String> args = new HashMap<>();

        assertThrows(NullPointerException.class, () -> command.validateArgs(null));
        assertThrows(NullPointerException.class, () -> command.execute(null));
        assertThrows(NullPointerException.class, () -> search.search(null));
        assertThrows(CommandParseException.class, () -> command.validateArgs(args));
        assertThrows(CommandParseException.class, () -> command.execute(args));
        assertThrows(IllegalArgumentException.class, () -> search.search(args));

        args.put("v", null);
        assertThrows(NullPointerException.class, () -> command.validateArgs(args));
        assertThrows(NullPointerException.class, () -> command.execute(args));
        assertThrows(NullPointerException.class, () -> search.search(args));
        args.clear();
        args.put(null, "hall");
        assertThrows(NullPointerException.class, () -> command.validateArgs(args));
        assertThrows(NullPointerException.class, () -> command.execute(args));
        assertThrows(NullPointerException.class, () -> search.search(args));

        assertEquals(List.of(), printedLines());
    }

    @Test
    void findEvent_missingOrBlankKeyword_rejectsInputWithoutSearching() {
        for (String input : List.of("findEvent", "findEvent e/", "findEvent e/\"\"", "findEvent e/\"   \"")) {
            assertThrows(CommandParseException.class, () -> parser.parse(input), input);
        }

        assertEquals(List.of(), printedLines());
    }

    @Test
    void findEvent_unsupportedOrDuplicateArguments_rejectsInputWithoutSearching() {
        for (String input : List.of("findEvent m/Alice", "findEvent all",
                "findEvent e/party m/Alice", "findEvent e/party e/meeting", "fe v/hall v/office",
                "fe q/party q/meeting", "fe b/100", "fe completed/true")) {
            assertThrows(CommandParseException.class, () -> parser.parse(input), input);
        }

        assertEquals(List.of(), printedLines());
    }

    /**
     * Returns captured output as lines independent of the operating system's line endings.
     *
     * @return lines printed by the user interface
     */
    private List<String> printedLines() {
        return output.toString(StandardCharsets.UTF_8).lines().toList();
    }
}
