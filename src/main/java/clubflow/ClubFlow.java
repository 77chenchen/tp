package clubflow;

import clubflow.command.CommandParser;
import clubflow.command.CompletionCommand;
import clubflow.command.CreateEventCommand;
import clubflow.command.CreateMemberCommand;
import clubflow.command.DeleteEventCommand;
import clubflow.command.DeleteMemberCommand;
import clubflow.command.EditEventCommand;
import clubflow.command.ExitCommand;
import clubflow.command.FindCommand;
import clubflow.command.LoadCommand;
import clubflow.command.ViewEventCommand;
import clubflow.command.ViewMemberCommand;
import clubflow.exception.ClubFlowException;
import clubflow.completion.CompletionHandler;
import clubflow.completion.EventCompletion;
import clubflow.search.EventSearch;

import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Represents the ClubFlow application.
 */
public class ClubFlow {
    /** Package logger retained for the lifetime of the app so its file handler stays active. */
    private static final Logger PACKAGE_LOGGER = configureLogging();

    /** Records lifecycle events and unexpected failures for this class. */
    private static final Logger LOGGER = Logger.getLogger(ClubFlow.class.getName());

    /** File used for automatic loading and saving. */
    private static final Path DEFAULT_SAVE_PATH = Path.of("saves", "clubs.cf");

    /** Terminal interface shared by the parser and commands. */
    private final UserInterface ui;

    /** Parses user input and dispatches registered commands. */
    private final CommandParser parser;

    /** In-memory store shared by all commands that manage club data. */
    private final ClubData clubData;

    /** Save file for this app instance; tests use temporary files. */
    private final Path savePath;

    /**
     * Initializes the ClubFlow app.
     */
    public ClubFlow() {
        this(DEFAULT_SAVE_PATH);
    }

    /**
     * Initializes the app with a chosen persistence file.
     *
     * @param savePath file used for automatic loading and saving
     */
    ClubFlow(Path savePath) {
        this.savePath = Objects.requireNonNull(savePath, "Save path must not be null");
        ui = new UserInterface("[CF] ", "> ");
        parser = new CommandParser(ui);
        clubData = new ClubData();

        //parser.register("test", new TestCommand(ui)); //TODO: REMOVE!!! FOR TESTING ONLY

        parser.register("createEvent", new CreateEventCommand(ui, clubData));
        parser.register("editEvent", new EditEventCommand(ui, clubData));
        parser.register("deleteEvent", new DeleteEventCommand(ui, clubData));
        parser.register("viewEvent", new ViewEventCommand(ui, clubData));
        EventSearch eventSearch = new EventSearch(ui, clubData);
        parser.register("find", new FindCommand(ui, Map.of("e", eventSearch::search)));
        EventCompletion eventCompletion = new EventCompletion(ui, clubData);
        Map<String, CompletionHandler> completionHandlers = Map.of("e", eventCompletion::setCompleted);
        parser.register("mark", new CompletionCommand(ui, completionHandlers, true));
        parser.register("unmark", new CompletionCommand(ui, completionHandlers, false));

        CreateMemberCommand createMemberCommand = new CreateMemberCommand(ui, clubData);
        parser.register("createMember", createMemberCommand);
        parser.register("addMember", createMemberCommand);
        parser.register("viewMember", new ViewMemberCommand(ui, clubData));
        parser.register("deleteMember", new DeleteMemberCommand(ui, clubData));

        parser.register("load", new LoadCommand(ui, clubData));
        parser.register("exit", new ExitCommand(ui));

        assert ui != null && parser != null && clubData != null
                : "Application components must be initialized";
        LOGGER.info("ClubFlow initialized");
    }

    /**
     * Routes application logs to a file instead of Java's default console handler.
     *
     * @return the configured package logger
     */
    private static Logger configureLogging() {
        Logger packageLogger = Logger.getLogger(ClubFlow.class.getPackageName());
        packageLogger.setUseParentHandlers(false);

        try {
            Handler fileHandler = new FileHandler("clubflow.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            packageLogger.addHandler(fileHandler);
        } catch (IOException | SecurityException e) {
            // Logging must not interfere with the command-line interface or prevent startup.
            packageLogger.setLevel(Level.OFF);
        }

        return packageLogger;
    }

    /**
     * Starts running the ClubFlow app.
     */
    public void run() {
        ui.welcome();

        if (Files.exists(savePath)) {
            try {
                ClubData loaded = new SaveLoadManager().load(savePath);
                clubData.replaceWith(loaded);
                clubData.markSaved();
                ui.print("Saved data loaded.");
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Could not load data file", e);
                ui.print("Could not load saved data: " + e.getMessage());
                return;
            }
        }

        LOGGER.info("ClubFlow started");
        while (true) {
            try {
                String input = ui.prompt();
                boolean terminate = parser.parse(input);
                if (clubData.hasUnsavedChanges()) {
                    try {
                        new SaveLoadManager().save(clubData, savePath);
                        clubData.markSaved();
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "Autosave failed", e);
                        ui.print("Changes could not be saved: " + e.getMessage());
                    }
                }
                if (terminate) {
                    LOGGER.info("ClubFlow exited normally");
                    break;
                }
            } catch (ClubFlowException e) {
                LOGGER.log(Level.FINE, "Could not complete command", e);
                ui.print(e.getMessage());
            } catch (NoSuchElementException e) {
                // End-of-input is normal when input is redirected from a file or pipe.
                LOGGER.info("Input stream closed; ClubFlow is stopping");
                ui.print(e.getMessage());
                break;
            } catch (RuntimeException e) {
                // Keep implementation details in the log while showing a safe message to the user.
                LOGGER.log(Level.SEVERE, "Unexpected error while processing a command", e);
                ui.print(e.getMessage());
            }
        }
    }

    /**
     * Main method.
     */
    public static void main(String[] args) {
        new ClubFlow().run();
    }
}
