package clubflow.command;

import clubflow.UserInterface;
import clubflow.exception.ClubFlowException;
import clubflow.exception.CommandParseException;

import java.util.HashMap;

public class HelpCommand extends Command{

    private CommandParser parser;

    /**
     * Creates a help command.
     *
     * @param ui User Interface to print command messages in.
     * @param parser Command Parser needed
     */
    public HelpCommand(UserInterface ui, CommandParser parser) {
        super(ui);
        this.parser = parser;
        addSyntax("");
        addSyntax("c/Cmd_Keyword");
    }

    @Override
    public String[] requiredArgIds() {
        return new String[0];
    }

    @Override
    public String[] optionalArgIds() {
        return new String[] {"c"};
    }

    @Override
    public void validateArgs(HashMap<String, String> args) throws CommandParseException {
        if (args.containsKey("c") && !parser.commandExist(args.get("c"))){
            throw new CommandParseException("Command " + args.get("c") + " does not exist.");
        }
    }

    @Override
    public boolean execute(HashMap<String, String> args) throws ClubFlowException {
        if (!args.containsKey("c")){
            ui.print("Commands and their syntaxes are as following:");
            parser.printAllCommandSyntax();
            return false;
        }
        parser.printCommandSyntax(args.get("c"));
        return false;
    }
}
