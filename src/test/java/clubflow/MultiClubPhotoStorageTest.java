package clubflow;

import clubflow.event.Event;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests photo snapshots without relying on the machine's image viewer. */
class MultiClubPhotoStorageTest {
    private static final String EVENT_TAG = "MULTICLUB_STORAGE_TEST";
    private static final Path CACHE_PHOTOS = Path.of(Event.PHOTO_SAVE_ROOT,
            Event.PHOTO_RELATIVE_PATH);

    @TempDir
    Path testDirectory;

    @AfterEach
    void deleteTestCache() throws IOException {
        Path testEventDirectory = CACHE_PHOTOS.resolve(EVENT_TAG);
        if (Files.isDirectory(testEventDirectory)) {
            try (var entries = Files.walk(testEventDirectory)) {
                for (Path path : entries.sorted(java.util.Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
    }

    @Test
    void save_andLoad_restoresOnlySelectedClubsPhotos() throws IOException {
        SaveLoadManager storage = new SaveLoadManager();
        Path chessSave = testDirectory.resolve("chess/club.cf");
        Path roboticsSave = testDirectory.resolve("robotics/club.cf");
        Club chess = new Club();
        chess.addEvent(new Event(EVENT_TAG, "Chess event", null, null, 0, "", ""));
        Path cachedPhoto = CACHE_PHOTOS.resolve(EVENT_TAG + "/portrait.png");
        Files.createDirectories(cachedPhoto.getParent());
        Files.write(cachedPhoto, new byte[]{1, 2, 3});

        storage.save(chess, chessSave);
        assertTrue(Files.isRegularFile(testDirectory.resolve("chess/photos/"
                + EVENT_TAG + "/portrait.png")));

        Files.createDirectories(roboticsSave.getParent());
        Files.createFile(roboticsSave);
        Club restoredRobotics = storage.load(roboticsSave);
        assertTrue(restoredRobotics.getEvents().isEmpty());
        assertFalse(Files.exists(cachedPhoto));
        assertFalse(Files.exists(testDirectory.resolve("robotics/photos/"
                + EVENT_TAG + "/portrait.png")));

        Club restoredChess = storage.load(chessSave);
        assertEquals(List.of("portrait"), restoredChess.getEvent(EVENT_TAG).getPhotoNames());
        assertTrue(Files.isRegularFile(cachedPhoto));
    }

    @Test
    void save_removedPhotos_clearsOldSnapshot() throws IOException {
        SaveLoadManager storage = new SaveLoadManager();
        Club club = new Club();
        club.addEvent(new Event(EVENT_TAG, "Event", null, null, 0, "", ""));
        Path saveFile = testDirectory.resolve("chess/club.cf");
        Path cachedPhoto = CACHE_PHOTOS.resolve(EVENT_TAG + "/deleted.png");
        Path savedPhoto = testDirectory.resolve("chess/photos/" + EVENT_TAG + "/deleted.png");
        Files.createDirectories(cachedPhoto.getParent());
        Files.write(cachedPhoto, new byte[]{1});

        storage.save(club, saveFile);
        assertTrue(Files.exists(savedPhoto));
        Files.delete(cachedPhoto);
        storage.save(club, saveFile);

        assertFalse(Files.exists(savedPhoto));
        assertTrue(Files.isRegularFile(saveFile));
    }

    @Test
    void save_ignoresFoldersNotBelongingToClubEvents() throws IOException {
        SaveLoadManager storage = new SaveLoadManager();
        Club club = new Club();
        Path cachedPhoto = CACHE_PHOTOS.resolve(EVENT_TAG + "/orphan.png");
        Files.createDirectories(cachedPhoto.getParent());
        Files.write(cachedPhoto, new byte[]{7});
        Path saveFile = testDirectory.resolve("empty/club.cf");

        storage.save(club, saveFile);

        assertFalse(Files.exists(testDirectory.resolve("empty/photos/" + EVENT_TAG)));
    }

    @Test
    void load_invalidFile_doesNotClearExistingPhotoCache() throws IOException {
        SaveLoadManager storage = new SaveLoadManager();
        Path cachedPhoto = CACHE_PHOTOS.resolve(EVENT_TAG + "/untouched.png");
        Files.createDirectories(cachedPhoto.getParent());
        Files.write(cachedPhoto, new byte[]{1});
        Path saveFile = testDirectory.resolve("broken.cf");
        Files.writeString(saveFile, "INVALID|record\n");

        assertThrows(IOException.class, () -> storage.load(saveFile));
        assertTrue(Files.isRegularFile(cachedPhoto));
    }

    @Test
    void updateEvent_keepsRegisteredPhotosAndMarksClubUnsaved() {
        Club club = new Club();
        Event original = new Event(EVENT_TAG, "Before", null, null, 0, "", "");
        original.loadPhoto("registered");
        club.addEvent(original);
        club.markSaved();

        assertTrue(club.updateEvent(new Event(EVENT_TAG, "After", null, null, 0, "", "")));

        assertTrue(club.hasUnsavedChanges());
        assertEquals(List.of("registered"), club.getEvent(EVENT_TAG).getPhotoNames());
        assertEquals("Before", original.getName());
    }

    @Test
    void markUnsaved_andDeleteAllPhotos_keepFailedDeletionsRegistered() {
        Club club = new Club();
        Event event = new Event(EVENT_TAG, "Event", null, null, 0, "", "");
        club.addEvent(event);
        club.markSaved();
        club.markUnsaved();
        assertTrue(club.hasUnsavedChanges());

        event.loadPhoto("missing-file");
        assertEquals(1, event.deleteAllPhotos());
        assertEquals(List.of("missing-file"), event.getPhotoNames());
    }
}
