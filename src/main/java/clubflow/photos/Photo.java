package clubflow.photos;

import clubflow.exception.PhotoDeleteException;
import clubflow.exception.PhotoLoadException;
import clubflow.exception.PhotoOpenException;

import javax.imageio.ImageIO;
import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

//@@author czfray
/**
 * Represents a photo stored as a PNG file in a relative directory.
 * Supports importing, opening, and deleting photos.
 */
public class Photo {

    private static final String FORMAT = ".png";

    private String relativePath;

    /**
     * Creates a photo by loading an image from the specified source path,
     * converting it to PNG format, and saving it to the destination directory.
     *
     * @param sourcePath path to the original image file
     * @param destPath relative path to the destination directory
     * @param name name of the saved image file, excluding the extension
     * @throws PhotoLoadException if the source file is invalid, the image format
     *                            is unsupported, or the image cannot be saved
     */
    public Photo(String sourcePath, String destPath, String name) throws PhotoLoadException {
        BufferedImage image;
        Path source = Path.of(sourcePath);
        Path destFolderPath = Path.of(destPath);
        Path destFilePath = destFolderPath.resolve(name + FORMAT);

        if (Files.exists(destFilePath)) {
            throw new PhotoLoadException("Photo with name \"" + name + "\" already exists.");
        }

        if (destFolderPath.isAbsolute()) {
            throw new PhotoLoadException("Destination path must be relative.");
        }

        if (!Files.isRegularFile(source)) {
            throw new PhotoLoadException("File in path \"" + sourcePath +"\" does not exist or is not regular.");
        }

        try{
            image = ImageIO.read(source.toFile());
            if (image == null) {
                throw new IOException();
            }
        } catch (IOException e) {
            throw new PhotoLoadException("File is not a supported image.");
        }

        try {
            Files.createDirectories(destFolderPath);
        }
        catch(IOException e){
            throw new PhotoLoadException("Failed to create destination folders.");
        }

        try {
            if (!ImageIO.write(image, "png", destFilePath.toFile())) {
                throw new IOException();
            }
            this.relativePath = destFilePath.toString();
        }
        catch(IOException e){
            throw new PhotoLoadException("Failed to copy image into destination folders.");
        }
    }

    /**
     * Returns the relative path of the saved photo.
     *
     * @return relative path of the photo
     */
    public String getPath() {
        return relativePath;
    }

    /**
     * Opens the photo using the system's default image viewer.
     *
     * @throws PhotoOpenException if the photo cannot be opened
     */
    public void open() throws PhotoOpenException {
        try{
            Desktop.getDesktop().open(Path.of(relativePath).toFile());
        } catch (Exception e) {
            throw new PhotoOpenException("IO exception occurred.");
        }
    }

    /**
     * Deletes the saved photo from the file system.
     *
     * @throws PhotoDeleteException if the photo cannot be deleted
     */
    public void delete() throws PhotoDeleteException {
        try {
            Files.delete(Path.of(relativePath));
        } catch (IOException | SecurityException e) {
            throw new PhotoDeleteException("IO Exception occurred.");
        }
    }
}
