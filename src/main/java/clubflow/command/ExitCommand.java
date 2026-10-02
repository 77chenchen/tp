package clubflow.command;

import clubflow.UserInterface;

import java.util.HashMap;

public class ExitCommand extends Command{

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
