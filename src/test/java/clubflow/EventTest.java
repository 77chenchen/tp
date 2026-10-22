//@@author camimaigrot
package clubflow;

import clubflow.event.Event;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests event details and immutable completion-state changes.
 */
public class EventTest {
    @Test
    public void constructor_validInputs_storesValuesCorrectly() {
        Event event = new Event(
                "XMAS",
                "Christmas Party",
                LocalDate.of(2026, 12, 25),
                LocalDate.of(2026, 12, 20),
                500.0,
                "Main Hall",
                "Tables and chairs"
        );

        assertEquals("XMAS", event.getTag());
        assertEquals("Christmas Party", event.getName());
        assertEquals(LocalDate.of(2026, 12, 25), event.getDate());
        assertEquals(LocalDate.of(2026, 12, 20), event.getDeadline());
        assertEquals(500.0, event.getBudget());
        assertEquals("Main Hall", event.getVenue());
        assertEquals("Tables and chairs", event.getLogistics());
        assertFalse(event.isCompleted());
    }

    @Test
    public void withCompleted_changesStateWithoutChangingOriginalDetails() {
        Event original = new Event("XMAS", "Party", LocalDate.of(2026, 12, 25),
                LocalDate.of(2026, 12, 20), 500, "Hall", "Chairs");

        Event completed = original.withCompleted(true);
        Event unmarked = completed.withCompleted(false);

        assertFalse(original.isCompleted());
        assertTrue(completed.isCompleted());
        assertFalse(unmarked.isCompleted());
        assertEquals(original.getTag(), completed.getTag());
        assertEquals(original.getName(), completed.getName());
        assertEquals(original.getDate(), completed.getDate());
        assertEquals(original.getDeadline(), completed.getDeadline());
        assertEquals(original.getBudget(), completed.getBudget());
        assertEquals(original.getVenue(), completed.getVenue());
        assertEquals(original.getLogistics(), completed.getLogistics());
    }
}
