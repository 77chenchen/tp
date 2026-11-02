package clubflow.photo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import clubflow.exception.PhotoDeleteException;
import clubflow.ProjectTempDirectory;
import clubflow.exception.PhotoLoadException;
import clubflow.exception.PhotoOpenException;
import clubflow.exception.PhotoRenameException;

/**
 * Exercises photo conversion and filesystem failures without opening an image viewer.
 */
class PhotoFileTest {
    @TempDir(factory = ProjectTempDirectory.class)
    Path testDirectory;

    @Test
    void constructor_bitmapSource_convertsToPngAndPreservesPixels() throws Exception {
        Path source = createImage();
        Path destination = testDirectory.resolve("nested/photos");

        Photo photo = new Photo(source.toString(), relative(destination), "converted");

        Path saved = Path.of(photo.getPath());
        assertEquals(destination.resolve("converted.png"), saved.toAbsolutePath().normalize());
        assertTrue(Files.isRegularFile(source));
        assertArrayEquals(new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10},
                Arrays.copyOf(Files.readAllBytes(saved), 8));
        BufferedImage imported = ImageIO.read(saved.toFile());
        assertEquals(3, imported.getWidth());
        assertEquals(2, imported.getHeight());
        assertEquals(0xFF12AB34, imported.getRGB(1, 1));
    }

    @Test
    void constructor_invalidNames_rejectsWithoutCreatingDestination() throws IOException {
        Path source = createImage();
        Path destination = testDirectory.resolve("photos");

        for (String name : Arrays.asList(null, "", "   ", "..", "a..b", "folder/photo", "folder\\photo")) {
            assertThrows(PhotoLoadException.class,
                    () -> new Photo(source.toString(), relative(destination), name), "Name: " + name);
        }

        assertFalse(Files.exists(destination));
        assertTrue(Files.isRegularFile(source));
    }

    @Test
    void constructor_parentIsFile_wrapsDirectoryCreationFailure() throws IOException {
        Path source = createImage();
        Path blocker = testDirectory.resolve("blocked");
        Files.writeString(blocker, "keep this file");

        PhotoLoadException error = assertThrows(PhotoLoadException.class,
                () -> new Photo(source.toString(), relative(blocker.resolve("photos")), "photo"));

        assertTrue(error.getMessage().contains("Failed to create destination folders."));
        assertEquals("keep this file", Files.readString(blocker));
    }

    @Test
    void rename_validName_movesFileAndUpdatesPath() throws Exception {
        Path original = createImage();
        byte[] image = Files.readAllBytes(original);
        Photo photo = new Photo(original.toString());

        photo.rename("renamed");

        Path renamed = testDirectory.resolve("renamed.png");
        assertEquals(renamed.toString(), photo.getPath());
        assertFalse(Files.exists(original));
        assertArrayEquals(image, Files.readAllBytes(renamed));
        photo.delete();
        assertFalse(Files.exists(renamed));
    }

    @Test
    void rename_invalidNames_preservesOriginalFileAndPath() throws IOException {
        Path original = createImage();
        byte[] image = Files.readAllBytes(original);
        Photo photo = new Photo(original.toString());

        for (String name : Arrays.asList(null, "", "   ", "..", "a..b", "folder/photo", "folder\\photo")) {
            assertThrows(PhotoRenameException.class, () -> photo.rename(name), "Name: " + name);
            assertEquals(original.toString(), photo.getPath());
            assertArrayEquals(image, Files.readAllBytes(original));
        }
    }

    @Test
    void rename_existingTarget_doesNotOverwriteEitherFile() throws IOException {
        Path original = createImage();
        Path occupied = testDirectory.resolve("occupied.png");
        Files.writeString(occupied, "existing photo");
        byte[] image = Files.readAllBytes(original);
        Photo photo = new Photo(original.toString());

        assertThrows(PhotoRenameException.class, () -> photo.rename("occupied"));

        assertEquals(original.toString(), photo.getPath());
        assertArrayEquals(image, Files.readAllBytes(original));
        assertEquals("existing photo", Files.readString(occupied));
    }

    @Test
    void rename_sameName_doesNotChangeFile() throws IOException {
        Path original = testDirectory.resolve("same.png");
        Files.writeString(original, "original");
        Photo photo = new Photo(original.toString());

        assertThrows(PhotoRenameException.class, () -> photo.rename("same"));

        assertEquals(original.toString(), photo.getPath());
        assertEquals("original", Files.readString(original));
    }

    @Test
    void rename_missingSource_wrapsFailureAndKeepsOriginalPath() {
        Path original = testDirectory.resolve("missing.png");
        Photo photo = new Photo(original.toString());

        PhotoRenameException error = assertThrows(PhotoRenameException.class, () -> photo.rename("renamed"));

        assertTrue(error.getMessage().contains("IO error"));
        assertEquals(original.toString(), photo.getPath());
        assertFalse(Files.exists(testDirectory.resolve("renamed.png")));
    }

    @Test
    void delete_nonEmptyDirectory_wrapsFailureAndPreservesContents() throws IOException {
        Path directory = Files.createDirectory(testDirectory.resolve("photos"));
        Path child = Files.writeString(directory.resolve("keep.txt"), "keep");
        Photo photo = new Photo(directory.toString());

        assertThrows(PhotoDeleteException.class, photo::delete);

        assertEquals("keep", Files.readString(child));
    }

    @Test
    void open_missingFile_wrapsFailureWithoutLaunchingViewer() {
        Photo photo = new Photo(testDirectory.resolve("missing.png").toString());

        assertThrows(PhotoOpenException.class, photo::open);
    }

    /** Creates a small bitmap whose pixel value can be checked after PNG conversion. */
    private Path createImage() throws IOException {
        Path source = testDirectory.resolve("source.bmp");
        BufferedImage image = new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB);
        image.setRGB(1, 1, 0x12AB34);
        assertTrue(ImageIO.write(image, "bmp", source.toFile()));
        return source;
    }

    /** Photo import accepts relative destinations, including paths to the JUnit temporary directory. */
    private String relative(Path path) {
        return Path.of("").toAbsolutePath().relativize(path.toAbsolutePath()).toString();
    }
}
