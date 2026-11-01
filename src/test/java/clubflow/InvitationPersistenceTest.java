package clubflow;

import clubflow.event.Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs real application sessions to verify invitation edits are autosaved and restored.
 * Separate headless JVMs prevent these tests from accessing the system clipboard.
 */
class InvitationPersistenceTest {
    @TempDir
    Path testDirectory;

    @Test
    void run_fillMissingInvitationDetails_autosavesAndRestoresOnRestart() throws Exception {
        String firstSession = runSession("createEvent e/CAMP\ninvite e/CAMP\n"
                + "Y\nClub Camp\n2026-12-15\nMain Hall\nexit\n");

        assertTrue(firstSession.contains("Updated event: CAMP"), firstSession);
        assertTrue(firstSession.contains("Location: Main Hall"), firstSession);
        Event saved = new SaveLoadManager().load(
                testDirectory.resolve("saves/clubs/default/club.cf"))
                .findEventByTag("CAMP").orElseThrow();
        assertEquals("Club Camp", saved.getName());
        assertEquals(LocalDate.of(2026, 12, 15), saved.getDate());
        assertEquals("Main Hall", saved.getVenue());

        // A complete invitation must not consume the following exit command as an answer.
        String restarted = runSession("invite e/CAMP\nexit\n");

        assertTrue(restarted.contains("You're invited to: Club Camp!"), restarted);
        assertTrue(restarted.contains("Date: 2026-12-15"), restarted);
        assertTrue(restarted.contains("Location: Main Hall"), restarted);
        assertFalse(restarted.contains("Would you like to fill them in?"), restarted);
        assertFalse(restarted.contains("[Add"), restarted);
        assertTrue(restarted.contains("Goodbye!"), restarted);
    }

    @Test
    void run_declineMissingDetails_keepsPlaceholdersAndProcessesNextCommand() throws Exception {
        String output = runSession("createEvent e/CAMP\ninvite e/CAMP\nN\n"
                + "createEvent e/NEXT n/NextMeeting\nexit\n");

        assertTrue(output.contains("You're invited to: [Add event name]!"), output);
        assertTrue(output.contains("Date: [Add date]"), output);
        assertTrue(output.contains("Location: [Add venue]"), output);
        assertFalse(output.contains("Updated event: CAMP"), output);
        Club saved = new SaveLoadManager().load(testDirectory.resolve("saves/clubs/default/club.cf"));
        assertTrue(saved.findEventByTag("CAMP").orElseThrow().getName().isBlank());
        assertTrue(saved.findEventByTag("NEXT").isPresent());
    }

    /** Runs with isolated save/cache files and a time limit to catch stuck input loops. */
    private String runSession(String input) throws Exception {
        Path javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java");
        Path classes = Path.of(ClubFlow.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Process process = new ProcessBuilder(javaExecutable.toString(), "-ea", "-Djava.awt.headless=true",
                "-cp", classes.toString(), "clubflow.ClubFlow")
                .directory(testDirectory.toFile())
                .redirectErrorStream(true)
                .start();
        try {
            try (var inputStream = process.getOutputStream()) {
                inputStream.write(input.getBytes(StandardCharsets.UTF_8));
            }
            assertTrue(process.waitFor(15, TimeUnit.SECONDS), "Application session timed out");
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(0, process.exitValue(), output);
            return output;
        } finally {
            process.destroyForcibly();
        }
    }
}
