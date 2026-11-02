package clubflow;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import clubflow.event.Event;
import clubflow.event.EventAssignment;
import clubflow.member.Member;

/**
 * Tests rejected records and deterministic failures while replacing saved photo snapshots.
 * Snapshot rollback tests operate only inside the JUnit temporary directory.
 */
class SaveLoadManagerFailureTest {
    @TempDir
    Path testDirectory;

    private Path cacheSaveFile;
    private Path cacheEventDirectory;

    @AfterEach
    void removeCacheFixtures() throws IOException {
        if (cacheEventDirectory != null) {
            Path cacheRoot = Path.of(Event.PHOTO_SAVE_ROOT, Event.PHOTO_RELATIVE_PATH)
                    .toAbsolutePath().normalize();
            Path target = cacheEventDirectory.toAbsolutePath().normalize();
            assertTrue(target.startsWith(cacheRoot));
            assertTrue(target.getFileName().toString().startsWith("STORAGE_TEST_"));
            if (Files.exists(target)) {
                try (Stream<Path> files = Files.walk(target)) {
                    for (Path file : files.sorted(Comparator.reverseOrder()).toList()) {
                        Files.delete(file);
                    }
                }
            }
        }
        if (cacheSaveFile != null) {
            Files.deleteIfExists(cacheSaveFile);
        }
    }

    @Test
    void save_installationFails_restoresPreviousPhotoSnapshotAndCleansStaging() throws IOException {
        Path originalPhoto = createOldSnapshot();
        byte[] originalContents = Files.readAllBytes(originalPhoto);
        IOException installationFailure = new IOException("Simulated installation failure");
        List<Path> moves = new ArrayList<>();
        SaveLoadManager storage = new SaveLoadManager((source, destination) -> {
            moves.add(source);
            if (source.getFileName().toString().startsWith("photos-next-")) {
                throw installationFailure;
            }
            Files.move(source, destination);
        });

        IOException error = assertThrows(IOException.class,
                () -> storage.save(new Club(), testDirectory.resolve("club.cf")));

        assertSame(installationFailure, error);
        assertEquals(3, moves.size(), "Replacement must attempt backup, installation, and rollback");
        assertArrayEquals(originalContents, Files.readAllBytes(originalPhoto));
        assertEquals(0, error.getSuppressed().length);
        assertNoTemporarySnapshots();
    }

    @Test
    void save_installationFailsWithoutBackup_removesStagingAndLeavesDestinationAbsent() throws IOException {
        IOException installationFailure = new IOException("Simulated installation failure");
        List<Path> moves = new ArrayList<>();
        SaveLoadManager storage = new SaveLoadManager((source, destination) -> {
            moves.add(source);
            throw installationFailure;
        });

        IOException error = assertThrows(IOException.class,
                () -> storage.save(new Club(), testDirectory.resolve("club.cf")));

        assertSame(installationFailure, error);
        assertEquals(1, moves.size(), "There is no previous snapshot to back up or restore");
        assertFalse(Files.exists(testDirectory.resolve("photos")));
        assertNoTemporarySnapshots();
    }

    @Test
    void save_rollbackAlsoFails_preservesBackupAndReportsBothFailures() throws IOException {
        Path originalPhoto = createOldSnapshot();
        byte[] originalContents = Files.readAllBytes(originalPhoto);
        IOException installationFailure = new IOException("Simulated installation failure");
        IOException rollbackFailure = new IOException("Simulated rollback failure");
        SaveLoadManager storage = new SaveLoadManager((source, destination) -> {
            String name = source.getFileName().toString();
            if (name.startsWith("photos-next-")) {
                throw installationFailure;
            }
            if (name.startsWith("photos-old-")) {
                throw rollbackFailure;
            }
            Files.move(source, destination);
        });

        IOException error = assertThrows(IOException.class,
                () -> storage.save(new Club(), testDirectory.resolve("club.cf")));

        assertSame(installationFailure, error);
        assertArrayEquals(new Throwable[]{rollbackFailure}, error.getSuppressed());
        assertFalse(Files.exists(testDirectory.resolve("photos")));
        List<Path> backups;
        try (Stream<Path> paths = Files.list(testDirectory)) {
            backups = paths.filter(path -> path.getFileName().toString().startsWith("photos-old-")).toList();
        }
        assertEquals(1, backups.size(), "The original snapshot must remain available for recovery");
        assertArrayEquals(originalContents, Files.readAllBytes(backups.getFirst().resolve("OLD/keep.png")));
        try (Stream<Path> paths = Files.list(testDirectory)) {
            assertTrue(paths.noneMatch(path -> path.getFileName().toString().startsWith("photos-next-")));
        }
    }

    @Test
    void save_backupMoveFails_preservesOriginalSnapshotAndCleansStaging() throws IOException {
        Path originalPhoto = createOldSnapshot();
        byte[] originalContents = Files.readAllBytes(originalPhoto);
        IOException backupFailure = new IOException("Simulated backup failure");
        SaveLoadManager storage = new SaveLoadManager((source, destination) -> {
            throw backupFailure;
        });

        IOException error = assertThrows(IOException.class,
                () -> storage.save(new Club(), testDirectory.resolve("club.cf")));

        assertSame(backupFailure, error);
        assertArrayEquals(originalContents, Files.readAllBytes(originalPhoto));
        assertNoTemporarySnapshots();
    }

    @Test
    void save_danglingAssignments_rejectsBeforeOverwritingExistingData() throws IOException {
        Path saveFile = Files.writeString(testDirectory.resolve("club.cf"), "keep existing records");
        Path originalPhoto = createOldSnapshot();
        for (boolean missingMember : List.of(true, false)) {
            Member knownMember = new Member("Alice", "alice", "", List.of());
            Event knownEvent = new Event("AGM", "Meeting", null, null, 0, "", "");
            Member assignedMember = missingMember ? new Member("Alice", "alice", "", List.of()) : knownMember;
            Event assignedEvent = missingMember ? knownEvent : new Event("OTHER", "", null, null, 0, "", "");
            EventAssignment dangling = new EventAssignment(assignedEvent, assignedMember, "Host");
            // Simulate an inconsistent store at the persistence boundary without using reflection.
            Club inconsistent = new Club() {
                @Override
                public List<EventAssignment> getAssignments() {
                    return List.of(dangling);
                }
            };
            inconsistent.addMember(knownMember);
            inconsistent.addEvent(knownEvent);

            IOException error = assertThrows(IOException.class,
                    () -> new SaveLoadManager().save(inconsistent, saveFile));

            assertEquals("Cannot save an assignment with a missing member or event", error.getMessage());
            assertEquals("keep existing records", Files.readString(saveFile));
            assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(originalPhoto));
        }
    }

    @Test
    void load_wrongAssignmentFieldCount_reportsRecordLineAndPreservesFile() throws IOException {
        Path saveFile = testDirectory.resolve("invalid.cf");
        String validRecords = "MEMBER|Alice|alice||\nEVENT|AGM|Meeting|null|null|0.0||\n";
        for (String assignment : List.of("ASSIGNMENT", "ASSIGNMENT|AGM|1", "ASSIGNMENT|AGM|1|Host|extra")) {
            String contents = validRecords + assignment + "\n";
            Files.writeString(saveFile, contents);

            IOException error = assertThrows(IOException.class, () -> new SaveLoadManager().load(saveFile));

            assertEquals("Invalid assignment record at line 3", error.getMessage());
            assertEquals(contents, Files.readString(saveFile));
        }
    }

    @Test
    void saveAndLoad_cacheIsAlsoSnapshot_preservesPhotosAndRegistersOnlyImageFiles() throws IOException {
        String tag = "STORAGE_TEST_" + UUID.randomUUID();
        cacheSaveFile = Path.of(Event.PHOTO_SAVE_ROOT, tag + ".cf");
        cacheEventDirectory = Path.of(Event.PHOTO_SAVE_ROOT, Event.PHOTO_RELATIVE_PATH, tag);
        Files.createDirectories(cacheEventDirectory.resolve("directory.png"));
        Path photo = Files.write(cacheEventDirectory.resolve("portrait.png"), new byte[]{7, 8, 9});
        Path notes = Files.writeString(cacheEventDirectory.resolve("notes.txt"), "keep notes");
        Club club = new Club();
        club.addEvent(new Event(tag, "Same cache", null, null, 0, "", ""));
        SaveLoadManager storage = new SaveLoadManager();

        storage.save(club, cacheSaveFile);
        Club restored = storage.load(cacheSaveFile);

        assertEquals(List.of("portrait"), restored.getEvents().getFirst().getPhotoNames());
        assertArrayEquals(new byte[]{7, 8, 9}, Files.readAllBytes(photo));
        assertEquals("keep notes", Files.readString(notes));
        assertTrue(Files.isDirectory(cacheEventDirectory.resolve("directory.png")));
    }

    /** Creates the previous snapshot that rollback tests must protect. */
    private Path createOldSnapshot() throws IOException {
        Path folder = Files.createDirectories(testDirectory.resolve("photos/OLD"));
        return Files.write(folder.resolve("keep.png"), new byte[]{1, 2, 3});
    }

    /** Checks that a completed rollback or early failure has left no staging or backup directories. */
    private void assertNoTemporarySnapshots() throws IOException {
        try (Stream<Path> paths = Files.list(testDirectory)) {
            assertTrue(paths.noneMatch(path -> path.getFileName().toString().startsWith("photos-next-")
                    || path.getFileName().toString().startsWith("photos-old-")));
        }
    }
}
