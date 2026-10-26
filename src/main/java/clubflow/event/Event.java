//@@author camimaigrot
package clubflow.event;

import clubflow.exception.PhotoDeleteException;
import clubflow.exception.PhotoLoadException;
import clubflow.exception.PhotoOpenException;
import clubflow.photo.Photo;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a club event.
 */
public class Event {
    public static final String PHOTO_SAVE_ROOT = "cache/";
    public static final String PHOTO_RELATIVE_PATH = "photos/";

    private static final Logger LOGGER = Logger.getLogger(Event.class.getName());

    private final String tag;
    private final String name;
    private final LocalDate date;
    private final LocalDate deadline;
    private final double budget;
    private final String venue;
    private final String logistics;
    private HashMap<String, Photo> photos;

    /** Whether this event has been marked as completed. */
    private final boolean completed;

    //@@author camimaigrot
    /**
     * Creates an event after validating values required by the domain and save format.
     * Dates may be null because they are optional.
     *
     * @param tag unique non-blank event tag
     * @param name event name, or an empty string
     * @param date optional event date
     * @param deadline optional planning deadline
     * @param budget finite non-negative event budget
     * @param venue event venue, or an empty string
     * @param logistics logistics notes, or an empty string
     */
    public Event(String tag, String name, LocalDate date, LocalDate deadline,
                 double budget, String venue, String logistics) {
        this(tag, name, date, deadline, budget, venue, logistics, false);
    }

    /**
     * Creates an event with an explicit completion state, including when loading saved data.
     *
     * @param tag unique non-blank event tag
     * @param name event name, or an empty string
     * @param date optional event date
     * @param deadline optional planning deadline
     * @param budget finite non-negative event budget
     * @param venue event venue, or an empty string
     * @param logistics logistics notes, or an empty string
     * @param completed whether the event is completed
     */
    public Event(String tag, String name, LocalDate date, LocalDate deadline,
                 double budget, String venue, String logistics, boolean completed) {
        this.tag = requireNonBlank(tag, "Event tag");
        this.name = requireText(name, "Event name");
        this.date = date;
        this.deadline = deadline;
        if (!Double.isFinite(budget) || budget < 0) {
            throw new IllegalArgumentException("Budget must be finite and non-negative");
        }
        this.budget = budget;
        this.venue = requireText(venue, "Venue");
        this.logistics = requireText(logistics, "Logistics");
        this.photos = new HashMap<>();
        this.completed = completed;
    }

    /**
     * Validates an optional text field against null and the save-file separator.
     *
     * @param value field value
     * @param description field name used in the exception message
     * @return the validated value
     */
    private static String requireText(String value, String description) {
        Objects.requireNonNull(value, description + " must not be null");
        if (value.contains("|")) {
            throw new IllegalArgumentException(description + " must not contain |");
        }
        return value;
    }

    /**
     * Validates a required text field.
     *
     * @param value field value
     * @param description field name used in the exception message
     * @return the validated value
     */
    private static String requireNonBlank(String value, String description) {
        String checkedValue = requireText(value, description);
        if (checkedValue.isBlank()) {
            throw new IllegalArgumentException(description + " must not be blank");
        }
        return checkedValue;
    }

    public String getTag() {
        return tag;
    }

    public String getName() {
        return name;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public double getBudget() {
        return budget;
    }

    public String getVenue() {
        return venue;
    }

    public String getLogistics() {
        return logistics;
    }

    //@@author Eugene3129
    public boolean isCompleted() {
        return completed;
    }

    /**
     * Returns a copy with the requested completion state, leaving this event unchanged.
     *
     * @param completed desired completion state
     * @return event with the same details and the supplied completion state
     */
    public Event withCompleted(boolean completed) {
        return new Event(tag, name, date, deadline, budget, venue, logistics, completed);
    }

    //@@author czfray
    public List<String> getPhotoNames(){
        return new ArrayList<>(photos.keySet());
    }

    /**
     * Adds a photo to the event by copying and converting it to PNG format.
     * Stores the photo in the event's directory and registers it by name.
     *
     * @param photoPath path to the original image file
     * @param name name of the photo, excluding the file extension
     * @return the newly created Photo object
     * @throws PhotoLoadException if the photo cannot be loaded or saved
     */
    public Photo addPhoto(String photoPath, String name) throws PhotoLoadException {
        Photo p = new Photo(photoPath, PHOTO_SAVE_ROOT + PHOTO_RELATIVE_PATH + tag, name);
        photos.put(name, p);
        return p;
    }

    /**
     * Adds all photos from a directory to the event.
     * Continues loading remaining photos if one fails.
     *
     * @param photosDirPath path to the directory containing the photos
     * @return int array containing the number of successful and failed imports
     * @throws PhotoLoadException if the directory is invalid or cannot be read
     */
    public int[] addPhotos(String photosDirPath) throws PhotoLoadException {
        File directory = new File(photosDirPath);

        if (!directory.isDirectory()) {
            throw new PhotoLoadException("Invalid photo directory: " + photosDirPath);
        }

        File[] files = directory.listFiles();

        if (files == null) {
            throw new PhotoLoadException("Cannot read photo directory: " + photosDirPath);
        }

        int[] result = new int[2];

        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }

            String filename = file.getName();
            int dotIndex = filename.lastIndexOf('.');
            String name = dotIndex > 0 ? filename.substring(0, dotIndex) : filename;

            try {
                addPhoto(file.getAbsolutePath(), name);
                result[0]++;
            } catch (PhotoLoadException e) {
                LOGGER.log(Level.WARNING, e.getMessage() + "(" + filename + ")");
                result[1]++;
            }
        }

        return result;
    }

    /**
     * Loads an existing photo from the event's local photo directory
     * and registers it in the event's photo collection.
     *
     * @param name name of the photo, excluding the file extension
     * @return the loaded Photo object
     */
    public Photo loadPhoto(String name){
        Photo p = new Photo(PHOTO_SAVE_ROOT + PHOTO_RELATIVE_PATH + tag + "/" + name + Photo.PHOTO_FORMAT);
        photos.put(name, p);
        return p;
    }

    /**
     * Deletes a photo with the specified name from the file system.
     *
     * @param name name of the photo to delete
     * @return true if the photo is found and deleted, false if not found
     * @throws PhotoDeleteException if the photo cannot be deleted
     */
    public boolean deletePhoto(String name) throws PhotoDeleteException {
        if (!photos.containsKey(name)){
            return false;
        }
        photos.get(name).delete();
        photos.remove(name);
        return true;
    }

    /**
     * Attempts to delete all photos belonging to the event.
     *
     * @return int array containing the number of successful and failed deletions
     */
    public int[] deleteAllPhotos() {
        int[] result = new int[2];

        for (String name : new ArrayList<>(photos.keySet())) {
            try {
                photos.get(name).delete();
                photos.remove(name);
                result[0]++;
            } catch (PhotoDeleteException e) {
                LOGGER.log(Level.WARNING, e.getMessage() + "(" + name + ")");
                result[1]++;
            }
        }
        return result;
    }

    /**
     * Opens a photo with the specified name using the default image viewer.
     *
     * @param name name of the photo to open
     * @return true if the photo exists and is opened, false if it is not found
     * @throws PhotoOpenException if the photo exists but cannot be opened
     */
    public boolean openPhoto(String name) throws PhotoOpenException {
        if (!photos.containsKey(name)){
            return false;
        }
        photos.get(name).open();
        return true;
    }

    /**
     * Opens the event's photo directory using the default file explorer.
     * Creates the directory if it does not already exist.
     *
     * @return true if the directory is opened successfully, false if an I/O error occurs
     */
    public boolean openPhotoDir() {
        try {
            Path p = Path.of(PHOTO_SAVE_ROOT + PHOTO_RELATIVE_PATH + tag);
            Files.createDirectories(p);
            Desktop.getDesktop().open(p.toFile());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to open photo directory: " + e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Copies photo registrations from another event.
     *
     * @param original event whose photos should be preserved
     */
    public void copyPhotosFrom(Event original) {
        this.photos = new HashMap<>(original.photos);
    }

}
