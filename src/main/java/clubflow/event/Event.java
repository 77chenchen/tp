//@@author camimaigrot
package clubflow.event;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents a club event.
 */
public class Event {
    private final String tag;
    private final String name;
    private final LocalDate date;
    private final LocalDate deadline;
    private final double budget;
    private final String venue;
    private final String logistics;

    /** Whether this event has been marked as completed. */
    private final boolean completed;

    /**
     * Creates an event after validating values required by the domain and save format.
     * Dates may be null because they are optional.
     *
     * @param tag unique non-blank event tag
     * @param name event name, or an empty string
     * @param date optional event date
     * @param deadline optional planning deadline
     * @param budget finite non-negative event budget
     * @param venue event venue, or an empty string
     * @param logistics logistics notes, or an empty string
     */
    public Event(String tag, String name, LocalDate date, LocalDate deadline,
                 double budget, String venue, String logistics) {
        this(tag, name, date, deadline, budget, venue, logistics, false);
    }

    /**
     * Creates an event with an explicit completion state, including when loading saved data.
     *
     * @param tag unique non-blank event tag
     * @param name event name, or an empty string
     * @param date optional event date
     * @param deadline optional planning deadline
     * @param budget finite non-negative event budget
     * @param venue event venue, or an empty string
     * @param logistics logistics notes, or an empty string
     * @param completed whether the event is completed
     */
    public Event(String tag, String name, LocalDate date, LocalDate deadline,
                 double budget, String venue, String logistics, boolean completed) {
        this.tag = requireNonBlank(tag, "Event tag");
        this.name = requireText(name, "Event name");
        this.date = date;
        this.deadline = deadline;
        if (!Double.isFinite(budget) || budget < 0) {
            throw new IllegalArgumentException("Budget must be finite and non-negative");
        }
        this.budget = budget;
        this.venue = requireText(venue, "Venue");
        this.logistics = requireText(logistics, "Logistics");
        this.completed = completed;
    }

    /**
     * Validates an optional text field against null and the save-file separator.
     *
     * @param value field value
     * @param description field name used in the exception message
     * @return the validated value
     */
    private static String requireText(String value, String description) {
        Objects.requireNonNull(value, description + " must not be null");
        if (value.contains("|")) {
            throw new IllegalArgumentException(description + " must not contain |");
        }
        return value;
    }

    /**
     * Validates a required text field.
     *
     * @param value field value
     * @param description field name used in the exception message
     * @return the validated value
     */
    private static String requireNonBlank(String value, String description) {
        String checkedValue = requireText(value, description);
        if (checkedValue.isBlank()) {
            throw new IllegalArgumentException(description + " must not be blank");
        }
        return checkedValue;
    }

    public String getTag() {
        return tag;
    }

    public String getName() {
        return name;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public double getBudget() {
        return budget;
    }

    public String getVenue() {
        return venue;
    }

    public String getLogistics() {
        return logistics;
    }

    public boolean isCompleted() {
        return completed;
    }

    /**
     * Returns a copy with the requested completion state, leaving this event unchanged.
     *
     * @param completed desired completion state
     * @return event with the same details and the supplied completion state
     */
    public Event withCompleted(boolean completed) {
        return new Event(tag, name, date, deadline, budget, venue, logistics, completed);
    }
}
