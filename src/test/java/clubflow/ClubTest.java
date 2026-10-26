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
public class ClubTest {

    @Test
    public void constructor_noData_hasEmptyCollections() {
        Club club = new Club();

        assertEquals(List.of(), club.getMembers());
        assertEquals(List.of(), club.getEvents());
    }

    @Test
    public void addMember_validMember_storesMember() {
        Club club = new Club();
        Member member = new Member(
                "Lucy",
                "lucy123",
                "lucy.club@yahoo.com",
                List.of("president")
        );

        club.addMember(member);

        assertEquals(List.of(member), club.getMembers());
    }

    @Test
    public void addEvent_validEvent_storesEvent() {
        Club club = new Club();
        Event event = createEvent("XMAS");

        club.addEvent(event);

        assertEquals(List.of(event), club.getEvents());
    }

    @Test
    public void addEvent_duplicateTag_rejectsDuplicateIgnoringCase() {
        Club club = new Club();
        Event firstEvent = createEvent("XMAS");
        Event duplicateEvent = createEvent("xmas");

        assertTrue(club.addEvent(firstEvent));
        assertFalse(club.addEvent(duplicateEvent));
        assertEquals(List.of(firstEvent), club.getEvents());
    }

    @Test
    public void findAndRemoveEvent_existingTag_findsThenRemovesEvent() {
        Club club = new Club();
        Event event = createEvent("XMAS");
        club.addEvent(event);

        assertEquals(event, club.findEventByTag("xmas").orElseThrow());
        assertTrue(club.removeEventByTag("xmas"));
        assertEquals(List.of(), club.getEvents());
        assertFalse(club.removeEventByTag("xmas"));
    }

    @Test
    public void updateEvent_existingTag_preservesOrderAndOldSnapshots() {
        Club club = new Club();
        Event original = createEvent("XMAS");
        Event other = createEvent("OTHER");
        club.addEvent(original);
        club.addEvent(other);
        List<Event> snapshot = club.getEvents();
        Event updated = new Event("xmas", "Dinner", null, null, 600, "Restaurant", "");

        assertTrue(club.updateEvent(updated));

        assertEquals(List.of(updated, other), club.getEvents());
        assertEquals(List.of(original, other), snapshot);
        assertEquals("Christmas Party", original.getName());
        assertEquals(updated, club.findEventByTag("XMAS").orElseThrow());
    }

    @Test
    public void updateEvent_missingOrNullEvent_keepsStoreUnchanged() {
        Club club = new Club();
        Event original = createEvent("XMAS");
        club.addEvent(original);

        assertFalse(club.updateEvent(createEvent("UNKNOWN")));
        assertThrows(NullPointerException.class, () -> club.updateEvent(null));

        assertEquals(List.of(original), club.getEvents());
    }

    @Test
    public void getters_returnedCollections_cannotBeModified() {
        Club club = new Club();

        assertThrows(UnsupportedOperationException.class,
                () -> club.getMembers().add(null));
        assertThrows(UnsupportedOperationException.class,
                () -> club.getEvents().add(null));
    }

    @Test
    public void replaceWith_loadedData_replacesBothCollectionsAndRetainsSnapshots() {
        Club club = new Club();
        Member originalMember = new Member("Lucy", "lucy", "", List.of());
        Event originalEvent = createEvent("OLD");
        club.addMember(originalMember);
        club.addEvent(originalEvent);
        List<Member> memberSnapshot = club.getMembers();
        List<Event> eventSnapshot = club.getEvents();
        Club loaded = new Club();
        Member loadedMember = new Member("Thomas", "", "thomas@example.com", List.of());
        Event loadedEvent = createEvent("NEW");
        loaded.addMember(loadedMember);
        loaded.addEvent(loadedEvent);

        club.replaceWith(loaded);
        // Later edits to the source store must not affect the destination store.
        loaded.removeMemberByIndex(1);
        loaded.removeEventByTag("NEW");

        assertEquals(List.of(loadedMember), club.getMembers());
        assertEquals(List.of(loadedEvent), club.getEvents());
        assertEquals(List.of(originalMember), memberSnapshot);
        assertEquals(List.of(originalEvent), eventSnapshot);
    }

    @Test
    public void replaceWith_sameStore_preservesMembersAndEvents() {
        Club club = new Club();
        Member member = new Member("Lucy", "lucy", "", List.of());
        Event event = createEvent("XMAS");
        club.addMember(member);
        club.addEvent(event);

        club.replaceWith(club);

        assertEquals(List.of(member), club.getMembers());
        assertEquals(List.of(event), club.getEvents());
    }

    @Test
    public void findAndRemoveMember_invalidIndexes_leaveMembersUnchanged() {
        Club club = new Club();
        Member member = new Member("Lucy", "lucy", "", List.of());
        club.addMember(member);

        for (int index : new int[]{-1, 0, 2}) {
            assertTrue(club.findMemberByIndex(index).isEmpty());
            assertTrue(club.removeMemberByIndex(index).isEmpty());
        }

        assertEquals(List.of(member), club.getMembers());
    }

    @Test
    public void removeMember_existingIndex_shiftsLaterMemberIndexes() {
        Club club = new Club();
        Member firstMember = new Member("Lucy", "lucy", "", List.of());
        Member secondMember = new Member("Thomas", "", "thomas@example.com", List.of());
        club.addMember(firstMember);
        club.addMember(secondMember);

        assertEquals(firstMember, club.removeMemberByIndex(1).orElseThrow());

        assertEquals(secondMember, club.findMemberByIndex(1).orElseThrow());
        assertTrue(club.findMemberByIndex(2).isEmpty());
        assertEquals(List.of(secondMember), club.getMembers());
    }

    /**
     * Creates a valid event fixture with the supplied tag.
     *
     * @param tag event tag to use
     * @return event suitable for Club tests
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
