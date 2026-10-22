package clubflow.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import clubflow.ClubData;
import clubflow.UserInterface;
import clubflow.event.Event;
import clubflow.exception.ClubFlowException;
import clubflow.exception.CommandParseException;
import clubflow.search.EventSearch;

/**
 * Checks search dispatch, argument validation, and the supported event search through the parser.
 */
class FindCommandTest {
    private final PrintStream originalOutput = System.out;
    private ByteArrayOutputStream output;
    private PrintStream capturedOutput;
    private ClubData clubData;
    private CommandParser parser;

    @BeforeEach
    void setUp() {
        output = new ByteArrayOutputStream();
        capturedOutput = new PrintStream(output, true, StandardCharsets.UTF_8);
        System.setOut(capturedOutput);
        clubData = new ClubData();
        UserInterface ui = new UserInterface("[TEST] ", "> ");
        parser = new CommandParser(ui);
        EventSearch eventSearch = new EventSearch(ui, clubData);
        parser.register("find", new FindCommand(ui, Map.of("e", eventSearch::search)));
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOutput);
        capturedOutput.close();
    }

    @Test
    void find_customSearchType_dispatchesTrimmedKeywordToSelectedHandler() throws ClubFlowException {
        List<String> firstKeywords = new ArrayList<>();
        List<String> secondKeywords = new ArrayList<>();
        UserInterface ui = new UserInterface("[TEST] ", "> ");
        CommandParser customParser = new CommandParser(ui);
        customParser.register("find", new FindCommand(ui, Map.of(
                "x", firstKeywords::add, "y", secondKeywords::add)));

        assertFalse(customParser.parse("find y/\"  Mixed Case  \""));

        assertEquals(List.of(), firstKeywords);
        assertEquals(List.of("Mixed Case"), secondKeywords);
    }

    @Test
    void find_multipleSupportedTypes_rejectsRequestBeforeCallingHandlers() {
        List<String> keywords = new ArrayList<>();
        UserInterface ui = new UserInterface("[TEST] ", "> ");
        CommandParser customParser = new CommandParser(ui);
        customParser.register("find", new FindCommand(ui, Map.of(
                "x", keywords::add, "y", keywords::add)));

        assertThrows(CommandParseException.class, () -> customParser.parse("find x/one y/two"));
        assertThrows(CommandParseException.class, () -> customParser.parse("find"));
        assertThrows(CommandParseException.class, () -> customParser.parse("find y/\"   \""));

        assertEquals(List.of(), keywords);
    }

    @Test
    void find_changedHandlerMap_keepsRegisteredSearchTypes() throws ClubFlowException {
        List<String> keywords = new ArrayList<>();
        Map<String, Consumer<String>> handlers = new HashMap<>();
        handlers.put("x", keywords::add);
        UserInterface ui = new UserInterface("[TEST] ", "> ");
        CommandParser customParser = new CommandParser(ui);
        customParser.register("find", new FindCommand(ui, handlers));
        handlers.clear();

        assertFalse(customParser.parse("find x/keyword"));

        assertEquals(List.of("keyword"), keywords);
    }

    @Test
    void find_partialTag_ignoresCaseAndHandlesMissingName() throws ClubFlowException {
        clubData.addEvent(new Event("XMAS", "", null, null, 0, "", ""));
        clubData.addEvent(new Event("OTHER", "Other event", null, null, 0, "", ""));

        assertFalse(parser.parse("find e/mAs"));

        assertEquals(List.of("[TEST] Found 1 matching event(s).", "[TEST] [XMAS] Not set"), printedLines());
    }

    @Test
    void find_partialName_returnsEachMatchOnceInInsertionOrder() throws ClubFlowException {
        clubData.addEvent(new Event("PARTY", "Christmas Party", null, null, 0, "", ""));
        clubData.addEvent(new Event("AGM", "Annual meeting", null, null, 0, "", ""));
        clubData.addEvent(new Event("NYE", "New Year Party", null, null, 0, "", ""));
        List<Event> originalEvents = clubData.getEvents();

        assertFalse(parser.parse("find e/pArT"));

        assertEquals(List.of(
                "[TEST] Found 2 matching event(s).",
                "[TEST] [PARTY] Christmas Party",
                "[TEST] [NYE] New Year Party"
        ), printedLines());
        assertEquals(originalEvents, clubData.getEvents());
    }

    @Test
    void find_quotedPhrase_matchesContiguousTextAndTrimsOuterWhitespace() throws ClubFlowException {
        clubData.addEvent(new Event("XMAS", "Christmas Party", null, null, 0, "", ""));
        clubData.addEvent(new Event("OTHER", "Christmas Dinner Party", null, null, 0, "", ""));

        assertFalse(parser.parse("find e/\"  christmas party  \""));

        assertEquals(List.of("[TEST] Found 1 matching event(s).", "[TEST] [XMAS] Christmas Party"), printedLines());
    }

    @Test
    void find_noMatch_printsNotFoundMessage() throws ClubFlowException {
        clubData.addEvent(new Event("AGM", "Meeting", null, null, 0, "Party Hall", "party supplies"));

        assertFalse(parser.parse("find e/party"));

        assertEquals(List.of("[TEST] No matching events found."), printedLines());
    }

    @Test
    void find_emptyStore_printsNotFoundMessage() throws ClubFlowException {
        assertFalse(parser.parse("find e/party"));

        assertEquals(List.of("[TEST] No matching events found."), printedLines());
    }

    @Test
    void find_changedStore_usesCurrentEvents() throws ClubFlowException {
        clubData.addEvent(new Event("OLD", "Party", null, null, 0, "", ""));
        parser.parse("find e/party");
        output.reset();
        clubData.removeEventByTag("OLD");
        clubData.addEvent(new Event("NEW", "Party", null, null, 0, "", ""));

        assertFalse(parser.parse("find e/party"));

        assertEquals(List.of("[TEST] Found 1 matching event(s).", "[TEST] [NEW] Party"), printedLines());
    }

    @Test
    void find_turkishLocale_matchesUsingLocaleIndependentCase() throws ClubFlowException {
        clubData.addEvent(new Event("MEETING", "", null, null, 0, "", ""));
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));

            assertFalse(parser.parse("FIND E/meeting"));

            assertEquals(List.of("[TEST] Found 1 matching event(s).", "[TEST] [MEETING] Not set"), printedLines());
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    void find_missingOrBlankKeyword_rejectsInputWithoutSearching() {
        for (String input : List.of("find", "find e/", "find e/\"\"", "find e/\"   \"")) {
            assertThrows(CommandParseException.class, () -> parser.parse(input), input);
        }

        assertEquals(List.of(), printedLines());
    }

    @Test
    void find_unsupportedOrDuplicateArguments_rejectsInputWithoutSearching() {
        for (String input : List.of("find m/Alice", "find all", "find e/party m/Alice", "find e/party e/meeting")) {
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
