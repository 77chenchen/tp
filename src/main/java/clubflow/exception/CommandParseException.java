package clubflow.exception;

import clubflow.command.CommandParser;

/**
 * Represents an exception thrown when the command text cannot be parsed into a command object.
 */
public class CommandParseException extends ClubFlowException {

    private boolean printSyntax;
    private String keyword;

    public CommandParseException(String description) {
        super("Command syntax error", description);
        printSyntax = false;
    }

    public boolean willPrintSyntax(){
        return printSyntax;
    }

    public void setCommandKeyword(String keyword){
        this.keyword = keyword.toLowerCase();
        printSyntax = true;
    }

    public void printCorrectSyntax(CommandParser parser){
        parser.printCommandSyntax(keyword);
    }
}
