package clubflow;

import clubflow.command.CommandParser;
import clubflow.command.ExitCommand;
import clubflow.command.TestCommand;
import clubflow.exception.CommandParseException;

public class ClubFlow {

    UserInterface ui;
    CommandParser parser;

    public ClubFlow(){
        ui = new UserInterface("[CF] ", "> ");
        parser = new CommandParser(ui);

        parser.register("test", new TestCommand(ui)); //TODO: REMOVE!!! FOR TESTING ONLY
        parser.register("exit", new ExitCommand(ui));
    }

    public void run(){
        ui.welcome();
        while (true){
            try{
                if (parser.parse(ui.prompt())){
                    break;
                }
            } catch (CommandParseException e) {
                ui.print(e.toString());
            }
        }
    }

    public static void main(String[] args){
        new ClubFlow().run();
    }
}
