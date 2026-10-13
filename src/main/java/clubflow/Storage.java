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

/**
 * Saves and loads club data as one member or event record per line in a UTF-8 text file.
 * Fields are separated by {@code |}, and a member's functions are separated by commas.
 */
public class Storage {
    /** Creates a storage handler. */
    public Storage() {
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
            // Event | Tag | Name | Date | Deadline | Budget | Venue | Logistics
            String eventLine = String.join("|",
                    "EVENT", event.getTag(), event.getName(),
                    String.valueOf(event.getDate()), String.valueOf(event.getDeadline()),
                    String.valueOf(event.getBudget()), event.getVenue(), event.getLogistics());
            lines.add(eventLine);
        }

        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(path, lines, StandardCharsets.UTF_8);
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
        ClubData loaded = new ClubData();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

        for (String line : lines) {
            String[] fields = line.split("\\|", -1);

            if (fields[0].equals("MEMBER")) {
                if (fields.length != 5) {
                    throw new IOException("Invalid member record: " + line);
                }

                List<String> functions = fields[4].isEmpty() ? List.of() : List.of(fields[4].split(",", -1));
                if (functions.stream().anyMatch(String::isBlank)) {
                    throw new IOException("Invalid member record: " + line);
                }
                if (fields[1].isBlank() || (fields[2].isBlank() && fields[3].isBlank())) {
                    throw new IOException("Invalid member record: " + line);
                }
                Member member = new Member(fields[1], fields[2], fields[3], functions);
                loaded.addMember(member);
            } else if (fields[0].equals("EVENT")) {
                if (fields.length != 8) {
                    throw new IOException("Invalid event record: " + line);
                }
                if (fields[1].isBlank()) {
                    throw new IOException("Invalid event record: " + line);
                }
                try {
                    LocalDate date = fields[3].equals("null") ? null : LocalDate.parse(fields[3]);
                    LocalDate deadline = fields[4].equals("null") ? null : LocalDate.parse(fields[4]);
                    double budget = Double.parseDouble(fields[5]);

                    if (!Double.isFinite(budget) || budget < 0) {
                        throw new IOException("Invalid event record: " + line);
                    }

                    Event event = new Event(fields[1], fields[2], date, deadline, budget, fields[6], fields[7]);
                    if (!loaded.addEvent(event)) {
                        throw new IOException("Duplicate event tag: " + fields[1]);
                    }
                } catch (DateTimeParseException | NumberFormatException e) {
                    throw new IOException("Invalid event record: " + line, e);
                }
            } else {
                throw new IOException("Unknown record type: " + fields[0]);
            }
        }

        return loaded;
    }
}
