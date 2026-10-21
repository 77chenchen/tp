package clubflow.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import clubflow.ClubData;
import clubflow.UserInterface;
import clubflow.event.Event;
import clubflow.exception.CommandParseException;

/**
 * Checks the event details and messages displayed through the command parser.
 */
class ViewEventCommandTest {
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
        parser.register("viewEvent", new ViewEventCommand(ui, clubData));
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOutput);
        capturedOutput.close();
    }

    @Test
    void viewEvent_matchingTag_printsOnlySelectedEventDetails() throws CommandParseException {
        clubData.addEvent(christmasParty());
        clubData.addEvent(new Event("OTHER", "Other event", null, null, 0, "", ""));

        assertFalse(parser.parse("viewEvent e/xmas"));

        assertEquals(List.of(
                "[TEST] [XMAS] Christmas Party",
                "[TEST]   Status:    Not completed",
                "[TEST]   Date:      2026-12-25",
                "[TEST]   Deadline:  2026-12-20",
                "[TEST]   Budget:    $500.00",
                "[TEST]   Venue:     Main Hall",
                "[TEST]   Logistics: Tables and chairs"
        ), printedLines());
    }

    @Test
    void viewEvent_missingDetails_printsPlaceholdersAndZeroBudget() throws CommandParseException {
        // Both empty strings and whitespace count as unspecified text.
        clubData.addEvent(new Event("DRAFT", "", null, null, 0, "   ", ""));

        assertFalse(parser.parse("viewEvent e/DRAFT"));

        assertEquals(List.of(
                "[TEST] [DRAFT] Not set",
                "[TEST]   Status:    Not completed",
                "[TEST]   Date:      Not set",
                "[TEST]   Deadline:  Not set",
                "[TEST]   Budget:    $0.00",
                "[TEST]   Venue:     Not set",
                "[TEST]   Logistics: Not set"
        ), printedLines());
    }

    @Test
    void viewEvent_allEvents_printsEveryEventInInsertionOrder() throws CommandParseException {
        clubData.addEvent(christmasParty().withCompleted(true));
        clubData.addEvent(new Event("DRAFT", "", null, null, 0, "", ""));

        assertFalse(parser.parse("viewEvent all"));

        assertEquals(List.of(
                "[TEST] Displaying all events.",
                "[TEST] [XMAS] Christmas Party",
                "[TEST]   Status:    Completed",
                "[TEST]   Date:      2026-12-25",
                "[TEST]   Deadline:  2026-12-20",
                "[TEST]   Budget:    $500.00",
                "[TEST]   Venue:     Main Hall",
                "[TEST]   Logistics: Tables and chairs",
                "[TEST] [DRAFT] Not set",
                "[TEST]   Status:    Not completed",
                "[TEST]   Date:      Not set",
                "[TEST]   Deadline:  Not set",
                "[TEST]   Budget:    $0.00",
                "[TEST]   Venue:     Not set",
                "[TEST]   Logistics: Not set"
        ), printedLines());
    }

    @Test
    void viewEvent_noEvents_printsEmptyMessage() throws CommandParseException {
        assertFalse(parser.parse("viewEvent all"));

        assertEquals(List.of("[TEST] No events found."), printedLines());
    }

    @Test
    void viewEvent_unknownTag_printsNotFoundMessage() throws CommandParseException {
        clubData.addEvent(christmasParty());

        assertFalse(parser.parse("viewEvent e/UNKNOWN"));

        assertEquals(List.of("[TEST] Event not found."), printedLines());
    }

    @Test
    void viewEvent_nonEnglishLocale_keepsBudgetDecimalPoint() throws CommandParseException {
        clubData.addEvent(new Event("MEETING", "Meeting", null, null, 12.5, "", ""));
        Locale originalLocale = Locale.getDefault(Locale.Category.FORMAT);
        try {
            // German formatting normally uses a comma as its decimal separator.
            Locale.setDefault(Locale.Category.FORMAT, Locale.GERMANY);

            assertFalse(parser.parse("viewEvent e/MEETING"));

            assertTrue(printedLines().contains("[TEST]   Budget:    $12.50"));
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, originalLocale);
        }
    }

    /**
     * Creates an event with every optional detail supplied.
     *
     * @return complete event used to check the displayed field values
     */
    private Event christmasParty() {
        return new Event("XMAS", "Christmas Party", LocalDate.of(2026, 12, 25),
                LocalDate.of(2026, 12, 20), 500, "Main Hall", "Tables and chairs");
    }

    /**
     * Returns captured output as lines so assertions work with Windows and Unix line endings.
     *
     * @return lines printed by the user interface, including its prefix
     */
    private List<String> printedLines() {
        return output.toString(StandardCharsets.UTF_8).lines().toList();
    }
}
