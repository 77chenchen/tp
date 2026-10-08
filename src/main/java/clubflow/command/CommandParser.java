package clubflow.command;

import clubflow.exception.CommandParseException;
import clubflow.UserInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

/**
 * Represents a compiler that parses and executes user's command input in plaintext.
 * Stores all command that can be registered.
 */
public class CommandParser {

    private static final char QUOTE_CHAR = '\"';
    private static final char SPACE_CHAR = ' ';
    private static final char SLASH_CHAR = '/';

    private UserInterface ui;
    private HashMap<String, Command> commands;

    /**
     * Creates a command parser.
     * @param ui User interface to print command messages in.
     */
    public CommandParser(UserInterface ui){
        this.ui = ui;
        commands = new HashMap<String, Command>();
    }

    /**
     * Registers a command into the command parser.
     * @param keyword The keyword to set for the command.
     *                e.g "test a/1 b/1" has keyword "test".
     *                Command keywords are case-insensitive.
     * @param command The command to be registered.
     */
    public void register(String keyword, Command command){
        if (commands.containsKey(keyword)){
            ui.print("Command with keyword \"" + keyword + "\" already exists!");
        }
        commands.put(keyword.toLowerCase(), command);
    }

    /**
     * Parses a user text input into a command object and command arguments, and executing it.
     * @param input User's text input to parse.
     * @return True if the command would terminate the app, False if not.
     * @throws CommandParseException If text input cannot be parsed into a command object.
     */
    public boolean parse(String input) throws CommandParseException {
        ArrayList<String> sepList = separate(input);
        String commandKeyword = sepList.getFirst().toLowerCase();

        Command command = commands.get(commandKeyword);
        if (command == null){
            throw new CommandParseException("Command \"" + commandKeyword + "\" does not exist.");
        }

        HashMap<String, String> args = parseArgs(sepList, command);
        return command.execute(args);
    }

    /**
     * Separates an input line at spaces while preserving spaces inside double quotes.
     * For example, {@code test a/1 b/"I love CS2113"} becomes three tokens rather
     * than splitting {@code "I love CS2113"} into two tokens.
     *
     * @param input input line to separate
     * @return separated command and argument tokens
     */
    private ArrayList<String> separate(String input){
        return separate(null, input);
    }

    /**
     * Recursively separates the remaining input and accumulates its tokens.
     *
     * @param sepList tokens collected so far, or null when starting
     * @param s remaining input to separate
     * @return all separated tokens
     */
    private ArrayList<String> separate(ArrayList<String> sepList, String s){
        ArrayList<String> resultSepList = (sepList == null? new ArrayList<String>() : sepList);
        String sTrim = s.trim();

        int quoteIndex = sTrim.indexOf(QUOTE_CHAR);
        int spaceIndex = sTrim.indexOf(SPACE_CHAR);
        if (quoteIndex != -1 && quoteIndex < spaceIndex){
            int nextQuoteIndex = sTrim.indexOf(QUOTE_CHAR, quoteIndex + 1);
            if (nextQuoteIndex != -1) {
                spaceIndex = sTrim.indexOf(SPACE_CHAR, nextQuoteIndex);
            }
        }

        if (spaceIndex == -1){
            resultSepList.add(sTrim);
            return resultSepList;
        }

        resultSepList.add(sTrim.substring(0, spaceIndex));
        return separate(resultSepList, sTrim.substring(spaceIndex + 1));
    }

    /**
     * Parses and validates the arguments for a command.
     * Validation covers duplicate IDs, valid IDs, required IDs, flags, and any
     * additional rules supplied by {@link Command#validateArgs(HashMap)}.
     *
     * @param sepList separated input containing the command keyword followed by arguments
     * @param command command whose argument rules should be applied
     * @return arguments mapped from their IDs to their values
     * @throws CommandParseException if any argument is malformed or invalid
     */
    private HashMap<String, String> parseArgs(ArrayList<String> sepList, Command command) throws CommandParseException {
        ArrayList<String> strArgs = sepList;
        strArgs.removeFirst();

        HashMap<String, String> args = new HashMap<String, String>();
        Set<String> argIds = new HashSet<String>();

        Set<String> validArgIds = Set.of(command.validArgIds());
        Set<String> requiredArgIds = Set.of(command.requiredArgIds());
        Set<String> flagArgIds = Set.of(command.flagArgIds());
        Set<String> repeatableArgIds = Set.of(command.repeatableArgIds());

        for(String strArg : strArgs){
            String[] arg = parseArg(strArg, flagArgIds);
            String argId = arg[0];
            if (argIds.contains(argId)){
                if (repeatableArgIds.contains(argId)) {
                    String previousValues = args.get(argId);
                    args.put(argId, previousValues + Command.REPEATED_ARGUMENT_SEPARATOR + arg[1]);
                    continue;
                }
                throw new CommandParseException("Multiple arguments start with \"" + argId + "/\".");
            }
            args.put(argId, arg[1]);
            argIds.add(argId);
        }

        if (!validArgIds.containsAll(argIds)){
            Set<String> wrongArgIds = new HashSet<>(argIds);
            wrongArgIds.removeAll(validArgIds);
            throw new CommandParseException("Argument(s) starting with " + String.join(", ", wrongArgIds)
                    + " are invalid.");
        }

        if (!argIds.containsAll(requiredArgIds)){
            Set<String> missingArgId = new HashSet<>(requiredArgIds);
            missingArgId.removeAll(argIds);
            throw new CommandParseException("Argument(s) starting with " + String.join(", ", missingArgId)
                    + " are required but missing.");
        }

        command.validateArgs(args);
        return args;
    }

    /**
     * Parses an {@code ID/value} argument or a declared flag.
     * Flags may use either {@code flag} or {@code /flag} syntax and are represented
     * in the result with an empty value.
     *
     * @param strArg argument text to parse
     * @param flagArgIds IDs that are valid as flags for the current command
     * @return a two-element array containing the lowercase ID and its value
     * @throws CommandParseException if the argument or its quotation marks are malformed
     */
    private String[] parseArg(String strArg, Set<String> flagArgIds) throws CommandParseException {
        String lowercaseArg = strArg.toLowerCase();
        if (strArg.startsWith(String.valueOf(SLASH_CHAR))) {
            String flagId = lowercaseArg.substring(1);
            if (!flagId.isBlank() && flagId.indexOf(SLASH_CHAR) == -1 && flagArgIds.contains(flagId)) {
                return new String[]{flagId, ""};
            }
            throw new CommandParseException("Argument \"" + strArg + "\" is not a valid flag.");
        }

        if (strArg.indexOf(SLASH_CHAR) == -1) {
            if (flagArgIds.contains(lowercaseArg)) {
                return new String[]{lowercaseArg, ""};
            }
            throw new CommandParseException("Argument \"" + strArg + "\" has no \"" + SLASH_CHAR + "\".");
        }

        String[] separatedArg = strArg.split(String.valueOf(SLASH_CHAR), 2);
        String argId;
        String argVal;
        try{
            argId = separatedArg[0].toLowerCase();
            argVal = separatedArg[1];
        } catch (Exception e) {
            throw new CommandParseException("Argument \"" + strArg + "\" has no \"" + SLASH_CHAR +"\".");
        }

        if (flagArgIds.contains(argId)) {
            throw new CommandParseException("Flag \"" + argId + "\" must not contain \"" + SLASH_CHAR + "\".");
        }

        int startQuoteIndex = argVal.indexOf(QUOTE_CHAR);
        if (startQuoteIndex != -1){
            if (startQuoteIndex != 0){
                throw new CommandParseException("Argument " + strArg
                        + " has incorrect position of starting quotation mark ("
                        + QUOTE_CHAR + ").");
            }
            int endQuoteIndex = argVal.indexOf(QUOTE_CHAR, startQuoteIndex + 1);
            if (endQuoteIndex == -1){
                throw new CommandParseException("Argument " + strArg
                        + " does not have an ending quotations mark(" + QUOTE_CHAR + ").");
            }
            if (endQuoteIndex < argVal.length() - 1){
                throw new CommandParseException("Argument " + strArg
                        + " has incorrect position of ending quotation mark (" + QUOTE_CHAR + ").");
            }
        }
        return new String[] {argId, argVal.replace(String.valueOf(QUOTE_CHAR), "")};
    }
}
