package clubflow;

import clubflow.command.CommandParser;
import clubflow.command.ExitCommand;
import clubflow.command.TestCommand;
import clubflow.exception.CommandParseException;

/**
 * Represents the ClubFlow application.
 */
public class ClubFlow {

    private UserInterface ui;
    private CommandParser parser;

    /**
     * Initializes the ClubFlow app.
     */
    public ClubFlow(){
        ui = new UserInterface("[CF] ", "> ");
        parser = new CommandParser(ui);

        parser.register("test", new TestCommand(ui)); //TODO: REMOVE!!! FOR TESTING ONLY
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
