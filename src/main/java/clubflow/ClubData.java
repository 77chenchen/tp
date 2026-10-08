package clubflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import clubflow.event.Event;
import clubflow.member.Member;

/**
 * Stores the members and events managed by ClubFlow.
 */
public class ClubData {
    // These mutable lists remain private; callers receive read-only snapshots.
    private final List<Member> members = new ArrayList<>();
    private final List<Event> events = new ArrayList<>();

    /**
     * Adds a member to the club.
     *
     * @param member member to add
     */
    public void addMember(Member member) {
        members.add(member);
    }

    /**
     * Adds an event to the club.
     * Event tags are compared case-insensitively and must be unique.
     *
     * @param event event to add
     * @return true if the event was added, or false if its tag already exists
     */
    public boolean addEvent(Event event) {
        if (findEventByTag(event.getTag()).isPresent()) {
            return false;
        }
        events.add(event);
        return true;
    }

    /**
     * Returns a read-only snapshot of the club's members.
     * Later changes to ClubData are not reflected in the returned list.
     *
     * @return members currently stored in ClubFlow
     */
    public List<Member> getMembers() {
        return List.copyOf(members);
    }

    /**
     * Finds a member using the one-based index shown by the member list.
     *
     * @param index one-based member index
     * @return matching member, or an empty Optional if the index is out of range
     */
    public Optional<Member> findMemberByIndex(int index) {
        if (index < 1 || index > members.size()) {
            return Optional.empty();
        }
        return Optional.of(members.get(index - 1));
    }

    /**
     * Removes a member using the one-based index shown by the member list.
     *
     * @param index one-based member index
     * @return the removed member, or an empty Optional if the index is out of range
     */
    public Optional<Member> removeMemberByIndex(int index) {
        if (index < 1 || index > members.size()) {
            return Optional.empty();
        }
        return Optional.of(members.remove(index - 1));
    }

    /**
     * Returns a read-only snapshot of the club's events.
     * Later changes to ClubData are not reflected in the returned list.
     *
     * @return events currently stored in ClubFlow
     */
    public List<Event> getEvents() {
        return List.copyOf(events);
    }

    /**
     * Finds an event using its case-insensitive tag.
     *
     * @param tag event tag to find
     * @return matching event, or an empty Optional if none exists
     */
    public Optional<Event> findEventByTag(String tag) {
        return events.stream()
                .filter(event -> event.getTag().equalsIgnoreCase(tag))
                .findFirst();
    }

    /**
     * Removes the event with the given tag.
     *
     * @param tag tag of the event to remove
     * @return true if an event was removed
     */
    public boolean removeEventByTag(String tag) {
        Optional<Event> eventToRemove = findEventByTag(tag);
        if (eventToRemove.isEmpty()) {
            return false;
        }
        return events.remove(eventToRemove.get());
    }
}
