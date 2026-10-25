package clubflow;

import clubflow.command.AddPhotoCommand;
import clubflow.command.AssignMemberCommand;
import clubflow.command.CommandParser;
import clubflow.command.CompletionCommand;
import clubflow.command.CreateEventCommand;
import clubflow.command.CreateMemberCommand;
import clubflow.command.DeleteEventCommand;
import clubflow.command.DeleteMemberCommand;
import clubflow.command.DeletePhotoCommand;
import clubflow.command.EditEventCommand;
import clubflow.command.EditMemberCommand;
import clubflow.command.ExitCommand;
import clubflow.command.FindCommand;
import clubflow.command.FindMemberCommand;
import clubflow.command.HelpCommand;
import clubflow.command.InviteCommand;
import clubflow.command.ListPhotoCommand;
import clubflow.command.LoadCommand;
import clubflow.command.OpenPhotoCommand;
import clubflow.command.UnassignMemberCommand;
import clubflow.command.ViewEventCommand;
import clubflow.command.ViewMemberCommand;
import clubflow.exception.ClubFlowException;
import clubflow.completion.CompletionHandler;
import clubflow.completion.EventCompletion;
import clubflow.exception.CommandParseException;
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

        parser.register("help", new HelpCommand(ui, parser));

        parser.register("createEvent", new CreateEventCommand(ui, clubData));
        parser.registerAlias("createEvent", "ce");

        parser.register("editEvent", new EditEventCommand(ui, clubData));
        parser.registerAlias("editEvent", "e");

        parser.register("deleteEvent", new DeleteEventCommand(ui, clubData));
        parser.registerAlias("deleteEvent", "de");

        parser.register("viewEvent", new ViewEventCommand(ui, clubData));
        parser.registerAlias("viewEvent", "ve");

        EventSearch eventSearch = new EventSearch(ui, clubData);
        parser.register("find", new FindCommand(ui, Map.of("e", eventSearch::search)));
        EventCompletion eventCompletion = new EventCompletion(ui, clubData);
        Map<String, CompletionHandler> completionHandlers = Map.of("e", eventCompletion::setCompleted);
        parser.register("mark", new CompletionCommand(ui, completionHandlers, true));
        parser.register("unmark", new CompletionCommand(ui, completionHandlers, false));

        parser.register("createMember", new CreateMemberCommand(ui, clubData));
        parser.registerAlias("createMember", "cm");
        parser.registerAlias("createMember", "addMember");
        parser.register("viewMember", new ViewMemberCommand(ui, clubData));
        parser.registerAlias("viewMember", "vm");

        parser.register("deleteMember", new DeleteMemberCommand(ui, clubData));
        parser.registerAlias("deleteMember", "dm");

        //@@author 77chenchen
        parser.register("editMember", new EditMemberCommand(ui, clubData));
        parser.registerAlias("editMember", "em");
        parser.register("findMember", new FindMemberCommand(ui, clubData));
        parser.registerAlias("findMember", "fm");
        parser.register("assignMember", new AssignMemberCommand(ui, clubData));
        parser.registerAlias("assignMember", "am");
        parser.register("unassignMember", new UnassignMemberCommand(ui, clubData));
        parser.registerAlias("unassignMember", "um");
        //@@author

        parser.register("addPhoto", new AddPhotoCommand(ui, clubData));
        parser.registerAlias("addPhoto", "ap");

        parser.register("openPhoto", new OpenPhotoCommand(ui, clubData));
        parser.registerAlias("openPhoto", "op");

        parser.register("deletePhoto", new DeletePhotoCommand(ui, clubData));
        parser.registerAlias("deletePhoto", "dp");

        parser.register("listPhoto", new ListPhotoCommand(ui, clubData));
        parser.registerAlias("listPhoto", "lp");

        parser.register("load", new LoadCommand(ui, clubData));
        parser.register("exit", new ExitCommand(ui));
        parser.register("invite", new InviteCommand(ui, clubData));

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

                if (e instanceof CommandParseException){
                    CommandParseException cpe = (CommandParseException) e;
                    if (cpe.willPrintSyntax()){
                        cpe.printCorrectSyntax(parser);
                    }
                }
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
