package clubflow;

import java.util.ArrayList;
import java.util.Arrays;

public abstract class Command {
    public abstract String[] requiredArgIds();
    public abstract String[] optionalArgIds();

    public String[] validArgIds(){
        String[] result = new String[requiredArgIds().length + optionalArgIds().length];
        System.arraycopy(requiredArgIds(), 0, result, 0, requiredArgIds().length);
        System.arraycopy(optionalArgIds(), 0, result, requiredArgIds().length, optionalArgIds().length);
        return result;
    }

    public abstract boolean execute(ArrayList<CommandArg> args);
}
