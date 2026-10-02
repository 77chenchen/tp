package clubflow.command;

import clubflow.UserInterface;

import java.util.HashMap;

/**
 * Represents command that exits the program.
 */
public class ExitCommand extends Command{

    /**
     * Creates exit command
     * @param ui User interface to print messages in.
     */
    public ExitCommand(UserInterface ui) {
        super(ui);
    }

    @Override
    public String[] requiredArgIds() {
        return new String[]{};
    }

    @Override
    public String[] optionalArgIds() {
        return new String[]{};
    }

    @Override
    public boolean execute(HashMap<String, String> args) {
        ui.print("Goodbye!");
        return true;
    }
}
