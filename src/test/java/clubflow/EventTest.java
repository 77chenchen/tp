//@@author camimaigrot
package clubflow;

import clubflow.event.Event;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    //@@author Eugene3129
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

    @Test
    void constructor_invalidBudgets_rejectsNegativeAndNonFiniteValues() {
        for (double budget : new double[]{-0.01, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new Event("AGM", "Meeting", null, null, budget, "", ""));
        }
    }

    @Test
    void constructor_fieldSeparators_rejectsEachTextField() {
        assertThrows(IllegalArgumentException.class, () -> new Event("A|B", "", null, null, 0, "", ""));
        assertThrows(IllegalArgumentException.class, () -> new Event("AGM", "A|B", null, null, 0, "", ""));
        assertThrows(IllegalArgumentException.class, () -> new Event("AGM", "", null, null, 0, "A|B", ""));
        assertThrows(IllegalArgumentException.class, () -> new Event("AGM", "", null, null, 0, "", "A|B"));
    }

    @Test
    void constructor_nullTextFields_rejectsMissingDomainValues() {
        assertThrows(NullPointerException.class, () -> new Event(null, "", null, null, 0, "", ""));
        assertThrows(NullPointerException.class, () -> new Event("AGM", null, null, null, 0, "", ""));
        assertThrows(NullPointerException.class, () -> new Event("AGM", "", null, null, 0, null, ""));
        assertThrows(NullPointerException.class, () -> new Event("AGM", "", null, null, 0, "", null));
    }

    @Test
    void withCompleted_preservesPhotosAndKeepsRegistrationsIndependent() throws Exception {
        Event original = new Event("AGM", "Meeting", null, null, 0, "", "");
        original.loadPhoto("existing");

        Event completed = original.withCompleted(true);
        completed.loadPhoto("new");
        List<String> snapshot = original.getPhotoNames();
        snapshot.clear();

        assertEquals(List.of("existing"), original.getPhotoNames());
        assertEquals(2, completed.getPhotoNames().size());
        assertTrue(completed.getPhotoNames().containsAll(List.of("existing", "new")));
        assertFalse(original.isCompleted());
        assertTrue(completed.isCompleted());
    }
}
