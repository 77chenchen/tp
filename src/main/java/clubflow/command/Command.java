package clubflow.command;

import clubflow.UserInterface;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class Command {

    protected UserInterface ui;

    public Command(UserInterface ui){
        this.ui = ui;
    }

    public abstract String[] requiredArgIds();
    public abstract String[] optionalArgIds();

    public String[] validArgIds(){
        String[] result = new String[requiredArgIds().length + optionalArgIds().length];
        System.arraycopy(requiredArgIds(), 0, result, 0, requiredArgIds().length);
        System.arraycopy(optionalArgIds(), 0, result, requiredArgIds().length, optionalArgIds().length);
        return result;
    }

    public abstract boolean execute(HashMap<String, String> args);
}
