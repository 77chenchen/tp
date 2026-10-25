package clubflow;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import clubflow.event.Event;
import clubflow.event.EventAssignment;
import clubflow.exception.PhotoLoadException;
import clubflow.member.Member;

/**
 * Saves and loads club data as member, event, and assignment records in a UTF-8 text file.
 * Fields are separated by {@code |}, and a member's functions are separated by commas.
 */
public class SaveLoadManager {
    private static final Logger LOGGER = Logger.getLogger(SaveLoadManager.class.getName());

    //@@author 77chenchen
    /** Holds an assignment record until all members and events have been read. */
    private record PendingAssignment(String[] fields, int lineNumber) {
    }
    //@@author

    /** Creates a storage handler. */
    public SaveLoadManager() {
    }

    /**
     * Writes all members and events to a file, replacing its previous contents.
     * Creates the parent directory when it does not already exist.
     *
     * @param data club data to save
     * @param path destination file
     * @throws IOException if the directory or file cannot be written
     */
    public void save(ClubData data, Path path) throws IOException {
        Objects.requireNonNull(data, "Club data must not be null");
        Objects.requireNonNull(path, "Save path must not be null");
        List<String> lines = new ArrayList<>();

        for (Member member : data.getMembers()) {
            // formatted member line
            // Member | Name | Tele | Email | Function
            String memberLine = String.join("|",
                    "MEMBER", member.getName(), member.getTelegramHandle(), member.getEmail(),
                    String.join(",", member.getFunctions()));
            lines.add(memberLine);
        }
        for (Event event : data.getEvents()) {
            // formatted event line
            // Event | Tag | Name | Date | Deadline | Budget | Venue | Logistics | Completed
            String eventLine = String.join("|",
                    "EVENT", event.getTag(), event.getName(),
                    String.valueOf(event.getDate()), String.valueOf(event.getDeadline()),
                    String.valueOf(event.getBudget()), event.getVenue(), event.getLogistics(),
                    String.valueOf(event.isCompleted()));
            lines.add(eventLine);
        }

        //@@author 77chenchen
        List<Member> members = data.getMembers();
        List<Event> events = data.getEvents();
        for (EventAssignment assignment : data.getAssignments()) {
            int memberIndex = indexOfMember(members, assignment.getMember());
            boolean eventExists = events.stream().anyMatch(event -> event == assignment.getEvent());
            if (memberIndex < 0 || !eventExists) {
                throw new IOException("Cannot save an assignment with a missing member or event");
            }
            lines.add(String.join("|", "ASSIGNMENT", assignment.getEvent().getTag(),
                    String.valueOf(memberIndex + 1), assignment.getRole()));
        }
        //@@author

        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(path, lines, StandardCharsets.UTF_8);
        LOGGER.info(() -> "Saved " + lines.size() + " records to " + path);

        savePhotos(path.toString());
    }

    /**
     * Reads and validates every record before returning the loaded club data.
     * An invalid file does not change the application's current data.
     *
     * @param path file to read
     * @return a new club data object containing the saved members and events
     * @throws IOException if the file cannot be read or contains an invalid record
     */
    public ClubData load(Path path) throws IOException {
        Objects.requireNonNull(path, "Load path must not be null");
        ClubData loaded = new ClubData();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        //@@author 77chenchen
        List<PendingAssignment> pendingAssignments = new ArrayList<>();
        //@@author

        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            String line = lines.get(lineIndex);
            int lineNumber = lineIndex + 1;
            String[] fields = line.split("\\|", -1);

            if (fields[0].equals("MEMBER")) {
                if (fields.length != 5) {
                    throw invalidRecord("member", lineNumber, null);
                }

                List<String> functions = fields[4].isEmpty() ? List.of() : List.of(fields[4].split(",", -1));
                if (functions.stream().anyMatch(String::isBlank)) {
                    throw invalidRecord("member", lineNumber, null);
                }
                if (fields[1].isBlank() || (fields[2].isBlank() && fields[3].isBlank())) {
                    throw invalidRecord("member", lineNumber, null);
                }
                try {
                    Member member = new Member(fields[1], fields[2], fields[3], functions);
                    loaded.addMember(member);
                } catch (IllegalArgumentException | NullPointerException e) {
                    throw invalidRecord("member", lineNumber, e);
                }
            } else if (fields[0].equals("EVENT")) {
                // Older files have eight fields and implicitly describe events that are not completed.
                if (fields.length != 8 && fields.length != 9) {
                    throw invalidRecord("event", lineNumber, null);
                }
                if (fields[1].isBlank()) {
                    throw invalidRecord("event", lineNumber, null);
                }
                try {
                    LocalDate date = fields[3].equals("null") ? null : LocalDate.parse(fields[3]);
                    LocalDate deadline = fields[4].equals("null") ? null : LocalDate.parse(fields[4]);
                    double budget = Double.parseDouble(fields[5]);

                    if (!Double.isFinite(budget) || budget < 0) {
                        throw invalidRecord("event", lineNumber, null);
                    }

                    boolean completed = false;
                    if (fields.length == 9) {
                        if (!fields[8].equals("true") && !fields[8].equals("false")) {
                            throw invalidRecord("event", lineNumber, null);
                        }
                        completed = Boolean.parseBoolean(fields[8]);
                    }
                    Event event = new Event(fields[1], fields[2], date, deadline, budget,
                            fields[6], fields[7], completed);
                    if (!loaded.addEvent(event)) {
                        throw new IOException("Duplicate event tag at line " + lineNumber);
                    }
                } catch (DateTimeParseException | IllegalArgumentException e) {
                    throw invalidRecord("event", lineNumber, e);
                }
            //@@author 77chenchen
            } else if (fields[0].equals("ASSIGNMENT")) {
                if (fields.length != 4) {
                    throw invalidRecord("assignment", lineNumber, null);
                }
                pendingAssignments.add(new PendingAssignment(fields, lineNumber));
            //@@author
            } else {
                throw new IOException("Unknown record type at line " + lineNumber);
            }
        }

        //@@author 77chenchen
        for (PendingAssignment pending : pendingAssignments) {
            String[] fields = pending.fields();
            try {
                int memberIndex = Integer.parseInt(fields[2]);
                Member member = loaded.findMemberByIndex(memberIndex)
                        .orElseThrow(() -> new IllegalArgumentException("Unknown member index"));
                if (!loaded.assignMemberToEvent(fields[1], member, fields[3])) {
                    throw invalidRecord("assignment", pending.lineNumber(), null);
                }
            } catch (IllegalArgumentException | NullPointerException e) {
                throw invalidRecord("assignment", pending.lineNumber(), e);
            }
        }

        assert loaded.getMembers().size() + loaded.getEvents().size()
                + loaded.getAssignments().size() == lines.size()
                : "Every persisted line must produce one record";
        //@@author
        LOGGER.info(() -> "Loaded " + lines.size() + " records from " + path);

        loadPhotos(path.toString(), loaded);
        return loaded;
    }

    //@@author 77chenchen
    /** Finds a member's zero-based list index by object identity for save serialization. */
    private int indexOfMember(List<Member> members, Member target) {
        for (int index = 0; index < members.size(); index++) {
            if (members.get(index) == target) {
                return index;
            }
        }
        return -1;
    }
    //@@author

    /**
     * Creates a consistent, line-numbered exception without exposing record contents.
     *
     * @param recordType type of malformed record
     * @param lineNumber one-based line number in the save file
     * @param cause underlying parsing failure, or null when none exists
     * @return exception describing the malformed record
     */
    private IOException invalidRecord(String recordType, int lineNumber, Exception cause) {
        String message = "Invalid " + recordType + " record at line " + lineNumber;
        return cause == null ? new IOException(message) : new IOException(message, cause);
    }

    //@@author czfray
    private void loadPhotos(String saveFilePath, ClubData clubData) {

        Path clubsPath = Path.of(saveFilePath).toAbsolutePath().normalize();
        Path photosPath = clubsPath.getParent().resolve(Event.PHOTO_RELATIVE_PATH);

        Path cachePhotosPath = Path.of(Event.PHOTO_SAVE_ROOT)
                .resolve(Event.PHOTO_RELATIVE_PATH)
                .toAbsolutePath()
                .normalize();

        if (!Files.isDirectory(photosPath)) {
            return;
        }

        boolean savingFromSaveRoot = photosPath.equals(cachePhotosPath);

        try {
            if (!savingFromSaveRoot) {
                deleteDirectory(cachePhotosPath.toString());
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to clear photo cache: " + cachePhotosPath, e);
            return;
        }

        for (Event e : clubData.getEvents()){
            File folder = photosPath.resolve(e.getTag()).toFile();
            File[] files = folder.listFiles();

            if (files == null) {
                LOGGER.log(Level.INFO, "Cannot access event photo directory: " + folder.getAbsolutePath());
                continue;
            }

            for (File file : files) {
                if (file.isFile() && file.getName().toLowerCase().endsWith(".png")) {
                    String name = file.getName();
                    name = name.substring(0, name.length() - 4);
                    try {
                        if (savingFromSaveRoot) {
                            e.loadPhoto(name);
                        } else {
                            e.addPhoto(file.getPath(), name);
                        }
                    } catch (PhotoLoadException ex) {
                        LOGGER.log(Level.WARNING, "Failed to load photo: " + file.getPath(), ex);
                    }
                }
            }
        }
    }

    private void savePhotos(String saveFilePath) throws IOException {
        String photoCopyPath = Path.of(saveFilePath).toAbsolutePath().normalize().getParent().toString();
        copyDirectory(photoCopyPath, Event.PHOTO_SAVE_ROOT + Event.PHOTO_RELATIVE_PATH);
    }

    private static void deleteDirectory(String path) throws IOException {
        Path pathObj = Path.of(path);
        if (Files.isDirectory(pathObj)) {
            try (var files = Files.walk(pathObj)) {
                for (Path p : files.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(p);
                }
            }
        }
    }

    private static void copyDirectory(String destPath, String sourcePath) throws IOException {
        Path source = Path.of(sourcePath);
        Path destination = Path.of(destPath).resolve(source.getFileName());

        if (!Files.isDirectory(source)) {
            return;
        }

        try (var paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path target = destination.resolve(source.relativize(path));

                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
