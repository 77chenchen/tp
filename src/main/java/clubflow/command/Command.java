package clubflow.command;

import clubflow.UserInterface;
import clubflow.exception.CommandParseException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Represents a command that can be executed.
 */
public abstract class Command {

    /** Separates repeated values stored by the parser under one argument ID. */
    static final String REPEATED_ARGUMENT_SEPARATOR = "\u001F";

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
     * Returns IDs that may be entered as flags without a slash or value.
     * Commands without flags can use this default empty result.
     *
     * @return valid flag argument IDs
     */
    public String[] flagArgIds() {
        return new String[]{};
    }

    /**
     * Returns IDs which users may specify more than once.
     * Commands without repeatable arguments can use this default empty result.
     *
     * @return repeatable argument IDs
     */
    public String[] repeatableArgIds() {
        return new String[]{};
    }

    /**
     * Returns all IDs of arguments that are valid.
     * Used for command parser validation.
     *
     * @return required, optional, and flag argument IDs
     */
    public String[] validArgIds(){
        String[] requiredArgIds = requiredArgIds();
        String[] optionalArgIds = optionalArgIds();
        String[] flagArgIds = flagArgIds();
        String[] result = new String[requiredArgIds.length + optionalArgIds.length + flagArgIds.length];

        System.arraycopy(requiredArgIds, 0, result, 0, requiredArgIds.length);
        System.arraycopy(optionalArgIds, 0, result, requiredArgIds.length, optionalArgIds.length);
        System.arraycopy(flagArgIds, 0, result, requiredArgIds.length + optionalArgIds.length,
                flagArgIds.length);
        return result;
    }

    /**
     * Validates combinations of arguments that are specific to this command.
     * Commands without additional validation rules can use this default implementation.
     *
     * @param args parsed command arguments
     * @throws CommandParseException if the combination of arguments is invalid
     */
    public void validateArgs(HashMap<String, String> args) throws CommandParseException {
        // No command-specific validation is required by default.
    }

    /**
     * Returns all values supplied for an argument, including repeated values.
     *
     * @param args parsed command arguments
     * @param argId ID of the argument to retrieve
     * @return values supplied for the argument, or an empty list if it was omitted
     */
    protected List<String> getArgumentValues(HashMap<String, String> args, String argId) {
        if (!args.containsKey(argId)) {
            return List.of();
        }
        return Arrays.asList(args.get(argId).split(REPEATED_ARGUMENT_SEPARATOR, -1));
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
