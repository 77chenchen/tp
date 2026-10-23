package clubflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

import clubflow.event.Event;
import clubflow.member.Member;

/**
 * Stores the members and events managed by ClubFlow.
 */
public class ClubData {
    private static final Logger LOGGER = Logger.getLogger(ClubData.class.getName());

    // These mutable lists remain private; callers receive read-only snapshots.
    private final List<Member> members = new ArrayList<>();
    private final List<Event> events = new ArrayList<>();

    /** Tracks whether there are any unsaved changes */
    private boolean unsavedChanges = false;

    /** Returns whether there are unsaved changes or not */
    public boolean hasUnsavedChanges() {
        return unsavedChanges;
    }

    /** Marks the current club data as saved */
    public void markSaved() {
        unsavedChanges = false;
        return;
    }

    /**
     * Adds a member to the club.
     *
     * @param member member to add
     */
    public void addMember(Member member) {
        Objects.requireNonNull(member, "Member must not be null");
        int previousSize = members.size();
        members.add(member);
        assert members.size() == previousSize + 1 : "Adding a member must increase the member count";
        LOGGER.fine(() -> "Added member; member count is now " + members.size());
        unsavedChanges = true;
    }

    /**
     * Adds an event to the club.
     * Event tags are compared case-insensitively and must be unique.
     *
     * @param event event to add
     * @return true if the event was added, or false if its tag already exists
     */
    public boolean addEvent(Event event) {
        Objects.requireNonNull(event, "Event must not be null");
        if (findEventByTag(event.getTag()).isPresent()) {
            LOGGER.fine(() -> "Rejected duplicate event tag: " + event.getTag());
            return false;
        }
        int previousSize = events.size();
        events.add(event);
        assert events.size() == previousSize + 1 : "Adding an event must increase the event count";
        LOGGER.fine(() -> "Added event; event count is now " + events.size());
        unsavedChanges = true;
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
        int previousSize = members.size();
        Member removedMember = members.remove(index - 1);
        assert members.size() == previousSize - 1 : "Removing a member must decrease the member count";
        LOGGER.fine(() -> "Removed member; member count is now " + members.size());
        unsavedChanges = true;
        return Optional.of(removedMember);
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
        Objects.requireNonNull(tag, "Event tag must not be null");
        return events.stream()
                .filter(event -> event.getTag().equalsIgnoreCase(tag))
                .findFirst();
    }

    /**
     * Replaces an existing event with updated details, preserving its position in the list.
     * The replacement must use the existing tag, compared case-insensitively.
     * Previously returned snapshots keep the original immutable event.
     *
     * @param updatedEvent replacement event containing the tag and updated details
     * @return true if an event with that tag was updated, or false if none exists
     */
    public boolean updateEvent(Event updatedEvent) {
        Objects.requireNonNull(updatedEvent, "Updated event must not be null");
        Optional<Event> existingEvent = findEventByTag(updatedEvent.getTag());
        if (existingEvent.isEmpty()) {
            return false;
        }
        int previousSize = events.size();
        events.set(events.indexOf(existingEvent.get()), updatedEvent);
        assert events.size() == previousSize : "Updating an event must preserve the event count";
        LOGGER.fine(() -> "Updated event: " + updatedEvent.getTag());
        unsavedChanges = true;
        return true;
    }

    /**
     * Removes the event with the given tag.
     *
     * @param tag tag of the event to remove
     * @return true if an event was removed
     */
    public boolean removeEventByTag(String tag) {
        Objects.requireNonNull(tag, "Event tag must not be null");
        Optional<Event> eventToRemove = findEventByTag(tag);
        if (eventToRemove.isEmpty()) {
            return false;
        }
        int previousSize = events.size();
        boolean removed = events.remove(eventToRemove.get());
        assert removed && events.size() == previousSize - 1
                : "A found event must be removed exactly once";
        LOGGER.fine(() -> "Removed event; event count is now " + events.size());
        unsavedChanges = true;
        return removed;
    }

    /**
     * Replaces this store's contents while keeping the same object shared by all commands.
     * Takes snapshots before clearing so replacing a store with itself preserves its contents.
     *
     * @param loaded club data whose members and events should become current
     */
    public void replaceWith(ClubData loaded) {
        Objects.requireNonNull(loaded, "Loaded club data must not be null");
        List<Member> loadedMembers = loaded.getMembers();
        List<Event> loadedEvents = loaded.getEvents();

        members.clear();
        events.clear();

        members.addAll(loadedMembers);
        events.addAll(loadedEvents);

        assert members.size() == loadedMembers.size() : "All loaded members must be copied";
        assert events.size() == loadedEvents.size() : "All loaded events must be copied";
        LOGGER.info(() -> "Replaced club data with " + members.size() + " members and "
                + events.size() + " events");
        unsavedChanges = true;
    }
}
