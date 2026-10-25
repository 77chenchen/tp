package clubflow.command;

import java.io.IOException;
import java.util.HashMap;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import clubflow.ClubData;
import clubflow.UserInterface;
import clubflow.SaveLoadManager;
import clubflow.exception.CommandParseException;

/**
 * Saves the current club data to a user-supplied relative path or the default save file.
 */
public class SaveCommand extends Command {
    private static final Logger LOGGER = Logger.getLogger(SaveCommand.class.getName());

    /** Club data shared with the other commands. */
    private final ClubData clubData;

    /**
     * Creates a save command for the shared club data.
     *
     * @param ui interface used to display the result
     * @param clubData current club data to save
     */
    public SaveCommand(UserInterface ui, ClubData clubData) {
        super(ui);
        this.clubData = Objects.requireNonNull(clubData, "Club data must not be null");
        addSyntax("[p/Path]");
    }

    @Override 
    public String[] requiredArgIds() {
        return new String[]{};
    }

    @Override 
    public String[] optionalArgIds() {
        return new String[]{"p"};
    }

    /**
     * Checks that an optional {@code p/} argument names a valid relative path.
     *
     * @param args parsed command arguments
     * @throws CommandParseException if the supplied path is blank, invalid, or absolute
     */
    @Override 
    public void validateArgs(HashMap<String, String> args) throws CommandParseException {
        if (!args.containsKey("p")) {
            return;
        }

        String pathString = args.get("p");
        if (pathString.isBlank()) {
            throw new CommandParseException("Provide a path after p/.");
        }

        try {
            if (Path.of(pathString).isAbsolute()) {
                throw new CommandParseException("The save path must be relative");
            }
        } catch (InvalidPathException e) {
            throw new CommandParseException("Invalid save path: " + e.getMessage());
        }
    }

    /**
     * Writes the club data and reports whether saving succeeded.
     *
     * @param args parsed arguments, optionally containing {@code p/}
     * @return {@code false} because saving does not exit the application
     */
    @Override 
    public boolean execute(HashMap<String, String> args) {
        Objects.requireNonNull(args, "Arguments must not be null");
        String pathString = args.getOrDefault("p", "saves/clubs.cf");
        Path path = Path.of(pathString);

        try {
            new SaveLoadManager().save(clubData, path);
            ui.print("Data saved successfully!");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not save club data to " + path, e);
            ui.print("Data not saved! " + e.getMessage());
        }

        return false;
    }
}
