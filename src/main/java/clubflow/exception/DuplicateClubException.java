//@@author camimaigrot
package clubflow.exception;

/**
 * Thrown when attempting to create an existing club.
 */
public class DuplicateClubException extends ClubFlowException {

    public DuplicateClubException(String clubName) {
        super("Club already exists", clubName);
    }
}
