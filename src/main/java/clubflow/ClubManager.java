package clubflow;

import clubflow.exception.ClubNotFoundException;
import clubflow.exception.DuplicateClubException;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Manages separate save files for clubs while keeping
 * one shared Club instance for commands.
 */
public class ClubManager {
    private static final Path CLUBS_ROOT = Path.of("saves", "clubs");

    private static final Path DEFAULT_FILE = CLUBS_ROOT.resolve("default").resolve("club.cf");

    private static final Path LEGACY_FILE = Path.of("saves", "clubs.cf");

    private static final String SAVE_FILENAME = "club.cf";

    private final Club activeClub;
    private final SaveLoadManager saveLoadManager;
    private final Path clubsRoot;
    private final Path defaultFile;
    private final Path legacyFile;

    private String activeClubId = "default";

    public ClubManager(Club activeClub) {
        this(activeClub, CLUBS_ROOT, DEFAULT_FILE, LEGACY_FILE);
    }

    ClubManager(Club activeClub, Path defaultFile) {
        this(activeClub, defaultFile.getParent().resolve("clubs"),defaultFile, null);
    }

    ClubManager(Club activeClub, Path clubsRoot, Path defaultFile, Path legacyFile) {
        this.activeClub = Objects.requireNonNull(activeClub);
        this.saveLoadManager = new SaveLoadManager();
        this.clubsRoot = Objects.requireNonNull(clubsRoot);
        this.defaultFile = Objects.requireNonNull(defaultFile);
        this.legacyFile = legacyFile;
    }

    public String getActiveClubId() {
        return activeClubId;
    }

    public Path getActiveClubFile() {
        return getClubFile(activeClubId);
    }

    public Path getClubFile(String clubName) {
        String clubId = normalizeClubId(clubName);

        if (clubId.equals("default")) {
            return defaultFile;
        }

        return clubsRoot.resolve(clubId).resolve(SAVE_FILENAME);
    }

    /**
     * Loads the default club at startup.
     * Migrates the old save file if necessary.
     */
    public boolean initialize() throws IOException {
        boolean existed = Files.isRegularFile(defaultFile);

        if (!existed && legacyFile != null
            && Files.isRegularFile(legacyFile)) {
            migrateLegacySave();
            existed = true;
        }

        if (!existed) {
            if (Files.exists(defaultFile)) {
                throw new IOException("Default club save path is not a file: " + defaultFile);
            }

            Path parent = defaultFile.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.createFile(defaultFile);
        }

        Club loaded = saveLoadManager.load(defaultFile);
        activeClub.replaceWith(loaded);
        activeClub.markSaved();

        return existed;
    }

    /**
     * Copies previous single-club data into the default club.
     */
    private void migrateLegacySave() throws IOException {
        Path destination = defaultFile.getParent();
        Files.createDirectories(destination);

        Path oldPhotos =
                legacyFile.getParent().resolve("photos");

        Path newPhotos = destination.resolve("photos");

        if (Files.isDirectory(oldPhotos)) {
            try (Stream<Path> entries = Files.walk(oldPhotos)) {
                for (Path entry : entries.toList()) {
                    Path target = newPhotos.resolve(oldPhotos.relativize(entry));

                    if (Files.isDirectory(entry)) {
                        Files.createDirectories(target);
                    } else if (Files.isRegularFile(entry)) {
                        Files.copy(entry, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }

        Files.copy(legacyFile, defaultFile);
    }

    /**
     * Creates a new club with an empty save file.
     */
    public void createClub(String clubName) throws DuplicateClubException, IOException {
        String clubId = normalizeClubId(clubName);

        if (clubId.equals("default")) {
            throw new DuplicateClubException(clubName);
        }

        Files.createDirectories(clubsRoot);

        if (hasClubEntry(clubId)) {
            throw new DuplicateClubException(clubName);
        }

        Path clubDirectory = clubsRoot.resolve(clubId);

        try {
            Files.createDirectory(clubDirectory);
        } catch (FileAlreadyExistsException e) {
            throw new DuplicateClubException(clubName);
        }

        try {
            Files.createFile(clubDirectory.resolve(SAVE_FILENAME));
        } catch (IOException e) {
            try {
                Files.deleteIfExists(clubDirectory);
            } catch (IOException cleanupException) {
                e.addSuppressed(cleanupException);
            }
            throw e;
        }
    }

    /**
     * Lists all clubs with valid save files.
     */
    public List<String> listClubs() throws IOException {
        List<String> clubs = new ArrayList<>();

        if (Files.isRegularFile(defaultFile)) {
            clubs.add("default");
        }

        if (Files.isDirectory(clubsRoot)) {
            try (Stream<Path> paths = Files.list(clubsRoot)) {
                paths.filter(Files::isDirectory)
                    .filter(path -> Files.isRegularFile(path.resolve(SAVE_FILENAME)))
                    .map(path -> path.getFileName().toString())
                    .filter(id -> !id.equalsIgnoreCase("default"))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .forEach(clubs::add);
            }
        }

        return clubs;
    }

    /**
     * Saves the current club and loads another club.
     */
    public void switchClub(String clubName) throws ClubNotFoundException, IOException {

        String targetId = normalizeClubId(clubName);

        if (targetId.equals(activeClubId)) {
            return;
        }

        Path targetFile = getClubFile(targetId);

        if (!Files.isRegularFile(targetFile)) {
            throw new ClubNotFoundException(clubName);
        }

        // Save the current club before switching.
        saveActiveClub();
        activeClub.markSaved();

        // Load the target club.
        Club loaded = saveLoadManager.load(targetFile);

        // Keep the same Club object for existing commands.
        activeClub.replaceWith(loaded);
        activeClub.markSaved();

        // Update the active ID only after successful loading.
        activeClubId = targetId;
    }

    /**
     * Saves the active club to its own file.
     */
    public void saveActiveClub() throws IOException {
        saveLoadManager.save(activeClub, getActiveClubFile());
        activeClub.markSaved();
    }

    /**
     * Checks whether an entry with the given club ID already exists,
     * ignoring case.
     *
     * @param clubId Normalized club identifier.
     * @return True if a matching entry exists.
     * @throws IOException If the club directory cannot be read.
     */
    private boolean hasClubEntry(String clubId) throws IOException {
        try (Stream<Path> entries = Files.list(clubsRoot)) {
            return entries.anyMatch(path -> path.getFileName().toString().equalsIgnoreCase(clubId));
        }
    }

    /**
     * Converts club names to lowercase safe directory IDs.
     */
    private String normalizeClubId(String clubName) {
        Objects.requireNonNull(clubName);

        String clubId = clubName.trim()
                .toLowerCase(Locale.ROOT);

        if (!clubId.matches("[a-z0-9_-]+")) {
            throw new IllegalArgumentException("Club ID must contain only letters, " + "digits, _ or -.");
        }

        return clubId;
    }
}
