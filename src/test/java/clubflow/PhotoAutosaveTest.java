package clubflow;

import clubflow.command.AddPhotoCommand;
import clubflow.command.CommandParser;
import clubflow.command.DeletePhotoCommand;
import clubflow.event.Event;
import clubflow.exception.PhotoDeleteException;
import clubflow.exception.PhotoLoadException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/** Checks that photo commands correctly trigger or skip autosave. */
class PhotoAutosaveTest {
    private static final String EVENT_TAG = "MULTICLUB_AUTOSAVE_TEST";

    @TempDir
    Path testDirectory;

    @AfterEach
    void cleanUpPhotoCache() throws IOException {
        Path photoDirectory = Path.of(Event.PHOTO_SAVE_ROOT,
                Event.PHOTO_RELATIVE_PATH, EVENT_TAG);
        if (Files.isDirectory(photoDirectory)) {
            try (var entries = Files.walk(photoDirectory)) {
                for (Path path : entries.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
    }

    private CommandParser createParser(Club club) {
        UserInterface ui = new UserInterface("[CF] ", "> ");
        CommandParser parser = new CommandParser(ui);
        parser.register("addPhoto", new AddPhotoCommand(ui, club));
        parser.register("deletePhoto", new DeletePhotoCommand(ui, club));
        return parser;
    }

    private Path createImage() throws IOException {
        Path image = testDirectory.resolve("source.png");
        BufferedImage contents = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(contents, "png", image.toFile());
        return image;
    }

    @Test
    void addPhoto_success_marksClubUnsaved() throws Exception {
        Club club = new Club();
        Event event = new Event(EVENT_TAG, "Event", null, null, 0, "", "");
        club.addEvent(event);
        club.markSaved();

        createParser(club).parse("addPhoto e/" + EVENT_TAG
                + " n/image p/\"" + createImage() + "\"");

        assertTrue(club.hasUnsavedChanges());
        assertEquals(1, event.getPhotoNames().size());
    }

    @Test
    void addPhoto_failedImport_doesNotMarkClubUnsaved() throws Exception {
        Club club = new Club();
        club.addEvent(new Event(EVENT_TAG, "Event", null, null, 0, "", ""));
        club.markSaved();

        assertThrows(PhotoLoadException.class, () ->
                createParser(club).parse("addPhoto e/" + EVENT_TAG
                        + " n/missing p/\"" + testDirectory.resolve("missing.png") + "\""));

        assertFalse(club.hasUnsavedChanges());
    }

    @Test
    void deletePhoto_existingPhoto_marksClubUnsaved() throws Exception {
        Club club = new Club();
        Event event = new Event(EVENT_TAG, "Event", null, null, 0, "", "");
        club.addEvent(event);
        CommandParser parser = createParser(club);
        parser.parse("addPhoto e/" + EVENT_TAG + " n/image p/\"" + createImage() + "\"");
        club.markSaved();

        parser.parse("deletePhoto e/" + EVENT_TAG + " n/image");

        assertTrue(club.hasUnsavedChanges());
        assertTrue(event.getPhotoNames().isEmpty());
    }

    @Test
    void deletePhoto_missingPhoto_doesNotMarkClubUnsaved() throws Exception {
        Club club = new Club();
        club.addEvent(new Event(EVENT_TAG, "Event", null, null, 0, "", ""));
        club.markSaved();

        assertThrows(PhotoDeleteException.class, () ->
                createParser(club).parse("deletePhoto e/" + EVENT_TAG + " n/missing"));

        assertFalse(club.hasUnsavedChanges());
    }

    @Test
    void deletePhoto_allMarksUnsavedOnlyWhenPhotosRemoved() throws Exception {
        Club club = new Club();
        Event event = new Event(EVENT_TAG, "Event", null, null, 0, "", "");
        club.addEvent(event);
        CommandParser parser = createParser(club);
        club.markSaved();

        parser.parse("deletePhoto e/" + EVENT_TAG + " all");
        assertFalse(club.hasUnsavedChanges());

        parser.parse("addPhoto e/" + EVENT_TAG + " n/image p/\"" + createImage() + "\"");
        club.markSaved();
        parser.parse("deletePhoto e/" + EVENT_TAG + " all");

        assertTrue(club.hasUnsavedChanges());
        assertTrue(event.getPhotoNames().isEmpty());
    }
}
