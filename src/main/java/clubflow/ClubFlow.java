package clubflow;

import clubflow.command.AddPhotoCommand;
import clubflow.command.CommandParser;
import clubflow.command.CompletionCommand;
import clubflow.command.CreateEventCommand;
import clubflow.command.CreateMemberCommand;
import clubflow.command.DeleteEventCommand;
import clubflow.command.DeleteMemberCommand;
import clubflow.command.DeletePhotoCommand;
import clubflow.command.EditEventCommand;
import clubflow.command.ExitCommand;
import clubflow.command.FindCommand;
import clubflow.command.HelpCommand;
import clubflow.command.ListPhotoCommand;
import clubflow.command.LoadCommand;
import clubflow.command.OpenPhotoCommand;
import clubflow.command.SaveCommand;
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
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Represents the ClubFlow application.
 */
public class ClubFlow {
    /** Package logger retained for the lifetime of the app so its file handler stays active. */
    private static final Logger PACKAGE_LOGGER = configureLogging();

    /** Records lifecycle events and unexpected failures for this class. */
    private static final Logger LOGGER = Logger.getLogger(ClubFlow.class.getName());

    /** Terminal interface shared by the parser and commands. */
    private final UserInterface ui;

    /** Parses user input and dispatches registered commands. */
    private final CommandParser parser;

    /** In-memory store shared by all commands that manage club data. */
    private final ClubData clubData;

    /**
     * Initializes the ClubFlow app.
     */
    public ClubFlow() {
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

        parser.register("addPhoto", new AddPhotoCommand(ui, clubData));
        parser.registerAlias("addPhoto", "ap");

        parser.register("openPhoto", new OpenPhotoCommand(ui, clubData));
        parser.registerAlias("openPhoto", "op");

        parser.register("deletePhoto", new DeletePhotoCommand(ui, clubData));
        parser.registerAlias("deletePhoto", "dp");

        parser.register("listPhoto", new ListPhotoCommand(ui, clubData));
        parser.registerAlias("listPhoto", "lp");

        parser.register("save", new SaveCommand(ui, clubData));
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
        LOGGER.info("ClubFlow started");
        while (true) {
            try {
                String input = ui.prompt();
                boolean terminate = parser.parse(input);
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
