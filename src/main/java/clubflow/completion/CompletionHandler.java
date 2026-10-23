package clubflow.completion;

import clubflow.exception.ClubFlowException;

/**
 * Updates completion for one item type and allows application errors to reach the main loop.
 */
@FunctionalInterface
public interface CompletionHandler {
    /**
     * Sets the completion state of the identified item.
     *
     * @param identifier identifier understood by this item type's handler
     * @param completed desired completion state
     * @throws ClubFlowException if the request cannot be completed
     */
    void setCompleted(String identifier, boolean completed) throws ClubFlowException;
}
