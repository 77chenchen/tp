package clubflow;

import clubflow.event.Event;
import clubflow.exception.ClubNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies startup club preferences across fresh manager instances.
 * Temporary directories keep these tests separate from the user's saved clubs.
 */
class ClubManagerLastActiveClubTest {
    @TempDir
    Path testDirectory;

    private ClubManager newManager(Club club) {
        Path root = testDirectory.resolve("clubs");
        return new ClubManager(club, root, root.resolve("default/club.cf"), null);
    }

    private Path preferenceFile() {
        return testDirectory.resolve("clubs/last-active.txt");
    }

    /** Saves recognizable default data so fallback tests check contents as well as the ID. */
    private ClubManager prepareDefaultClub() throws IOException {
        Club club = new Club();
        ClubManager manager = newManager(club);
        manager.initialize();
        club.addEvent(new Event("DEFAULT_EVENT", "Default meeting", null, null, 0, "", ""));
        manager.saveActiveClub();
        return manager;
    }

    private void assertRestoresDefault() throws IOException {
        Club restored = new Club();
        ClubManager restarted = newManager(restored);
        restarted.initialize();

        assertEquals("default", restarted.getActiveClubId());
        assertTrue(restored.findEventByTag("DEFAULT_EVENT").isPresent());
        assertFalse(restored.hasUnsavedChanges());
    }

    @Test
    void initialize_missingPreference_loadsDefaultData() throws IOException {
        prepareDefaultClub();

        assertFalse(Files.exists(preferenceFile()));
        assertRestoresDefault();
    }

    @Test
    void switchClub_success_remembersNormalizedIdAndRestoresData() throws Exception {
        Club original = new Club();
        ClubManager manager = newManager(original);
        manager.initialize();
        manager.createClub("chess");
        manager.switchClub("CHESS");
        original.addEvent(new Event("AGM", "Chess meeting", null, null, 0, "", ""));
        manager.saveActiveClub();

        assertEquals("chess", Files.readString(preferenceFile()));
        Club restored = new Club();
        ClubManager restarted = newManager(restored);
        restarted.initialize();

        assertEquals("chess", restarted.getActiveClubId());
        assertEquals(restarted.getClubFile("chess"), restarted.getActiveClubFile());
        assertEquals("Chess meeting", restored.findEventByTag("AGM").orElseThrow().getName());
        assertFalse(restored.hasUnsavedChanges());
    }

    @Test
    void switchClub_backToDefault_remembersDefaultOnRestart() throws Exception {
        ClubManager manager = prepareDefaultClub();
        manager.createClub("chess");
        manager.switchClub("chess");
        manager.switchClub("default");

        assertEquals("default", Files.readString(preferenceFile()));
        assertRestoresDefault();
    }

    @Test
    void createClub_withoutSwitch_keepsRememberedClub() throws Exception {
        ClubManager manager = prepareDefaultClub();
        manager.createClub("chess");
        manager.switchClub("chess");

        manager.createClub("robotics");

        assertEquals("chess", Files.readString(preferenceFile()));
        ClubManager restarted = newManager(new Club());
        restarted.initialize();
        assertEquals("chess", restarted.getActiveClubId());
    }

    @Test
    void switchClub_missingTarget_keepsRememberedClub() throws Exception {
        ClubManager manager = prepareDefaultClub();
        manager.createClub("chess");
        manager.switchClub("chess");

        assertThrows(ClubNotFoundException.class, () -> manager.switchClub("missing"));

        assertEquals("chess", manager.getActiveClubId());
        assertEquals("chess", Files.readString(preferenceFile()));
    }

    @Test
    void switchClub_corruptTarget_keepsRememberedClub() throws Exception {
        ClubManager manager = prepareDefaultClub();
        manager.createClub("chess");
        manager.switchClub("chess");
        manager.createClub("broken");
        Files.writeString(manager.getClubFile("broken"), "INVALID|record\n");

        assertThrows(IOException.class, () -> manager.switchClub("broken"));

        assertEquals("chess", manager.getActiveClubId());
        assertEquals("chess", Files.readString(preferenceFile()));
    }

    @Test
    void initialize_mixedCasePreferenceWithWhitespace_restoresNormalizedClub() throws Exception {
        ClubManager manager = prepareDefaultClub();
        manager.createClub("chess");
        Files.writeString(preferenceFile(), "  ChEsS\n");

        ClubManager restarted = newManager(new Club());
        restarted.initialize();

        assertEquals("chess", restarted.getActiveClubId());
    }

    @Test
    void initialize_invalidPreferences_preservesDefaultData() throws IOException {
        prepareDefaultClub();

        for (String invalidId : new String[]{"", " \n", "../escape", "two clubs"}) {
            Files.writeString(preferenceFile(), invalidId);
            assertRestoresDefault();
        }
    }

    @Test
    void initialize_missingRememberedClub_preservesDefaultWithoutCreatingClub() throws IOException {
        prepareDefaultClub();
        Files.writeString(preferenceFile(), "missing");

        assertRestoresDefault();
        assertFalse(Files.exists(testDirectory.resolve("clubs/missing")));
    }

    @Test
    void initialize_corruptRememberedClub_preservesDefaultData() throws Exception {
        ClubManager manager = prepareDefaultClub();
        manager.createClub("broken");
        Files.writeString(manager.getClubFile("broken"), "INVALID|record\n");
        Files.writeString(preferenceFile(), "broken");

        assertRestoresDefault();
    }

    @Test
    void initialize_preferenceIsDirectory_preservesDefaultData() throws IOException {
        prepareDefaultClub();
        Files.createDirectory(preferenceFile());

        assertRestoresDefault();
    }

    @Test
    void switchClub_preferenceWriteFails_stillSwitchesAndCanSaveTargetData() throws Exception {
        Club club = new Club();
        ClubManager manager = newManager(club);
        manager.initialize();
        manager.createClub("chess");
        // A directory at the preference path causes a write failure on any platform.
        Files.createDirectory(preferenceFile());

        assertDoesNotThrow(() -> manager.switchClub("chess"));

        assertEquals("chess", manager.getActiveClubId());
        club.addEvent(new Event("AGM", "Chess meeting", null, null, 0, "", ""));
        manager.saveActiveClub();
        Club saved = new SaveLoadManager().load(manager.getClubFile("chess"));
        assertTrue(saved.findEventByTag("AGM").isPresent());
        assertTrue(Files.isDirectory(preferenceFile()));
    }
}
