package clubflow.exception;

public abstract class ClubFlowException extends Exception{
    private String issue, description;

    public ClubFlowException(String issue, String description){
        this.issue = issue;
        this.description = description;
    }

    @Override
    public String toString() {
        return issue + ": " + description;
    }
}
