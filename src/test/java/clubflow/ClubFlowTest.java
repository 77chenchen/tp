package clubflow;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

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
    void run_missingEvents_reportsEachErrorOnceAndContinuesWithStoredData() {
        String result = runSession("createEvent e/XMAS n/Party\n"
                + "mark e/MISSING_MARK\nunmark e/MISSING_UNMARK\n"
                + "editEvent e/MISSING_EDIT n/Changed\ndeleteEvent e/MISSING_DELETE\n"
                + "viewEvent e/MISSING_VIEW\nviewEvent e/XMAS\nfind e/Party\nexit\n");

        assertEquals(List.of(
                "[CF] Event not found: MISSING_MARK",
                "[CF] Event not found: MISSING_UNMARK",
                "[CF] Event not found: MISSING_EDIT",
                "[CF] Event not found: MISSING_DELETE",
                "[CF] Event not found: MISSING_VIEW"
        ), result.lines().filter(line -> line.contains("[CF] Event not found:"))
                .map(line -> line.substring(line.indexOf("[CF]"))).toList());
        assertAll(
                () -> assertEquals(2, result.lines().filter(line -> line.contains("[CF] [XMAS] Party")).count()),
                () -> assertTrue(result.contains("[CF]   Status:    Not completed")),
                () -> assertTrue(result.contains("[CF] Found 1 matching event(s).")),
                () -> assertTrue(result.contains("[CF] Goodbye!")),
                () -> assertFalse(result.contains("Command syntax error"))
        );
    }

    @Test
    void run_duplicateEvent_reportsErrorOnceAndContinuesWithoutReplacingEvent() {
        String result = runSession("createEvent e/XMAS n/Original b/500\nmark e/XMAS\n"
                + "createEvent e/xmas n/Duplicate b/999\nviewEvent e/XMAS\n"
                + "createEvent e/AGM n/Meeting\nviewEvent all\nexit\n");

        assertAll(
                () -> assertEquals(1, result.lines()
                        .filter(line -> line.contains("[CF] Event already exists: xmas")).count()),
                () -> assertEquals(2, result.lines().filter(line -> line.contains("[CF] Created event:")).count()),
                () -> assertEquals(2, result.lines().filter(line -> line.contains("[CF] [XMAS] Original")).count()),
                () -> assertTrue(result.contains("[CF]   Status:    Completed")),
                () -> assertTrue(result.contains("[CF]   Budget:    $500.00")),
                () -> assertTrue(result.contains("[CF] [AGM] Meeting")),
                () -> assertTrue(result.contains("[CF] Goodbye!")),
                () -> assertFalse(result.contains("Duplicate")),
                () -> assertFalse(result.contains("Command syntax error"))
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
    void run_editEvent_updatesDetailsSeenByViewAndFind() {
        String result = runSession("createEvent e/XMAS n/Party v/\"Main Hall\"\n"
                + "editEvent e/xmas n/Dinner b/600\nviewEvent e/XMAS\n"
                + "find e/Dinner\nfind e/Party\nexit\n");

        assertAll(
                () -> assertTrue(result.contains("[CF] Updated event: XMAS")),
                () -> assertEquals(2, result.lines().filter(line -> line.contains("[CF] [XMAS] Dinner")).count()),
                () -> assertTrue(result.contains("[CF]   Venue:     Main Hall")),
                () -> assertTrue(result.contains("[CF]   Budget:    $600.00")),
                () -> assertTrue(result.contains("[CF] No matching events found.")),
                () -> assertFalse(result.contains("Command syntax error"))
        );
    }

    @Test
    void run_markAndUnmark_updatesStatusAndPreservesEdits() {
        String result = runSession("createEvent e/XMAS n/Party\nmark e/xmas\n"
                + "editEvent e/XMAS n/Dinner\nviewEvent e/XMAS\n"
                + "unmark e/XMAS\nviewEvent e/XMAS\nexit\n");

        assertAll(
                () -> assertTrue(result.contains("[CF] Marked event as completed: XMAS")),
                () -> assertTrue(result.contains("[CF] Marked event as not completed: XMAS")),
                () -> assertTrue(result.contains("[CF]   Status:    Completed")),
                () -> assertTrue(result.contains("[CF]   Status:    Not completed")),
                () -> assertTrue(result.indexOf("Status:    Completed") < result.indexOf("Status:    Not completed")),
                () -> assertEquals(2, result.lines().filter(line -> line.contains("[CF] [XMAS] Dinner")).count()),
                () -> assertFalse(result.contains("Command syntax error"))
        );
    }

    @Test
    void run_completionSaveAndLoad_restoresStatusInANewSessionAndAllowsUnmarking() throws IOException {
        // Keep the save path relative, as required by the commands, and separate from users' save files.
        Path testDirectory = Files.createTempDirectory(Files.createDirectories(Path.of("build", "tmp")),
                "completion-workflow-");
        Path saveFile = testDirectory.resolve("completion workflow.cf");
        String pathArgument = "p/\"" + saveFile + "\"";
        try {
            String savedSession = runSession("createEvent e/XMAS n/\"Christmas Party\" "
                    + "d/2026-12-25 by/2026-12-20 b/500 v/\"Main Hall\" l/Chairs\n"
                    + "mark e/XMAS\nsave " + pathArgument + "\nexit\n");

            assertTrue(savedSession.contains("[CF] Marked event as completed: XMAS"));
            assertTrue(savedSession.contains("[CF] Data saved successfully!"));
            assertTrue(Files.isRegularFile(saveFile));

            // A fresh app starts empty, so this can pass only if loading restores the saved event and status.
            String loadedSession = runSession("load " + pathArgument + "\nviewEvent e/XMAS\n"
                    + "unmark e/xmas\nviewEvent e/XMAS\nexit\n");

            assertAll(
                    () -> assertTrue(loadedSession.contains("[CF] Data loaded successfully!")),
                    () -> assertEquals(List.of("[CF]   Status:    Completed", "[CF]   Status:    Not completed"),
                            loadedSession.lines().filter(line -> line.contains("Status:")).toList()),
                    () -> assertTrue(loadedSession.contains("[CF] Marked event as not completed: XMAS")),
                    () -> assertEquals(2, loadedSession.lines()
                            .filter(line -> line.contains("[CF] [XMAS] Christmas Party")).count()),
                    () -> assertTrue(loadedSession.contains("[CF]   Date:      2026-12-25")),
                    () -> assertTrue(loadedSession.contains("[CF]   Deadline:  2026-12-20")),
                    () -> assertTrue(loadedSession.contains("[CF]   Budget:    $500.00")),
                    () -> assertTrue(loadedSession.contains("[CF]   Venue:     Main Hall")),
                    () -> assertTrue(loadedSession.contains("[CF]   Logistics: Chairs")),
                    () -> assertTrue(loadedSession.contains("[CF] Goodbye!"))
            );
        } finally {
            try (var paths = Files.walk(testDirectory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
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
