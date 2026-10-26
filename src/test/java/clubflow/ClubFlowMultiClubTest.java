package clubflow;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies real commands, autosaving, and loading across application restarts. */
class ClubFlowMultiClubTest {
    @TempDir
    Path testDirectory;

    private final InputStream originalInput = System.in;
    private final PrintStream originalOutput = System.out;
    private ByteArrayOutputStream outputBytes;
    private PrintStream capturedOutput;

    @BeforeEach
    void captureOutput() {
        outputBytes = new ByteArrayOutputStream();
        capturedOutput = new PrintStream(outputBytes, true, StandardCharsets.UTF_8);
        System.setOut(capturedOutput);
    }

    @AfterEach
    void restoreStreams() {
        System.setIn(originalInput);
        System.setOut(originalOutput);
        capturedOutput.close();
    }

    private String runSession(String commands) {
        outputBytes.reset();
        System.setIn(new ByteArrayInputStream(commands.getBytes(StandardCharsets.UTF_8)));
        new ClubFlow(testDirectory.resolve("default.cf")).run();
        return outputBytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    void run_createSwitchListAndRestart_preservesClubIsolation() throws Exception {
        String firstSession = runSession("createClub n/chess\n"
                + "switchClub n/chess\n"
                + "createEvent e/AGM n/ChessMeeting\n"
                + "switchClub n/default\n"
                + "createEvent e/AGM n/DefaultMeeting\n"
                + "listClubs\n"
                + "exit\n");

        Path chessFile = testDirectory.resolve("clubs/chess/club.cf");
        Path defaultFile = testDirectory.resolve("default.cf");
        assertTrue(firstSession.contains("Created club: chess"));
        assertTrue(firstSession.contains("default (active)"));
        assertTrue(Files.isRegularFile(chessFile));
        assertTrue(Files.isRegularFile(defaultFile));
        assertTrue(Files.readString(chessFile).contains("ChessMeeting"));
        assertTrue(Files.readString(defaultFile).contains("DefaultMeeting"));

        String secondSession = runSession("viewEvent e/AGM\n"
                + "switchClub n/chess\n"
                + "viewEvent e/AGM\n"
                + "currentClub\n"
                + "exit\n");

        assertTrue(secondSession.contains("Saved data loaded."));
        assertTrue(secondSession.contains("DefaultMeeting"));
        assertTrue(secondSession.contains("ChessMeeting"));
        assertTrue(secondSession.contains("Active club: chess"));
        assertFalse(secondSession.contains("Command syntax error"));
    }

    @Test
    void run_duplicateUnknownAndInvalidClubs_doNotTerminateSession() {
        String output = runSession("createClub n/Chess\n"
                + "createClub n/CHESS\n"
                + "switchClub n/missing\n"
                + "createClub n/../unsafe\n"
                + "currentClub\n"
                + "exit\n");

        assertTrue(output.contains("Club already exists: CHESS"));
        assertTrue(output.contains("Club not found: missing"));
        assertTrue(output.contains("Active club: default"));
        assertTrue(output.contains("Goodbye!"));
        assertTrue(output.contains("Use letters, digits, _ or - for a club ID."));
    }

    @Test
    void run_switchingDoesNotResetDefaultClub() throws Exception {
        runSession("createEvent e/FIRST n/Default\ncreateClub n/chess\n"
                + "switchClub n/chess\ncreateEvent e/SECOND n/Chess\nexit\n");

        assertEquals(1, new SaveLoadManager().load(testDirectory.resolve("default.cf")).getEvents().size());
        assertEquals(1, new SaveLoadManager().load(testDirectory.resolve("clubs/chess/club.cf"))
                .getEvents().size());
    }
}
