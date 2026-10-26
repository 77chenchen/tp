//@@author camimaigrot
package clubflow.exception;

/**
 * Thrown when a requested club cannot be found.
 */
public class ClubNotFoundException extends ClubFlowException {

    public ClubNotFoundException(String clubName) {
        super("Club not found", clubName);
    }
}