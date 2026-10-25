package clubflow.command;

import clubflow.exception.CommandParseException;
import clubflow.exception.ClubFlowException;
import clubflow.UserInterface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Represents a compiler that parses and executes user's command input in plaintext.
 * Stores all command that can be registered.
 */
public class CommandParser {
    private static final Logger LOGGER = Logger.getLogger(CommandParser.class.getName());

    private static final char QUOTE_CHAR = '\"';
    private static final char SPACE_CHAR = ' ';
    private static final char SLASH_CHAR = '/';

    private final UserInterface ui;
    private final HashMap<String, Command> commands;

    private final HashMap<String, String> aliases;
    private final HashMap<String, List<String>> aliasMap;

    /**
     * Creates a command parser.
     * @param ui User interface to print command messages in.
     */
    public CommandParser(UserInterface ui) {
        this.ui = Objects.requireNonNull(ui, "User interface must not be null");
        commands = new HashMap<>();

        aliases = new HashMap<>();
        aliasMap = new HashMap<>();
    }

    /**
     * Registers a command into the command parser.
     * @param keyword The keyword to set for the command.
     *                e.g "test a/1 b/1" has keyword "test".
     *                Command keywords are case-insensitive.
     * @param command The command to be registered.
     */
    public void register(String keyword, Command command) {
        Objects.requireNonNull(keyword, "Command keyword must not be null");
        Objects.requireNonNull(command, "Command must not be null");
        if (keyword.isBlank() || keyword.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("Command keyword must be a non-blank word");
        }
        validateCommandDefinition(command);

        String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
        if (commands.putIfAbsent(normalizedKeyword, command) != null) {
            throw new IllegalArgumentException("Command with keyword \"" + keyword + "\" already exists!");
        }

        assert commands.get(normalizedKeyword) == command : "Registered command must be retrievable";
        LOGGER.fine(() -> "Registered command: " + normalizedKeyword);
    }

    /**
     * Registers an alias for an existing command.
     *
     * @param fullKeyword The keyword of the registered command.
     * @param alias The alias to register for the command.
     * @throws IllegalArgumentException if the command keyword is not registered.
     */
    public void registerAlias(String fullKeyword, String alias){
        if (!commands.containsKey(fullKeyword.toLowerCase())) {
            throw new IllegalArgumentException("Command keyword is not registered in parser.");
        }

        if (!aliasMap.containsKey(fullKeyword.toLowerCase())){
            aliasMap.put(fullKeyword.toLowerCase(), new ArrayList<>());
        }

        register(alias.toLowerCase(), commands.get(fullKeyword.toLowerCase()));
        aliases.put(alias.toLowerCase(), fullKeyword.toLowerCase());
        aliasMap.get(fullKeyword.toLowerCase()).add(alias.toLowerCase());
        LOGGER.fine(() -> "Registered alias on command " + fullKeyword + ": " + alias);
    }

    /**
     * Parses a user text input into a command object and command arguments, and executing it.
     * @param input User's text input to parse.
     * @return True if the command would terminate the app, False if not.
     * @throws ClubFlowException if input is invalid or the selected command cannot complete the request
     */
    public boolean parse(String input) throws ClubFlowException {
        if (input == null) {
            throw new CommandParseException("Command must not be null.");
        }
        if (input.isBlank()) {
            throw new CommandParseException("Command must not be blank.");
        }

        ArrayList<String> sepList = separate(input);
        String commandKeyword = sepList.getFirst().toLowerCase(Locale.ROOT);

        Command command = commands.get(commandKeyword);
        if (command == null){
            throw new CommandParseException("Command \"" + commandKeyword + "\" does not exist. " +
                    "Type \"help\" to see all commands available.");
        }

        LOGGER.fine(() -> "Executing command: " + commandKeyword);

        boolean terminate;
        try{
            HashMap<String, String> args = parseArgs(sepList, command);
            terminate = command.execute(args);
        } catch (CommandParseException e){
            e.setCommandKeyword(commandKeyword);
            throw e;
        }

        return terminate;
    }

    /**
     * Verifies that a command declares a consistent set of argument IDs.
     * Invalid declarations indicate a programming error, rather than bad user input.
     *
     * @param command command whose metadata should be checked
     * @throws IllegalArgumentException if argument IDs are blank, duplicated, or inconsistent
     */
    private void validateCommandDefinition(Command command) {
        String[] requiredIds = Objects.requireNonNull(command.requiredArgIds(),
                "Required argument IDs must not be null");
        String[] optionalIds = Objects.requireNonNull(command.optionalArgIds(),
                "Optional argument IDs must not be null");
        String[] flagIds = Objects.requireNonNull(command.flagArgIds(),
                "Flag argument IDs must not be null");
        String[] repeatableIds = Objects.requireNonNull(command.repeatableArgIds(),
                "Repeatable argument IDs must not be null");

        Set<String> allIds = new HashSet<>();
        addUniqueIds(allIds, requiredIds);
        addUniqueIds(allIds, optionalIds);
        addUniqueIds(allIds, flagIds);

        Set<String> repeatableIdSet = new HashSet<>(Arrays.asList(repeatableIds));
        if (repeatableIdSet.size() != repeatableIds.length || !allIds.containsAll(repeatableIdSet)) {
            throw new IllegalArgumentException("Repeatable argument IDs must be unique valid argument IDs");
        }
    }

    /**
     * Adds command argument IDs while rejecting invalid or duplicate declarations.
     *
     * @param allIds IDs already declared by the command
     * @param ids IDs to validate and add
     */
    private void addUniqueIds(Set<String> allIds, String[] ids) {
        for (String id : ids) {
            if (id == null || id.isBlank() || id.indexOf(SLASH_CHAR) >= 0 || !id.equals(id.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Argument IDs must be non-blank lowercase strings without '/'");
            }
            if (!allIds.add(id)) {
                throw new IllegalArgumentException("Duplicate argument ID declared: " + id);
            }
        }
    }

    /**
     * Separates an input line at spaces while preserving spaces inside double quotes.
     * For example, {@code test a/1 b/"I love CS2113"} becomes three tokens rather
     * than splitting {@code "I love CS2113"} into two tokens.
     *
     * @param input input line to separate
     * @return separated command and argument tokens
     */
    private ArrayList<String> separate(String input) {
        return separate(null, input);
    }

    /**
     * Recursively separates the remaining input and accumulates its tokens.
     *
     * @param sepList tokens collected so far, or null when starting
     * @param s remaining input to separate
     * @return all separated tokens
     */
    private ArrayList<String> separate(ArrayList<String> sepList, String s) {
        ArrayList<String> resultSepList = (sepList == null ? new ArrayList<>() : sepList);
        String sTrim = s.trim();

        int quoteIndex = sTrim.indexOf(QUOTE_CHAR);
        int spaceIndex = sTrim.indexOf(SPACE_CHAR);
        if (quoteIndex != -1 && quoteIndex < spaceIndex) {
            int nextQuoteIndex = sTrim.indexOf(QUOTE_CHAR, quoteIndex + 1);
            if (nextQuoteIndex != -1) {
                spaceIndex = sTrim.indexOf(SPACE_CHAR, nextQuoteIndex);
            }
        }

        if (spaceIndex == -1) {
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

        HashMap<String, String> args = new HashMap<>();
        Set<String> argIds = new HashSet<>();

        Set<String> validArgIds = Set.of(command.validArgIds());
        Set<String> requiredArgIds = Set.of(command.requiredArgIds());
        Set<String> flagArgIds = Set.of(command.flagArgIds());
        Set<String> repeatableArgIds = Set.of(command.repeatableArgIds());

        for (String strArg : strArgs) {
            String[] arg = parseArg(strArg, flagArgIds);
            String argId = arg[0];
            if (argIds.contains(argId)) {
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

        if (!validArgIds.containsAll(argIds)) {
            Set<String> wrongArgIds = new HashSet<>(argIds);
            wrongArgIds.removeAll(validArgIds);
            throw new CommandParseException("Argument(s) starting with " + String.join(", ", wrongArgIds)
                    + " are invalid.");
        }

        if (!argIds.containsAll(requiredArgIds)) {
            Set<String> missingArgId = new HashSet<>(requiredArgIds);
            missingArgId.removeAll(argIds);
            throw new CommandParseException("Argument(s) starting with " + String.join(", ", missingArgId)
                    + " are required but missing.");
        }

        command.validateArgs(args);
        assert validArgIds.containsAll(args.keySet()) : "Parser returned an invalid argument ID";
        assert args.keySet().containsAll(requiredArgIds) : "Parser omitted a required argument";
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
        String lowercaseArg = strArg.toLowerCase(Locale.ROOT);
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
        try {
            argId = separatedArg[0].toLowerCase(Locale.ROOT);
            argVal = separatedArg[1];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new CommandParseException("Argument \"" + strArg + "\" has no \"" + SLASH_CHAR +"\".");
        }

        if (flagArgIds.contains(argId)) {
            throw new CommandParseException("Flag \"" + argId + "\" must not contain \"" + SLASH_CHAR + "\".");
        }

        int startQuoteIndex = argVal.indexOf(QUOTE_CHAR);
        if (startQuoteIndex != -1) {
            if (startQuoteIndex != 0) {
                throw new CommandParseException("Argument " + strArg
                        + " has incorrect position of starting quotation mark ("
                        + QUOTE_CHAR + ").");
            }
            int endQuoteIndex = argVal.indexOf(QUOTE_CHAR, startQuoteIndex + 1);
            if (endQuoteIndex == -1) {
                throw new CommandParseException("Argument " + strArg
                        + " does not have an ending quotations mark(" + QUOTE_CHAR + ").");
            }
            if (endQuoteIndex < argVal.length() - 1) {
                throw new CommandParseException("Argument " + strArg
                        + " has incorrect position of ending quotation mark (" + QUOTE_CHAR + ").");
            }
        }
        return new String[] {argId, argVal.replace(String.valueOf(QUOTE_CHAR), "")};
    }

    /**
     * Prints all supported syntaxes of a specified command.
     *
     * @param keyword The keyword of the command to display.
     */
    public void printCommandSyntax(String keyword){
        Command cmd = commands.get(keyword.toLowerCase());

        String nonAliasKeyword = keyword.toLowerCase();

        if (aliases.containsKey(keyword.toLowerCase())){
            nonAliasKeyword = aliases.get(keyword.toLowerCase());
        }

        ui.print("Syntax of command \"" + nonAliasKeyword + "\":");

        for (String syntax : cmd.getSyntaxes()){
            ui.print("> " + nonAliasKeyword + " " + syntax);
        }

        List<String> keywordAliases = aliasMap.getOrDefault(nonAliasKeyword, new ArrayList<>());
        if (!keywordAliases.isEmpty()){
            ui.print("Aliases: " + keywordAliases);
        }
    }

    /**
     * Prints the supported syntaxes of all registered commands,
     * excluding command aliases to avoid duplicate entries.
     */
    public void printAllCommandSyntax(){
        for (String keyword: commands.keySet()){
            if (!aliases.containsKey(keyword)){
                ui.print("");
                printCommandSyntax(keyword);
            }
        }
    }

    /**
     * Checks whether a command keyword is registered in the parser.
     * The keyword is case-insensitive.
     *
     * @param keyword The command keyword to check.
     * @return True if the command exists, false otherwise.
     */
    public boolean commandExist(String keyword){
        return commands.containsKey(keyword.toLowerCase());
    }
}
