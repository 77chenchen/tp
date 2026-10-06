package clubflow;

import clubflow.event.Event;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestEvent {
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
    }
}
