package clubflow.command;

import clubflow.UserInterface;

import java.util.HashMap;

/**
 * Test command to show how the command creation system work.
 */
public class TestCommand extends Command{

    public TestCommand(UserInterface ui) {
        super(ui);
    }

    @Override
    public String[] requiredArgIds() {
        return new String[]{"a", "b", "c"};
    }

    @Override
    public String[] optionalArgIds() {
        return new String[]{"d", "e"};
    }

    @Override
    public boolean execute(HashMap<String, String> args) {
        for(String arg : args.keySet()){
            ui.print(arg + ": " + args.get(arg));
        }
        return false;
    }
}
