package clubflow.command.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import clubflow.Club;
import clubflow.UserInterface;
import clubflow.command.CommandParser;
import clubflow.event.Event;
import clubflow.exception.ClubFlowException;
import clubflow.exception.CommandParseException;

//@@author camimaigrot
/**
 * Tests date-range searches and the upcoming events listing through the command parser.
 */
class EventDateFeaturesTest {
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
        parser.register("upcomingEvents", new UpcomingEventsCommand(ui, club));
        parser.registerAlias("upcomingEvents", "ue");
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOutput);
        capturedOutput.close();
    }

    @Test
    void dateRange_includesBothEndpointsAndExcludesMissingDates() throws ClubFlowException {
        add("BEFORE", LocalDate.of(2026, 10, 31));
        add("START", LocalDate.of(2026, 11, 1));
        add("MIDDLE", LocalDate.of(2026, 11, 15));
        add("END", LocalDate.of(2026, 11, 30));
        add("AFTER", LocalDate.of(2026, 12, 1));
        add("UNDATED", null);
        club.markSaved();

        assertFalse(parser.parse("fe from/2026-11-01 to/2026-11-30"));

        assertEquals(List.of(
                "[TEST] Found 3 matching event(s).",
                "[TEST] [START] START",
                "[TEST] [MIDDLE] MIDDLE",
                "[TEST] [END] END"), lines());
        assertFalse(club.hasUnsavedChanges());
    }

    @Test
    void dateRange_oneSidedBoundsAndSingleDayWork() throws ClubFlowException {
        add("BEFORE", LocalDate.of(2026, 11, 1));
        add("EXACT", LocalDate.of(2026, 11, 15));
        add("AFTER", LocalDate.of(2026, 12, 1));
        add("NONE", null);

        assertSearch("fe from/2026-11-15", "EXACT", "AFTER");
        assertSearch("fe to/2026-11-15", "BEFORE", "EXACT");
        assertSearch("fe from/2026-11-15 to/2026-11-15", "EXACT");
        assertSearch("fe d/2026-11-15", "EXACT");
    }

    @Test
    void dateRange_combinesWithOtherSearchFilters() throws ClubFlowException {
        club.addEvent(new Event("ONE", "Party", LocalDate.of(2026, 11, 5), null, 100, "Hall", ""));
        club.addEvent(new Event("TWO", "Meeting", LocalDate.of(2026, 11, 5), null, 100, "Hall", ""));
        club.addEvent(new Event("THREE", "Party", LocalDate.of(2026, 12, 5), null, 100, "Hall", ""));

        assertSearch("fe from/2026-11-01 to/2026-11-30 n/party b/100", "ONE");
    }

    @Test
    void dateRange_rejectsInvalidBoundsBeforeSearching() {
        for (String input : List.of(
                "fe from/2026-11-31", "fe to/2026-02-29", "fe from/not-a-date",
                "fe from/2026-12-01 to/2026-11-30",
                "fe d/2026-11-15 from/2026-11-16",
                "fe d/2026-11-15 to/2026-11-14",
                "fe from/", "fe to/", "fe from/2026-11-01 from/2026-11-02")) {
            assertThrows(CommandParseException.class, () -> parser.parse(input), input);
        }
        assertEquals(List.of(), lines());
    }

    @Test
    void upcomingEvents_filtersAndSortsWithoutMutatingClub() throws ClubFlowException {
        LocalDate today = LocalDate.now();
        add("LATER", today.plusDays(10));
        add("PAST", today.minusDays(1));
        add("TODAY", today);
        add("LATEB", today.plusDays(2));
        add("LATEA", today.plusDays(2));
        add("UNDATED", null);
        club.addEvent(new Event("DONE", "DONE", today.plusDays(3), null, 0, "", "", true));
        club.markSaved();
        List<Event> originalEvents = club.getEvents();

        assertFalse(parser.parse("upcomingEvents"));

        assertEquals(List.of(
                "[TEST] Upcoming events (4):",
                "[TEST] [TODAY] TODAY (" + today + ")",
                "[TEST] [LATEA] LATEA (" + today.plusDays(2) + ")",
                "[TEST] [LATEB] LATEB (" + today.plusDays(2) + ")",
                "[TEST] [LATER] LATER (" + today.plusDays(10) + ")"), lines());
        assertEquals(originalEvents, club.getEvents());
        assertFalse(club.hasUnsavedChanges());
    }

    @Test
    void upcomingEvents_aliasReturnsSameResultAndUsesUpdatedStore() throws ClubFlowException {
        add("FIRST", LocalDate.now().plusDays(1));
        assertFalse(parser.parse("ue"));
        List<String> expected = lines();
        output.reset();
        assertFalse(parser.parse("upcomingEvents"));
        assertEquals(expected, lines());

        club.removeEventByTag("FIRST");
        output.reset();
        assertFalse(parser.parse("ue"));
        assertEquals(List.of("[TEST] No upcoming events found."), lines());
    }

    @Test
    void upcomingEvents_emptyOrOnlyIneligibleEvents_printsEmptyMessage() throws ClubFlowException {
        assertFalse(parser.parse("ue"));
        assertEquals(List.of("[TEST] No upcoming events found."), lines());
        output.reset();
        add("PAST", LocalDate.now().minusDays(1));
        add("UNDATED", null);
        assertFalse(parser.parse("upcomingEvents"));
        assertEquals(List.of("[TEST] No upcoming events found."), lines());
    }

    @Test
    void upcomingEvents_rejectsUnexpectedArguments() {
        for (String input : List.of("ue d/2026-11-02", "upcomingEvents all", "ue from/2026-11-02")) {
            assertThrows(CommandParseException.class, () -> parser.parse(input), input);
        }
        UpcomingEventsCommand command = new UpcomingEventsCommand(new UserInterface("[TEST] ", "> "), club);
        assertThrows(NullPointerException.class, () -> command.execute(null));
        assertEquals(List.of(), lines());
    }

    private void add(String tag, LocalDate date) {
        club.addEvent(new Event(tag, tag, date, null, 0, "", ""));
    }

    private void assertSearch(String command, String... tags) throws ClubFlowException {
        output.reset();
        assertFalse(parser.parse(command));
        List<String> expected = new java.util.ArrayList<>();
        expected.add("[TEST] Found " + tags.length + " matching event(s).");
        for (String tag : tags) {
            expected.add("[TEST] [" + tag + "] " + club.findEventByTag(tag).orElseThrow().getName());
        }
        assertEquals(expected, lines(), command);
    }

    private List<String> lines() {
        return output.toString(StandardCharsets.UTF_8).lines().toList();
    }
}
//@@author
