package clubflow.command;

import clubflow.UserInterface;

import java.util.HashMap;

/**
 * Represents a command that can be executed.
 */
public abstract class Command {

    /**
     * User Interface to print command messages in.
     */
    protected UserInterface ui;

    /**
     * Creates a command.
     * @param ui User Interface to print command messages in.
     */
    public Command(UserInterface ui){
        this.ui = ui;
    }

    /**
     * Returns all IDs of arguments that must be inputted by the user for the command.
     * Used for command parser validation.
     * @return IDs of arguments that must be inputted by the user for the command.
     */
    public abstract String[] requiredArgIds();

    /**
     * Returns all IDs of arguments that is valid but not required for the command. (For optional arguments)
     * Used for command parser validation.
     * @return IDs of arguments that is valid but not required for the command.
     */
    public abstract String[] optionalArgIds();

    /**
     * Returns all IDs of arguments that is valid. Equals requiredArgIds + optionalArgIds.
     * Used for command parser validation.
     * @return IDs of arguments that is valid.
     */
    public String[] validArgIds(){
        String[] result = new String[requiredArgIds().length + optionalArgIds().length];
        System.arraycopy(requiredArgIds(), 0, result, 0, requiredArgIds().length);
        System.arraycopy(optionalArgIds(), 0, result, requiredArgIds().length, optionalArgIds().length);
        return result;
    }

    /**
     * Executes command with corresponding arguments.
     * @param args Arguments to be used to execute the command.
     *             For example: "addPhoneNumber n/"Tan Jun Jie" p/12345678"
     *             would have arguments represented as a hashmap as:
     *                  [(n, Tan Jun Jie),
     *                  (p, 12345678)]
     * @return True if the program terminates after the command executes.
     */
    public abstract boolean execute(HashMap<String, String> args);
}
