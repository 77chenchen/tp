package clubflow;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.function.IntConsumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Checks startup failures and recovery from errors during complete terminal sessions.
 * Filesystem failures use temporary paths so existing club data is never affected.
 */
class ClubFlowRecoveryTest {
    @TempDir
    Path testDirectory;

    private final InputStream originalInput = System.in;
    private final PrintStream originalOutput = System.out;
    private ByteArrayOutputStream output;
    private PrintStream capturedOutput;
    private Path saveFile;

    @BeforeEach
    void captureStreams() {
        output = new ByteArrayOutputStream();
        capturedOutput = new PrintStream(output, true, StandardCharsets.UTF_8);
        System.setOut(capturedOutput);
        saveFile = testDirectory.resolve("club.cf");
    }

    @AfterEach
    void restoreStreams() {
        System.setIn(originalInput);
        System.setOut(originalOutput);
        capturedOutput.close();
    }

    @Test
    void constructor_nullSavePath_rejectsInvalidConfiguration() {
        NullPointerException error = assertThrows(NullPointerException.class, () -> new ClubFlow(null));

        assertEquals("Save path must not be null", error.getMessage());
    }

    @Test
    void run_newSaveFile_initializesEmptyClubBeforeReadingCommands() throws IOException {
        String result = runSession("viewEvent all\nexit\n");

        assertAll(
                () -> assertTrue(Files.isRegularFile(saveFile)),
                () -> assertEquals("", Files.readString(saveFile)),
                () -> assertTrue(result.contains("Active club: default")),
                () -> assertFalse(result.contains("Saved data loaded.")),
                () -> assertTrue(result.indexOf("Active club: default") < result.indexOf("No events found.")),
                () -> assertTrue(result.contains("Goodbye!"))
        );
    }

    @Test
    void run_savePathIsDirectory_reportsLoadFailureAndStopsBeforeCommands() throws IOException {
        Files.createDirectory(saveFile);

        String result = assertDoesNotThrow(() -> runSession("createEvent e/IGNORED\nexit\n"));

        assertAll(
                () -> assertTrue(result.contains("Could not load saved data: Default club save path is not a file:")),
                () -> assertTrue(Files.isDirectory(saveFile)),
                () -> assertFalse(result.contains("Active club:")),
                () -> assertFalse(result.contains("Created event:")),
                () -> assertFalse(result.contains("Goodbye!"))
        );
    }

    @Test
    void run_corruptSave_reportsLoadFailureAndPreservesFile() throws IOException {
        String corruptData = "EVENT|BROKEN\n";
        Files.writeString(saveFile, corruptData);

        String result = assertDoesNotThrow(() -> runSession("createEvent e/IGNORED\nexit\n"));

        assertAll(
                () -> assertTrue(result.contains("Could not load saved data:")),
                () -> assertTrue(result.contains("line 1")),
                () -> assertEquals(corruptData, Files.readString(saveFile)),
                () -> assertFalse(result.contains("Saved data loaded.")),
                () -> assertFalse(result.contains("Active club:")),
                () -> assertFalse(result.contains("Created event:")),
                () -> assertFalse(result.contains("Goodbye!"))
        );
    }

    @Test
    void run_readOnlyCommands_doNotRewriteSavedData() throws IOException {
        runSession("createEvent e/MEETING n/Planning\nexit\n");
        String savedData = Files.readString(saveFile);
        Files.setLastModifiedTime(saveFile, FileTime.fromMillis(1_000_000));
        // Read back the timestamp to account for filesystem timestamp precision.
        FileTime savedTime = Files.getLastModifiedTime(saveFile);

        String result = runSession("viewEvent e/MEETING\nhelp c/ce\nexit\n");

        assertAll(
                () -> assertTrue(result.contains("Saved data loaded.")),
                () -> assertTrue(result.contains("[MEETING] Planning")),
                () -> assertEquals(savedData, Files.readString(saveFile)),
                () -> assertEquals(savedTime, Files.getLastModifiedTime(saveFile)),
                () -> assertFalse(result.contains("Changes could not be saved:"))
        );
    }

    @Test
    void run_autosaveKeepsFailing_retainsDataAndStillAllowsExit() {
        String result = runSession("createEvent e/MEETING n/Planning\nviewEvent e/MEETING\nexit\n", line -> {
            if (line == 0) {
                blockSaveFile();
            }
        });

        assertAll(
                () -> assertTrue(result.contains("Created event: MEETING")),
                () -> assertTrue(result.contains("[MEETING] Planning")),
                () -> assertEquals(3, result.lines()
                        .filter(text -> text.contains("Changes could not be saved:")).count()),
                () -> assertTrue(Files.isDirectory(saveFile)),
                () -> assertTrue(result.contains("Goodbye!"))
        );
    }

    @Test
    void run_autosaveRecovers_retriesPendingChangesOnNextCommand() throws IOException {
        String result = runSession("createEvent e/MEETING n/Planning\nviewEvent e/MEETING\nexit\n", line -> {
            if (line == 0) {
                blockSaveFile();
            } else if (line == 1) {
                assertDoesNotThrow(() -> Files.delete(saveFile));
            }
        });

        assertEquals(1, result.lines().filter(text -> text.contains("Changes could not be saved:")).count());
        Club saved = new SaveLoadManager().load(saveFile);
        assertEquals("Planning", saved.findEventByTag("MEETING").orElseThrow().getName());

        String restarted = runSession("viewEvent e/MEETING\nexit\n");
        assertTrue(restarted.contains("Saved data loaded."));
        assertTrue(restarted.contains("[MEETING] Planning"));
        assertTrue(restarted.contains("Goodbye!"));
    }

    @Test
    void run_unexpectedInputFailure_reportsErrorAndProcessesRemainingCommands() throws IOException {
        String result = assertDoesNotThrow(() -> runSession("createEvent e/RECOVERED\nexit\n", line -> {
            if (line == 0) {
                throw new IllegalStateException("Simulated terminal failure");
            }
        }));

        assertAll(
                () -> assertEquals(1, result.lines()
                        .filter(text -> text.contains("[CF] Simulated terminal failure")).count()),
                () -> assertEquals(1, result.lines().filter(text -> text.contains("Created event: RECOVERED")).count()),
                () -> assertTrue(result.contains("Goodbye!")),
                () -> assertTrue(new SaveLoadManager().load(saveFile).findEventByTag("RECOVERED").isPresent())
        );
    }

    /** Replaces the initialized save file with a directory to cause a portable write failure. */
    private void blockSaveFile() {
        assertDoesNotThrow(() -> {
            Files.delete(saveFile);
            Files.createDirectory(saveFile);
        });
    }

    /** Runs a session without injecting any failures. */
    private String runSession(String input) {
        return runSession(input, line -> { });
    }

    /**
     * Runs a session with an action before each input line is read. Delivering one line at a time
     * prevents Scanner from buffering later commands before a failure has been set up or repaired.
     *
     * @param input newline-separated commands
     * @param beforeLine action receiving the zero-based input line number
     * @return terminal output from this session
     */
    private String runSession(String input, IntConsumer beforeLine) {
        output.reset();
        System.setIn(new InputStream() {
            private final String[] lines = input.split("\n");
            private ByteArrayInputStream currentLine = new ByteArrayInputStream(new byte[0]);
            private int nextLine;

            @Override
            public int read() {
                byte[] singleByte = new byte[1];
                return read(singleByte, 0, 1) == -1 ? -1 : Byte.toUnsignedInt(singleByte[0]);
            }

            @Override
            public int read(byte[] bytes, int offset, int length) {
                if (length == 0) {
                    return 0;
                }
                if (currentLine.available() == 0) {
                    if (nextLine == lines.length) {
                        return -1;
                    }
                    currentLine = new ByteArrayInputStream((lines[nextLine] + "\n")
                            .getBytes(StandardCharsets.UTF_8));
                    beforeLine.accept(nextLine++);
                }
                return currentLine.read(bytes, offset, length);
            }
        });
        new ClubFlow(saveFile).run();
        return output.toString(StandardCharsets.UTF_8);
    }
}
