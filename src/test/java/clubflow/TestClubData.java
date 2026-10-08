package clubflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import clubflow.event.Event;
import clubflow.member.Member;

/**
 * Tests the in-memory club data store.
 */
public class TestClubData {

    @Test
    public void constructor_noData_hasEmptyCollections() {
        ClubData clubData = new ClubData();

        assertEquals(List.of(), clubData.getMembers());
        assertEquals(List.of(), clubData.getEvents());
    }

    @Test
    public void addMember_validMember_storesMember() {
        ClubData clubData = new ClubData();
        Member member = new Member(
                "Lucy",
                "lucy123",
                "lucy.club@yahoo.com",
                List.of("president")
        );

        clubData.addMember(member);

        assertEquals(List.of(member), clubData.getMembers());
    }

    @Test
    public void addEvent_validEvent_storesEvent() {
        ClubData clubData = new ClubData();
        Event event = createEvent("XMAS");

        clubData.addEvent(event);

        assertEquals(List.of(event), clubData.getEvents());
    }

    @Test
    public void addEvent_duplicateTag_rejectsDuplicateIgnoringCase() {
        ClubData clubData = new ClubData();
        Event firstEvent = createEvent("XMAS");
        Event duplicateEvent = createEvent("xmas");

        assertTrue(clubData.addEvent(firstEvent));
        assertFalse(clubData.addEvent(duplicateEvent));
        assertEquals(List.of(firstEvent), clubData.getEvents());
    }

    @Test
    public void findAndRemoveEvent_existingTag_findsThenRemovesEvent() {
        ClubData clubData = new ClubData();
        Event event = createEvent("XMAS");
        clubData.addEvent(event);

        assertEquals(event, clubData.findEventByTag("xmas").orElseThrow());
        assertTrue(clubData.removeEventByTag("xmas"));
        assertEquals(List.of(), clubData.getEvents());
        assertFalse(clubData.removeEventByTag("xmas"));
    }

    @Test
    public void getters_returnedCollections_cannotBeModified() {
        ClubData clubData = new ClubData();

        assertThrows(UnsupportedOperationException.class,
                () -> clubData.getMembers().add(null));
        assertThrows(UnsupportedOperationException.class,
                () -> clubData.getEvents().add(null));
    }

    /**
     * Creates a valid event fixture with the supplied tag.
     *
     * @param tag event tag to use
     * @return event suitable for ClubData tests
     */
    private Event createEvent(String tag) {
        return new Event(
                tag,
                "Christmas Party",
                LocalDate.of(2026, 12, 25),
                LocalDate.of(2026, 12, 20),
                500.0,
                "Main Hall",
                "Tables and chairs"
        );
    }
}
