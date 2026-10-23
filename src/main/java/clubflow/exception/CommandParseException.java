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

    /**
     * Checks whether the correct command syntax should be printed.
     *
     * @return True if the command syntax should be printed, false otherwise.
     */
    public boolean willPrintSyntax(){
        return printSyntax;
    }

    /**
     * Sets the command keyword and enables syntax printing.
     * The keyword is case-insensitive.
     *
     * @param keyword The command keyword to set.
     */
    public void setCommandKeyword(String keyword){
        this.keyword = keyword.toLowerCase();
        printSyntax = true;
    }

    /**
     * Prints the correct syntax of the command using the specified parser.
     *
     * @param parser The command parser used to print the syntax.
     */
    public void printCorrectSyntax(CommandParser parser){
        parser.printCommandSyntax(keyword);
    }
}
