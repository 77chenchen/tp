package clubflow.command;

import clubflow.exception.CommandParseException;
import clubflow.UserInterface;

import java.util.*;

public class CommandParser {

    private static final char QUOTE_CHAR = '\"';
    private static final char SPACE_CHAR = ' ';
    private static final char SLASH_CHAR = '/';

    UserInterface ui;
    HashMap<String, Command> commands;

    public CommandParser(UserInterface ui){
        this.ui = ui;
        commands = new HashMap<String, Command>();
    }

    public void register(String keyword, Command command){
        commands.put(keyword.toLowerCase(), command);
    }

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

    private ArrayList<String> separate(String s){
        return separate(null, s);
    }

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

