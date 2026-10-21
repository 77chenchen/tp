package clubflow;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises application startup and command dispatch using complete terminal sessions.
 */
class ClubFlowTest {
    private final InputStream originalInput = System.in;
    private final PrintStream originalOutput = System.out;
    private ByteArrayOutputStream output;
    private PrintStream capturedOutput;

    @BeforeEach
    void captureOutput() {
        output = new ByteArrayOutputStream();
        capturedOutput = new PrintStream(output, true, StandardCharsets.UTF_8);
        System.setOut(capturedOutput);
    }

    @AfterEach
    void restoreStreams() {
        System.setIn(originalInput);
        System.setOut(originalOutput);
        capturedOutput.close();
    }

    @Test
    void run_exit_printsWelcomeAndIgnoresLaterCommands() {
        String result = runSession("exit\ncreateEvent e/IGNORED\n");

        assertAll(
                () -> assertTrue(result.contains("Welcome to ClubFlow!")),
                () -> assertTrue(result.contains("[CF] Goodbye!")),
                () -> assertFalse(result.contains("Created event:")),
                () -> assertEquals(1, result.lines().filter(line -> line.contains("Goodbye!")).count())
        );
    }

    @Test
    void run_emptyInput_stopsWithoutAnExitCommand() {
        String result = assertDoesNotThrow(() -> runSession(""));

        assertTrue(result.contains("Welcome to ClubFlow!"));
        assertFalse(result.contains("Goodbye!"));
    }

    @Test
    void run_endOfInput_processesRemainingCommandsBeforeStopping() {
        String result = assertDoesNotThrow(() -> runSession("createEvent e/MEETING\nviewEvent all"));

        assertAll(
                () -> assertTrue(result.contains("[CF] Created event: MEETING")),
                () -> assertTrue(result.contains("[CF] [MEETING] Not set")),
                () -> assertFalse(result.contains("Goodbye!"))
        );
    }

    @Test
    void run_invalidCommands_reportsErrorsAndContinues() {
        String result = runSession("unknown\n\ncreateEvent e/BAD d/not-a-date\nviewEvent all\nexit\n");

        assertAll(
                () -> assertTrue(result.contains("[CF] Command syntax error: Command \"unknown\" does not exist.")),
                () -> assertTrue(result.contains("[CF] Command syntax error: Command must not be blank.")),
                () -> assertTrue(result.contains("[CF] Command syntax error: Event date must use YYYY-MM-DD format.")),
                () -> assertTrue(result.contains("[CF] No events found.")),
                () -> assertFalse(result.contains("Created event:")),
                () -> assertTrue(result.contains("[CF] Goodbye!"))
        );
    }

    @Test
    void run_eventCommands_shareDataAcrossTheSession() {
        String result = runSession("createEvent e/XMAS n/\"Christmas Party\"\n"
                + "viewEvent e/xmas\ndeleteEvent e/XMAS\nviewEvent all\nexit\n");

        assertAll(
                () -> assertTrue(result.contains("[CF] Created event: XMAS")),
                () -> assertTrue(result.contains("[CF] [XMAS] Christmas Party")),
                () -> assertTrue(result.contains("[CF] Deleted event: XMAS")),
                () -> assertTrue(result.contains("[CF] No events found.")),
                () -> assertTrue(result.indexOf("[XMAS] Christmas Party")
                        < result.indexOf("Deleted event: XMAS"))
        );
    }

    @Test
    void run_findCommand_searchesCreatedEventsAndReflectsDeletion() {
        String result = runSession("createEvent e/XMAS n/\"Christmas Party\"\n"
                + "find e/party\ndeleteEvent e/XMAS\nfind e/party\nexit\n");

        assertAll(
                () -> assertTrue(result.contains("[CF] Found 1 matching event(s).")),
                () -> assertTrue(result.contains("[CF] [XMAS] Christmas Party")),
                () -> assertTrue(result.contains("[CF] No matching events found.")),
                () -> assertTrue(result.indexOf("[XMAS] Christmas Party")
                        < result.indexOf("Deleted event: XMAS")),
                () -> assertFalse(result.contains("Command syntax error"))
        );
    }

    @Test
    void run_memberCommands_shareAliasesAndConfirmationInput() {
        String result = runSession("createMember n/Lucy t/lucy\naddMember n/Thomas e/thomas@example.com\n"
                + "viewMember\ndeleteMember i/1\nno\ndeleteMember i/1\nyes\n"
                + "viewMember\ndeleteMember i/1 /f\nviewMember\nexit\n");

        assertAll(
                () -> assertTrue(result.contains("[CF] Created member: Lucy")),
                () -> assertTrue(result.contains("[CF] Created member: Thomas")),
                () -> assertTrue(result.contains("[CF] 2. Thomas")),
                () -> assertTrue(result.contains("[CF] Deletion cancelled.")),
                () -> assertTrue(result.contains("[CF] Deleted member: Lucy")),
                () -> assertTrue(result.contains("[CF] 1. Thomas")),
                () -> assertTrue(result.contains("[CF] Deleted member: Thomas")),
                () -> assertTrue(result.contains("[CF] No members found.")),
                () -> assertTrue(result.contains("[CF] Goodbye!")),
                () -> assertFalse(result.contains("does not exist"))
        );
    }

    @Test
    void run_separateInstances_doNotShareClubData() {
        runSession("createEvent e/PRIVATE\ncreateMember n/Lucy t/lucy\nexit\n");

        String secondSession = runSession("viewEvent all\nviewMember\nexit\n");

        assertTrue(secondSession.contains("[CF] No events found."));
        assertTrue(secondSession.contains("[CF] No members found."));
        assertFalse(secondSession.contains("PRIVATE"));
        assertFalse(secondSession.contains("Lucy"));
    }

    @Test
    void main_exitInput_startsAndStopsWithAssertionsEnabled() {
        System.setIn(new ByteArrayInputStream("exit\n".getBytes(StandardCharsets.UTF_8)));

        assertDoesNotThrow(() -> ClubFlow.main(new String[0]));

        String result = output.toString(StandardCharsets.UTF_8);
        assertTrue(result.contains("Welcome to ClubFlow!"));
        assertTrue(result.contains("[CF] Goodbye!"));
    }

    /**
     * Runs a fresh application with scripted input and captures only that session's output.
     *
     * @param input commands and confirmation responses separated by newlines
     * @return output from the complete session
     */
    private String runSession(String input) {
        output.reset();
        System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        new ClubFlow().run();
        return output.toString(StandardCharsets.UTF_8);
    }
}
