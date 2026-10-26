package clubflow;

import clubflow.event.Event;
import clubflow.member.Member;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import clubflow.exception.ClubNotFoundException;
import clubflow.exception.DuplicateClubException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests club creation, selection, isolation, persistence, and legacy migration.
 * All paths are placed in a JUnit temporary directory.
 */
class ClubManagerTest {
    @TempDir
    Path testDirectory;

    private ClubManager newManager(Club club) {
        return new ClubManager(club, testDirectory.resolve("default.cf"));
    }

    @Test
    void constructor_defaultManager_usesDefaultClubId() {
        assertEquals("default", new ClubManager(new Club()).getActiveClubId());
    }

    @Test
    void initialize_missingSave_createsEmptyDefaultClub() throws IOException {
        Club club = new Club();
        club.addEvent(new Event("OLD", "Unsaved", null, null, 0, "", ""));
        ClubManager manager = newManager(club);

        assertFalse(manager.initialize());
        assertTrue(Files.isRegularFile(manager.getActiveClubFile()));
        assertTrue(club.getEvents().isEmpty());
        assertFalse(club.hasUnsavedChanges());
        assertEquals("default", manager.getActiveClubId());
        assertEquals(List.of("default"), manager.listClubs());
    }

    @Test
    void initialize_existingSave_loadsContentsWithoutChangingSharedClub() throws IOException {
        Club club = new Club();
        ClubManager manager = newManager(club);
        Files.writeString(manager.getActiveClubFile(), "EVENT|AGM|Annual|null|null|0.0|||true\n");

        assertTrue(manager.initialize());
        assertTrue(club.findEventByTag("AGM").orElseThrow().isCompleted());
        assertFalse(club.hasUnsavedChanges());
    }

    @Test
    void initialize_nonFileSavePath_rejectsDirectory() throws IOException {
        ClubManager manager = newManager(new Club());
        Files.createDirectories(manager.getActiveClubFile());

        assertThrows(IOException.class, manager::initialize);
    }

    @Test
    void initialize_invalidExistingFile_doesNotReplaceCurrentData() throws IOException {
        Club club = new Club();
        club.addEvent(new Event("KEEP", "Keep", null, null, 0, "", ""));
        ClubManager manager = newManager(club);
        Files.writeString(manager.getActiveClubFile(), "INVALID|line\n");

        assertThrows(IOException.class, manager::initialize);
        assertTrue(club.findEventByTag("KEEP").isPresent());
    }

    @Test
    void initialize_legacyFile_migratesPhotosAndPreservesOriginal() throws IOException {
        Path legacyFile = testDirectory.resolve("clubs.cf");
        Path clubsRoot = testDirectory.resolve("clubs");
        Path defaultFile = clubsRoot.resolve("default/club.cf");
        String contents = "EVENT|OLD|Original|null|null|0.0|||false\n";
        Files.writeString(legacyFile, contents, StandardCharsets.UTF_8);
        Path legacyPhoto = testDirectory.resolve("photos/OLD/existing.png");
        Files.createDirectories(legacyPhoto.getParent());
        Files.write(legacyPhoto, new byte[]{1, 2, 3});

        Club club = new Club();
        ClubManager manager = new ClubManager(club, clubsRoot, defaultFile, legacyFile);

        assertTrue(manager.initialize());
        assertEquals(contents, Files.readString(defaultFile));
        assertEquals(contents, Files.readString(legacyFile));
        assertTrue(Files.exists(clubsRoot.resolve("default/photos/OLD/existing.png")));
        assertEquals(List.of("existing"), club.findEventByTag("OLD").orElseThrow().getPhotoNames());
        assertFalse(club.hasUnsavedChanges());
    }

    @Test
    void initialize_legacyFileWithoutPhotos_stillMigrates() throws IOException {
        Path legacyFile = testDirectory.resolve("old.cf");
        Path root = testDirectory.resolve("clubs");
        Path defaultFile = root.resolve("default/club.cf");
        Files.writeString(legacyFile, "");

        ClubManager manager = new ClubManager(new Club(), root, defaultFile, legacyFile);

        assertTrue(manager.initialize());
        assertTrue(Files.isRegularFile(defaultFile));
        assertTrue(Files.isRegularFile(legacyFile));
    }

    @Test
    void createClub_normalizesNameAndCreatesEmptySave() throws IOException, DuplicateClubException {
        ClubManager manager = newManager(new Club());
        manager.initialize();

        manager.createClub(" ChEsS ");

        assertTrue(Files.isRegularFile(manager.getClubFile("CHESS")));
        assertEquals(0L, Files.size(manager.getClubFile("chess")));
        assertEquals(List.of("default", "chess"), manager.listClubs());
    }

    @Test
    void createClub_sameNameIgnoringCase_rejectsDuplicate() throws IOException, DuplicateClubException {
        ClubManager manager = newManager(new Club());
        manager.createClub("Chess");

        assertThrows(DuplicateClubException.class, () -> manager.createClub("cHeSs"));
        assertThrows(DuplicateClubException.class, () -> manager.createClub("default"));
    }

    @Test
    void createClub_preexistingMixedCaseDirectory_rejectsDuplicate() throws IOException {
        ClubManager manager = newManager(new Club());
        Path clubDirectory = testDirectory.resolve("clubs/Chess");
        Files.createDirectories(clubDirectory);

        assertThrows(DuplicateClubException.class, () -> manager.createClub("chess"));
    }

    @Test
    void createClub_preexistingFileWithSameName_rejectsDuplicate() throws IOException {
        ClubManager manager = newManager(new Club());
        Path root = testDirectory.resolve("clubs");
        Files.createDirectories(root);
        Files.createFile(root.resolve("chess"));

        assertThrows(DuplicateClubException.class, () -> manager.createClub("Chess"));
    }

    @Test
    void createClub_unwritableClubRoot_propagatesIOException() throws IOException {
        ClubManager manager = newManager(new Club());
        Files.createFile(testDirectory.resolve("clubs"));

        assertThrows(IOException.class, () -> manager.createClub("chess"));
    }

    @Test
    void createClub_invalidNames_rejectsUnsafePaths() {
        ClubManager manager = newManager(new Club());

        for (String name : List.of("", " ", "two words", "../escape", "foo/bar", "école")) {
            assertThrows(IllegalArgumentException.class, () -> manager.createClub(name));
        }
        assertThrows(NullPointerException.class, () -> manager.getClubFile(null));
    }

    @Test
    void listClubs_ignoresIncompleteEntriesAndSortsNames() throws IOException, DuplicateClubException {
        ClubManager manager = newManager(new Club());
        assertTrue(manager.listClubs().isEmpty());

        manager.createClub("zebra");
        manager.createClub("alpha");
        Files.createDirectory(testDirectory.resolve("clubs/incomplete"));
        Files.writeString(testDirectory.resolve("clubs/readme.txt"), "not a club");
        Files.createDirectories(testDirectory.resolve("clubs/default"));

        assertEquals(List.of("alpha", "zebra"), manager.listClubs());
    }

    @Test
    void switchClub_savesSeparateEventsAndMembers() throws IOException, ClubNotFoundException,
            DuplicateClubException {
        Club club = new Club();
        ClubManager manager = newManager(club);
        manager.initialize();
        manager.createClub("chess");
        manager.createClub("robotics");

        manager.switchClub("chess");
        assertEquals("chess", manager.getActiveClubId());
        club.addEvent(new Event("AGM", "Chess AGM", null, null, 0, "", ""));
        club.addMember(new Member("Alice", "alice", "", List.of("president")));
        manager.switchClub("robotics");
        assertTrue(club.getEvents().isEmpty());
        assertTrue(club.getMembers().isEmpty());
        club.addEvent(new Event("AGM", "Robotics AGM", null, null, 0, "", ""));
        club.addMember(new Member("Bob", "bob", "", List.of("secretary")));
        manager.switchClub("CHESS");

        assertEquals("Chess AGM", club.findEventByTag("AGM").orElseThrow().getName());
        assertEquals("Alice", club.getMembers().getFirst().getName());
        Club savedRobotics = new SaveLoadManager().load(manager.getClubFile("robotics"));
        assertEquals("Robotics AGM", savedRobotics.findEventByTag("AGM").orElseThrow().getName());
        assertEquals("Bob", savedRobotics.getMembers().getFirst().getName());
        assertFalse(club.hasUnsavedChanges());
        assertEquals(List.of("default", "chess", "robotics"), manager.listClubs());
    }

    @Test
    void switchClub_eventsWithSameTag_doNotSharePhotos() throws Exception {
        String tag = "MULTICLUB_PHOTO_TEST";
        Club club = new Club();
        ClubManager manager = newManager(club);
        manager.initialize();
        manager.createClub("chess");
        manager.createClub("robotics");
        Path source = testDirectory.resolve("source.png");
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB),
                "png", source.toFile());

        try {
            manager.switchClub("chess");
            club.addEvent(new Event(tag, "Chess event", null, null, 0, "", ""));
            club.getEvent(tag).addPhoto(source.toString(), "portrait");
            manager.switchClub("robotics");
            club.addEvent(new Event(tag, "Robotics event", null, null, 0, "", ""));

            assertTrue(club.getEvent(tag).getPhotoNames().isEmpty());
            assertFalse(Files.exists(testDirectory.resolve(
                    "clubs/robotics/photos/" + tag + "/portrait.png")));

            manager.switchClub("chess");
            assertEquals(List.of("portrait"), club.getEvent(tag).getPhotoNames());
            assertTrue(Files.isRegularFile(testDirectory.resolve(
                    "clubs/chess/photos/" + tag + "/portrait.png")));
        } finally {
            club.findEventByTag(tag).ifPresent(Event::deleteAllPhotos);
        }
    }

    @Test
    void switchClub_sameClub_isNoOpAndDoesNotSave() throws IOException, ClubNotFoundException {
        Club club = new Club();
        ClubManager manager = newManager(club);
        manager.initialize();
        club.addEvent(new Event("UNSAVED", "Pending", null, null, 0, "", ""));

        manager.switchClub("DEFAULT");

        assertTrue(club.hasUnsavedChanges());
        assertTrue(Files.readString(manager.getActiveClubFile()).isEmpty());
    }

    @Test
    void switchClub_missingTarget_doesNotSaveOrChangeSelection() throws IOException {
        Club club = new Club();
        ClubManager manager = newManager(club);
        manager.initialize();
        club.addEvent(new Event("UNSAVED", "Pending", null, null, 0, "", ""));

        assertThrows(ClubNotFoundException.class, () -> manager.switchClub("missing"));
        assertEquals("default", manager.getActiveClubId());
        assertTrue(club.hasUnsavedChanges());
        assertTrue(Files.readString(manager.getActiveClubFile()).isEmpty());
    }

    @Test
    void switchClub_invalidTarget_doesNotModifyState() throws IOException {
        ClubManager manager = newManager(new Club());
        manager.initialize();

        assertThrows(IllegalArgumentException.class, () -> manager.switchClub("../escape"));
        assertEquals("default", manager.getActiveClubId());
    }

    @Test
    void switchClub_corruptTarget_keepsCurrentClub() throws IOException, DuplicateClubException {
        Club club = new Club();
        ClubManager manager = newManager(club);
        manager.initialize();
        manager.createClub("broken");
        club.addEvent(new Event("SAFE", "Safe", null, null, 0, "", ""));
        Files.writeString(manager.getClubFile("broken"), "INVALID|record");

        assertThrows(IOException.class, () -> manager.switchClub("broken"));
        assertEquals("default", manager.getActiveClubId());
        assertTrue(club.findEventByTag("SAFE").isPresent());
    }

    @Test
    void switchClub_saveFailure_keepsCurrentSelection() throws IOException, DuplicateClubException {
        Club club = new Club();
        ClubManager manager = newManager(club);
        manager.initialize();
        manager.createClub("chess");
        Path defaultFile = manager.getActiveClubFile();
        Files.delete(defaultFile);
        Files.createDirectory(defaultFile);

        assertThrows(IOException.class, () -> manager.switchClub("chess"));
        assertEquals("default", manager.getActiveClubId());
        assertTrue(club.getEvents().isEmpty());
    }

    @Test
    void saveActiveClub_writesToCorrectClubAfterSwitch() throws IOException, DuplicateClubException,
            ClubNotFoundException {
        Club club = new Club();
        ClubManager manager = newManager(club);
        manager.initialize();
        manager.createClub("chess");
        manager.switchClub("chess");
        club.addEvent(new Event("PLAN", "Chess plan", null, null, 10, "", ""));

        manager.saveActiveClub();

        assertEquals(manager.getClubFile("Chess"), manager.getActiveClubFile());
        assertEquals("Chess plan", new SaveLoadManager().load(manager.getActiveClubFile())
                .findEventByTag("PLAN").orElseThrow().getName());
        assertNotNull(manager.getClubFile("default"));
    }
}
