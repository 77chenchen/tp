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

    /*
     * Basically s.split(" "), but it ignores the spaces in between quotation marks.
     * For example, s = "test a/123 b/"I love CS2113" c/test"
     * 1. s.split(" ") returns [test, a/123, b/"I, love, CS2113", c/test]
     * 2. separate(s) returns [test, a/123, b/"I love CS2113", c/test]
     * The 2nd one is what we want.
     */
    private ArrayList<String> separate(String s){
        return separate(null, s);
    }

    //I think regex can do the task of this, but now I could only do recursive programming for this task
    private ArrayList<String> separate(ArrayList<String> sepList, String s){
        ArrayList<String> resultSepList = (sepList == null? new ArrayList<String>() : sepList);
        String sTrim = s.trim();

        int quoteIndex = sTrim.indexOf(QUOTE_CHAR);
        int spaceIndex = sTrim.indexOf(SPACE_CHAR);
        if (quoteIndex != -1 && quoteIndex < spaceIndex){
            int nextQuoteIndex = sTrim.indexOf(QUOTE_CHAR, spaceIndex);
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

    /*
     * Parses arguments into hashmap.
     * Also verifies if whether the command has all the required and no invalid parameters.
     * For example, command "test a/123 b/"I love CS2113" c/test" would return a hash map of:
     * ("a", "123"),
     * ("b", "I love CS2113")
     * ("c", test")
     * This allows the arguments to be easily read.
     */
    private HashMap<String, String> parseArgs(ArrayList<String> sepList, Command command) throws CommandParseException {
        ArrayList<String> strArgs = sepList;
        strArgs.removeFirst();

        HashMap<String, String> args = new HashMap<String, String>();
        Set<String> argIds = new HashSet<String>();

        Set<String> validArgIds = Set.of(command.validArgIds());
        Set<String> requiredArgIds = Set.of(command.requiredArgIds());

        for(String strArg : strArgs){
            String[] arg = parseArg(strArg);
            String argId = arg[0];
            if (argIds.contains(argId)){
                throw new CommandParseException("Multiple arguments starts with \"" + argId + "/\".");
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

        return args;
    }

    /*
     * Gets the ID and the value of an argument from its text form.
     * Each command is in the format ARGUMENT_ID/ARGUMENT_VALUE
     * E.g. The argument "p/Testing":
     *      ARGUMENT_ID is "p", and ARGUMENT_VALUE is "testing".
     * Also validates if a particular argument is written wrongly.
     */
    private String[] parseArg(String strArg) throws CommandParseException {
        String[] separatedArg = strArg.split(String.valueOf(SLASH_CHAR), 2);
        String argId;
        String argVal;
        try{
            argId = separatedArg[0].toLowerCase();
            argVal = separatedArg[1];
        } catch (Exception e) {
            throw new CommandParseException("Argument \"" + strArg + "\" has no \"" + SLASH_CHAR +"\".");
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

