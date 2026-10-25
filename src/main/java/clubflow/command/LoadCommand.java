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
 * Loads club data from a user-supplied relative path into the shared data store.
 */
public class LoadCommand extends Command {
    private static final Logger LOGGER = Logger.getLogger(LoadCommand.class.getName());

    /** Club data shared with the other commands. */
    private final ClubData clubData;

    /**
     * Creates a load command for the shared club data.
     *
     * @param ui interface used to display the result
     * @param clubData club data to replace after a successful load
     */
    public LoadCommand(UserInterface ui, ClubData clubData) {
        super(ui);
        this.clubData = Objects.requireNonNull(clubData, "Club data must not be null");
        addSyntax("p/Path");
    }

    @Override 
    public String[] requiredArgIds() {
        return new String[]{"p"};
    }

    @Override 
    public String[] optionalArgIds() {
        return new String[]{};
    }

    /**
     * Checks that the required {@code p/} argument names a valid relative path.
     *
     * @param args parsed command arguments
     * @throws CommandParseException if the path is blank, invalid, or absolute
     */
    @Override 
    public void validateArgs(HashMap<String, String> args) throws CommandParseException {
        String pathString = args.get("p");
        if (pathString.isBlank()) {
            throw new CommandParseException("Provide a path after p/.");
        }

        try {
            if (Path.of(pathString).isAbsolute()) {
                throw new CommandParseException("The load path must be relative");
            }
        } catch (InvalidPathException e) {
            throw new CommandParseException("Invalid load path: " + e.getMessage());
        }
    }

    /**
     * Replaces the shared club data only after the file has loaded successfully.
     *
     * @param args parsed arguments containing the {@code p/} path
     * @return {@code false} because loading does not exit the application
     */
    @Override 
    public boolean execute(HashMap<String, String> args) {
        Objects.requireNonNull(args, "Arguments must not be null");
        assert args.containsKey("p") : "Validated arguments must contain a load path";
        String pathString = args.get("p");
        Path path = Path.of(pathString);

        try {
            ClubData loaded = new SaveLoadManager().load(path);
            clubData.replaceWith(loaded);
            ui.print("Data loaded successfully!");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not load club data from " + path, e);
            ui.print("Data not loaded! " + e.getMessage());
        }

        return false;
    }
}
