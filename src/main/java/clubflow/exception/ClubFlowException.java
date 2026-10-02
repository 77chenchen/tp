package clubflow.exception;

/**
 * Represents an exception thrown by the app.
 * Standardizes the printing of error messages.
 */
public abstract class ClubFlowException extends Exception{

    private String issue;
    private String description;

    /**
     * Creates an exception.
     * If issue is "Parsing error", and description is "3rd argument of command is not a number.",
     * The exception message would be "Parsing Error: 3rd argument of command is not a number.".
     * @param issue The main issue of what happened.
     * @param description A detail description of what happened.
     */
    public ClubFlowException(String issue, String description){
        this.issue = issue;
        this.description = description;
    }

    //TODO: Can change the aesthetic of error printing here.
    @Override
    public String getMessage() {
        return issue + ": " + description;
    }
}
