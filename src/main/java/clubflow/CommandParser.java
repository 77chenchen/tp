package clubflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class CommandParser {

    private static final char QUOTE_CHAR = '\"';
    private static final char SPACE_CHAR = ' ';
    private static final char SLASH_CHAR = '/';

    HashMap<String, Command> commands;

    public CommandParser(UserInterface ui){
        commands = new HashMap<String, Command>();
    }

    public void register(String keyword, Command command){
        commands.put(keyword.toLowerCase(), command);
    }

    public void parse(String input) throws CommandParseException {
        ArrayList<String> sepList = separate(input);
        String commandKeyword = sepList.getFirst().toLowerCase();
        for(String k : commands.keySet()){
            if (commandKeyword.equals(k)){
                Command command = commands.get(k);
                ArrayList<CommandArg> args = parseArgs(sepList, command);
            }
        }
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

    private ArrayList<CommandArg> parseArgs(ArrayList<String> sepList, Command command) throws CommandParseException {
        ArrayList<String> strArgs = sepList;
        strArgs.removeFirst();

        ArrayList<CommandArg> args = new ArrayList<CommandArg>();
        ArrayList<String> validArgIds = (ArrayList<String>) Arrays.asList(command.validArgIds());

        for(String strArg : strArgs){
            CommandArg arg = parseArg(strArg);
            if (!validArgIds.contains(arg.getId())){ //TODO Arg id is invalid
                throw new CommandParseException();
            }
            args.add(arg);
        }

        ArrayList<String> requiredArgIds = (ArrayList<String>) Arrays.asList(command.requiredArgIds());
        for (String requiredArgId : requiredArgIds){

        }

        return args;
    }

    private CommandArg parseArg(String strArg) throws CommandParseException {
        String[] separatedArg = strArg.split(String.valueOf(SLASH_CHAR), 2);
        try{
            return new CommandArg(separatedArg[0], separatedArg[1]);
        } catch (Exception e) {
            throw new CommandParseException(); //TODO No "/" found in argument
        }
    }
}

