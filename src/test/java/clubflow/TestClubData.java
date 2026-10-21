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

    @Test
    public void replaceWith_loadedData_replacesBothCollectionsAndRetainsSnapshots() {
        ClubData clubData = new ClubData();
        Member originalMember = new Member("Lucy", "lucy", "", List.of());
        Event originalEvent = createEvent("OLD");
        clubData.addMember(originalMember);
        clubData.addEvent(originalEvent);
        List<Member> memberSnapshot = clubData.getMembers();
        List<Event> eventSnapshot = clubData.getEvents();
        ClubData loaded = new ClubData();
        Member loadedMember = new Member("Thomas", "", "thomas@example.com", List.of());
        Event loadedEvent = createEvent("NEW");
        loaded.addMember(loadedMember);
        loaded.addEvent(loadedEvent);

        clubData.replaceWith(loaded);
        // Later edits to the source store must not affect the destination store.
        loaded.removeMemberByIndex(1);
        loaded.removeEventByTag("NEW");

        assertEquals(List.of(loadedMember), clubData.getMembers());
        assertEquals(List.of(loadedEvent), clubData.getEvents());
        assertEquals(List.of(originalMember), memberSnapshot);
        assertEquals(List.of(originalEvent), eventSnapshot);
    }

    @Test
    public void replaceWith_sameStore_preservesMembersAndEvents() {
        ClubData clubData = new ClubData();
        Member member = new Member("Lucy", "lucy", "", List.of());
        Event event = createEvent("XMAS");
        clubData.addMember(member);
        clubData.addEvent(event);

        clubData.replaceWith(clubData);

        assertEquals(List.of(member), clubData.getMembers());
        assertEquals(List.of(event), clubData.getEvents());
    }

    @Test
    public void findAndRemoveMember_invalidIndexes_leaveMembersUnchanged() {
        ClubData clubData = new ClubData();
        Member member = new Member("Lucy", "lucy", "", List.of());
        clubData.addMember(member);

        for (int index : new int[]{-1, 0, 2}) {
            assertTrue(clubData.findMemberByIndex(index).isEmpty());
            assertTrue(clubData.removeMemberByIndex(index).isEmpty());
        }

        assertEquals(List.of(member), clubData.getMembers());
    }

    @Test
    public void removeMember_existingIndex_shiftsLaterMemberIndexes() {
        ClubData clubData = new ClubData();
        Member firstMember = new Member("Lucy", "lucy", "", List.of());
        Member secondMember = new Member("Thomas", "", "thomas@example.com", List.of());
        clubData.addMember(firstMember);
        clubData.addMember(secondMember);

        assertEquals(firstMember, clubData.removeMemberByIndex(1).orElseThrow());

        assertEquals(secondMember, clubData.findMemberByIndex(1).orElseThrow());
        assertTrue(clubData.findMemberByIndex(2).isEmpty());
        assertEquals(List.of(secondMember), clubData.getMembers());
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
