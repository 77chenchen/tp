package clubflow.command.photo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import clubflow.Club;
import clubflow.SaveLoadManager;
import clubflow.UserInterface;
import clubflow.command.CommandParser;
import clubflow.event.Event;
import clubflow.exception.CommandParseException;
import clubflow.exception.PhotoLoadException;
import clubflow.exception.PhotoOpenException;
import clubflow.exception.PhotoRenameException;
import clubflow.photo.Photo;

/**
 * Tests photo command output, failed renames, and persistence using isolated image files.
 * Desktop-opening commands use a small event substitute to avoid launching external applications.
 */
class PhotoCommandsTest {
    @TempDir
    Path testDirectory;

    private final PrintStream originalOutput = System.out;
    private ByteArrayOutputStream output;
    private PrintStream capturedOutput;
    private Club club;
    private Event event;
    private CommandParser parser;
    private Path photoDirectory;

    @BeforeEach
    void setUp() {
        output = new ByteArrayOutputStream();
        capturedOutput = new PrintStream(output, true, StandardCharsets.UTF_8);
        System.setOut(capturedOutput);
        club = new Club();
        event = new Event("PHOTO_TEST_" + UUID.randomUUID(), "Photos", null, null, 0, "", "");
        club.addEvent(event);
        club.markSaved();
        photoDirectory = Path.of(Event.PHOTO_SAVE_ROOT, Event.PHOTO_RELATIVE_PATH, event.getTag());
        UserInterface ui = new UserInterface("[TEST] ", "> ");
        parser = new CommandParser(ui);
        parser.register("renamePhoto", new RenamePhotoCommand(ui, club));
        parser.registerAlias("renamePhoto", "rp");
        parser.register("addPhoto", new AddPhotoCommand(ui, club));
        parser.register("listPhoto", new ListPhotoCommand(ui, club));
        parser.register("openPhoto", new OpenPhotoCommand(ui, club));
    }

    @AfterEach
    void cleanUp() throws IOException {
        System.setOut(originalOutput);
        capturedOutput.close();
        Path cacheRoot = Path.of(Event.PHOTO_SAVE_ROOT, Event.PHOTO_RELATIVE_PATH).toAbsolutePath().normalize();
        Path target = photoDirectory.toAbsolutePath().normalize();
        assertTrue(target.startsWith(cacheRoot));
        assertEquals(event.getTag(), target.getFileName().toString());
        if (Files.exists(target)) {
            try (Stream<Path> paths = Files.walk(target)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
    }

    @Test
    void renamePhoto_alias_movesFileUpdatesRegistrationAndMarksUnsaved() throws Exception {
        Photo photo = event.addPhoto(createImage("source.png").toString(), "old");
        Path oldPath = Path.of(photo.getPath());
        byte[] image = Files.readAllBytes(oldPath);

        assertFalse(parser.parse("rp e/" + event.getTag() + " n/old new/renamed"));

        assertEquals(List.of("renamed"), event.getPhotoNames());
        assertFalse(Files.exists(oldPath));
        assertEquals(photoDirectory.resolve("renamed.png").toString(), photo.getPath());
        assertArrayEquals(image, Files.readAllBytes(Path.of(photo.getPath())));
        assertTrue(club.hasUnsavedChanges());
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Renamed photo \"old\" to \"renamed\""));
    }

    @Test
    void renamePhoto_saveAndReload_preservesNewNameAndImage() throws Exception {
        event.addPhoto(createImage("source.png").toString(), "old");
        parser.parse("renamePhoto e/" + event.getTag() + " n/old new/renamed");
        Path saveFile = testDirectory.resolve("club.cf");
        SaveLoadManager manager = new SaveLoadManager();

        manager.save(club, saveFile);
        Club restored = manager.load(saveFile);

        assertEquals(List.of("renamed"), restored.getEvents().getFirst().getPhotoNames());
        assertTrue(Files.isRegularFile(testDirectory.resolve("photos/" + event.getTag() + "/renamed.png")));
        assertFalse(Files.exists(testDirectory.resolve("photos/" + event.getTag() + "/old.png")));
        assertTrue(Files.isRegularFile(photoDirectory.resolve("renamed.png")));
    }

    @Test
    void renamePhoto_occupiedName_preservesFilesRegistrationsAndSavedState() throws Exception {
        event.addPhoto(createImage("one.png").toString(), "one");
        event.addPhoto(createImage("two.png").toString(), "two");
        byte[] first = Files.readAllBytes(photoDirectory.resolve("one.png"));
        byte[] second = Files.readAllBytes(photoDirectory.resolve("two.png"));

        assertThrows(PhotoRenameException.class,
                () -> parser.parse("renamePhoto e/" + event.getTag() + " n/one new/two"));

        assertEquals(2, event.getPhotoNames().size());
        assertTrue(event.getPhotoNames().containsAll(List.of("one", "two")));
        assertArrayEquals(first, Files.readAllBytes(photoDirectory.resolve("one.png")));
        assertArrayEquals(second, Files.readAllBytes(photoDirectory.resolve("two.png")));
        assertFalse(club.hasUnsavedChanges());
        assertFalse(output.toString(StandardCharsets.UTF_8).contains("Renamed photo"));
    }

    @Test
    void renamePhoto_missingPhoto_reportsFailureWithoutDirtyingClub() {
        PhotoRenameException error = assertThrows(PhotoRenameException.class,
                () -> parser.parse("renamePhoto e/" + event.getTag() + " n/missing new/renamed"));

        assertTrue(error.getMessage().contains("No photo has name \"missing\""));
        assertTrue(event.getPhotoNames().isEmpty());
        assertFalse(club.hasUnsavedChanges());
    }

    @Test
    void renamePhoto_invalidArguments_rejectsBeforeChangingData() {
        for (String input : List.of("renamePhoto", "renamePhoto e/" + event.getTag() + " n/old",
                "renamePhoto e/MISSING n/old new/renamed")) {
            assertThrows(CommandParseException.class, () -> parser.parse(input), input);
        }
        assertFalse(club.hasUnsavedChanges());
    }

    @Test
    void renamePhoto_fileDisappeared_preservesOriginalRegistration() {
        event.loadPhoto("missing");

        assertThrows(PhotoRenameException.class,
                () -> parser.parse("renamePhoto e/" + event.getTag() + " n/missing new/renamed"));

        assertEquals(List.of("missing"), event.getPhotoNames());
        assertFalse(club.hasUnsavedChanges());
    }

    @Test
    void addPhoto_extensionlessSource_usesWholeFilenameAsPhotoName() throws Exception {
        Path source = createImage("snapshot");

        assertFalse(parser.parse("addPhoto e/" + event.getTag() + " p/\"" + source + "\""));

        assertEquals(List.of("snapshot"), event.getPhotoNames());
        assertTrue(Files.isRegularFile(photoDirectory.resolve("snapshot.png")));
        assertTrue(club.hasUnsavedChanges());
    }

    @Test
    void addPhoto_invalidDerivedName_rejectsRootAndMalformedPaths() {
        String root = testDirectory.toAbsolutePath().getRoot().toString();
        for (String path : List.of(root, "bad\0path")) {
            assertThrows(CommandParseException.class,
                    () -> parser.parse("addPhoto e/" + event.getTag() + " p/\"" + path + "\""));
        }
        assertTrue(event.getPhotoNames().isEmpty());
        assertFalse(club.hasUnsavedChanges());
    }

    @Test
    void addPhotos_subdirectoryAndExtensionlessImage_importsOnlyFiles() throws Exception {
        createImage("snapshot");
        Files.createDirectory(testDirectory.resolve("nested"));

        assertArrayEquals(new int[]{1, 0}, event.addPhotos(testDirectory.toString()));

        assertEquals(List.of("snapshot"), event.getPhotoNames());
        assertTrue(Files.isRegularFile(photoDirectory.resolve("snapshot.png")));
    }

    @Test
    void addPhotos_fileOrMissingDirectory_reportsInvalidDirectory() throws IOException {
        Path source = createImage("source.png");

        assertThrows(PhotoLoadException.class, () -> event.addPhotos(source.toString()));
        assertThrows(PhotoLoadException.class, () -> event.addPhotos(testDirectory.resolve("missing").toString()));
        assertTrue(event.getPhotoNames().isEmpty());
    }

    @Test
    void listPhoto_selectedEvent_numbersNamesAndLeavesClubSaved() throws Exception {
        event.loadPhoto("one");
        event.loadPhoto("two");
        club.addEvent(new Event("OTHER", "Other", null, null, 0, "", ""));
        club.markSaved();

        assertFalse(parser.parse("listPhoto e/" + event.getTag()));

        String result = output.toString(StandardCharsets.UTF_8);
        assertTrue(result.contains("Photos of " + event.getTag() + ":"));
        assertEquals(2, result.lines().filter(line -> line.matches("\\[TEST]   [12]\\. (one|two)")).count());
        assertFalse(result.contains("Photos of OTHER"));
        assertFalse(club.hasUnsavedChanges());
    }

    @Test
    void openPhoto_directoryCreationFails_reportsFailureWithoutLaunchingDesktop() throws Exception {
        Files.createDirectories(photoDirectory.getParent());
        Files.writeString(photoDirectory, "blocked");

        PhotoOpenException error = assertThrows(PhotoOpenException.class,
                () -> parser.parse("openPhoto e/" + event.getTag()));

        assertTrue(error.getMessage().contains("Cannot open the photo directory"));
        assertEquals("blocked", Files.readString(photoDirectory));
        assertFalse(club.hasUnsavedChanges());
    }

    @Test
    void openPhoto_availableDirectory_delegatesOnceAndPrintsSuccess() throws Exception {
        OpeningEvent openingEvent = new OpeningEvent();
        club.addEvent(openingEvent);
        club.markSaved();

        assertFalse(parser.parse("openPhoto e/OPEN"));

        assertEquals(1, openingEvent.directoryOpenCount);
        assertEquals(0, openingEvent.photoOpenCount);
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Opened directory of photos in event \"OPEN\"."));
        assertFalse(club.hasUnsavedChanges());
    }

    @Test
    void openPhoto_availablePhoto_delegatesNameAndPrintsSuccess() throws Exception {
        OpeningEvent openingEvent = new OpeningEvent();
        club.addEvent(openingEvent);
        club.markSaved();

        assertFalse(parser.parse("openPhoto e/OPEN n/snapshot"));

        assertEquals("snapshot", openingEvent.openedName);
        assertEquals(1, openingEvent.photoOpenCount);
        assertEquals(0, openingEvent.directoryOpenCount);
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Opened photo \"snapshot\" in event \"OPEN\"."));
        assertFalse(club.hasUnsavedChanges());
    }

    /** Creates a PNG image even when the supplied filename has no extension. */
    private Path createImage(String name) throws IOException {
        Path source = testDirectory.resolve(name);
        assertTrue(ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", source.toFile()));
        return source;
    }

    /** Records opening requests while keeping desktop applications out of command tests. */
    private static class OpeningEvent extends Event {
        private int directoryOpenCount;
        private int photoOpenCount;
        private String openedName;

        OpeningEvent() {
            super("OPEN", "Open", null, null, 0, "", "");
        }

        @Override
        public boolean openPhotoDir() {
            directoryOpenCount++;
            return true;
        }

        @Override
        public boolean openPhoto(String name) {
            photoOpenCount++;
            openedName = name;
            return true;
        }
    }
}
