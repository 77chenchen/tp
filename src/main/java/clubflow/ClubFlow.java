package clubflow;

import clubflow.command.CommandParser;
import clubflow.command.CreateMemberCommand;
import clubflow.command.DeleteMemberCommand;
import clubflow.command.CreateEventCommand;
import clubflow.command.DeleteEventCommand;
import clubflow.command.ExitCommand;
import clubflow.command.TestCommand;
import clubflow.command.ViewEventCommand;
import clubflow.command.ViewMemberCommand;
import clubflow.exception.CommandParseException;

/**
 * Represents the ClubFlow application.
 */
public class ClubFlow {

    /** Terminal interface shared by the parser and commands. */
    private UserInterface ui;

    /** Parses user input and dispatches registered commands. */
    private CommandParser parser;

    /** In-memory store shared by all commands that manage club data. */
    private ClubData clubData;

    /**
     * Initializes the ClubFlow app.
     */
    public ClubFlow(){
        ui = new UserInterface("[CF] ", "> ");
        parser = new CommandParser(ui);
        clubData = new ClubData();

        parser.register("test", new TestCommand(ui)); //TODO: REMOVE!!! FOR TESTING ONLY

        parser.register("createEvent", new CreateEventCommand(ui, clubData));
        parser.register("deleteEvent", new DeleteEventCommand(ui, clubData));
        parser.register("viewEvent", new ViewEventCommand(ui, clubData));

        CreateMemberCommand createMemberCommand = new CreateMemberCommand(ui, clubData);
        parser.register("createMember", createMemberCommand);
        parser.register("addMember", createMemberCommand);
        parser.register("viewMember", new ViewMemberCommand(ui, clubData));
        parser.register("deleteMember", new DeleteMemberCommand(ui, clubData));

        parser.register("exit", new ExitCommand(ui));
    }

    /**
     * Starts running the ClubFlow app.
     */
    public void run(){
        ui.welcome();
        while (true){
            try{
                String input = ui.prompt();
                boolean terminate = parser.parse(input);
                if (terminate){
                    break;
                }
            } catch (CommandParseException e) {
                ui.print(e.getMessage());
            }
        }
    }

    /**
     * Main method.
     */
    public static void main(String[] args){
        new ClubFlow().run();
    }
}
