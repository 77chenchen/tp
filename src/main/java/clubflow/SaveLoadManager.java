package clubflow;

import java.io.IOException;
import clubflow.member.Member;
import clubflow.event.Event;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Saves and loads club data as one member or event record per line in a UTF-8 text file.
 * Fields are separated by {@code |}, and a member's functions are separated by commas.
 */
public class SaveLoadManager {
    private static final Logger LOGGER = Logger.getLogger(SaveLoadManager.class.getName());

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

        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(path, lines, StandardCharsets.UTF_8);
        LOGGER.info(() -> "Saved " + lines.size() + " records to " + path);
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
            } else {
                throw new IOException("Unknown record type at line " + lineNumber);
            }
        }

        assert loaded.getMembers().size() + loaded.getEvents().size() == lines.size()
                : "Every persisted line must produce one record";
        LOGGER.info(() -> "Loaded " + lines.size() + " records from " + path);
        return loaded;
    }

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
}
