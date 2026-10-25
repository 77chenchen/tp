package clubflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

import clubflow.event.Event;
import clubflow.event.EventAssignment;
import clubflow.member.Member;

/**
 * Stores the members, events, and their assignments managed by ClubFlow.
 */
public class ClubData {
    private static final Logger LOGGER = Logger.getLogger(ClubData.class.getName());

    // These mutable lists remain private; callers receive read-only snapshots.
    private final List<Member> members = new ArrayList<>();
    private final List<Event> events = new ArrayList<>();
    //@@author 77chenchen
    /** Assignments refer to the actual objects, so deleting a preceding member cannot change their meaning. */
    private final List<EventAssignment> assignments = new ArrayList<>();
    //@@author

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
        //@@author 77chenchen
        if (members.stream().anyMatch(existing -> existing == member)) {
            throw new IllegalArgumentException("The same member object cannot be added twice");
        }
        //@@author
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

    //@@author 77chenchen

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
        assignments.removeIf(assignment -> assignment.getMember() == removedMember);
        assert members.size() == previousSize - 1 : "Removing a member must decrease the member count";
        LOGGER.fine(() -> "Removed member; member count is now " + members.size());
        unsavedChanges = true;
        return Optional.of(removedMember);
    }

    //@@author

    //@@author 77chenchen
    /**
     * Replaces a member after the caller has created and validated the replacement.
     * Assignments are transferred to the new immutable member object.
     *
     * @param index one-based member index
     * @param replacement validated replacement member
     * @return the previous member, or empty if the index does not exist
     */
    public Optional<Member> replaceMemberByIndex(int index, Member replacement) {
        Objects.requireNonNull(replacement, "Replacement member must not be null");
        if (index < 1 || index > members.size()) {
            return Optional.empty();
        }
        for (int memberIndex = 0; memberIndex < members.size(); memberIndex++) {
            if (memberIndex != index - 1 && members.get(memberIndex) == replacement) {
                throw new IllegalArgumentException("Replacement already belongs to another member index");
            }
        }

        Member previous = members.get(index - 1);
        List<EventAssignment> updatedAssignments = new ArrayList<>(assignments.size());
        for (EventAssignment assignment : assignments) {
            Member assignedMember = assignment.getMember() == previous ? replacement : assignment.getMember();
            updatedAssignments.add(new EventAssignment(assignment.getEvent(), assignedMember, assignment.getRole()));
        }

        members.set(index - 1, replacement);
        assignments.clear();
        assignments.addAll(updatedAssignments);
        assert assignments.stream().noneMatch(assignment -> assignment.getMember() == previous)
                : "Assignments must no longer reference the replaced member";
        LOGGER.fine(() -> "Updated member at index " + index);
        unsavedChanges = true;
        return Optional.of(previous);
    }

    /**
     * Searches all visible member details without changing their order.
     *
     * @param keyword non-blank search text
     * @return matching members in their original list order
     */
    public List<Member> findMembers(String keyword) {
        Objects.requireNonNull(keyword, "Search keyword must not be null");
        if (keyword.isBlank()) {
            throw new IllegalArgumentException("Search keyword must not be blank");
        }
        String query = keyword.strip().toLowerCase(Locale.ROOT);
        return members.stream().filter(member -> matchesMember(member, query)).toList();
    }

    /** Returns whether the query appears in a member field or function. */
    private boolean matchesMember(Member member, String query) {
        return member.getName().toLowerCase(Locale.ROOT).contains(query)
                || member.getTelegramHandle().toLowerCase(Locale.ROOT).contains(query)
                || member.getEmail().toLowerCase(Locale.ROOT).contains(query)
                || member.getFunctions().stream()
                        .anyMatch(function -> function.toLowerCase(Locale.ROOT).contains(query));
    }
    //@@author

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
     * Retrieves an event with the specified tag.
     *
     * @param tag tag of the event to retrieve
     * @return the matching event, or null if no event is found
     */
    public Event getEvent(String tag){
        for(Event e : getEvents()){
            if (e.getTag().equals(tag)){
                return e;
            }
        }
        return null;
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
        //@@author 77chenchen
        Event previousEvent = existingEvent.get();
        List<EventAssignment> updatedAssignments = new ArrayList<>(assignments.size());
        for (EventAssignment assignment : assignments) {
            Event assignedEvent = assignment.getEvent() == previousEvent ? updatedEvent : assignment.getEvent();
            updatedAssignments.add(new EventAssignment(assignedEvent, assignment.getMember(), assignment.getRole()));
        }
        //@@author
        int previousSize = events.size();
        events.set(events.indexOf(previousEvent), updatedEvent);
        //@@author 77chenchen
        assignments.clear();
        assignments.addAll(updatedAssignments);
        //@@author
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
        //@@author 77chenchen
        assignments.removeIf(assignment -> assignment.getEvent() == eventToRemove.get());
        //@@author
        assert removed && events.size() == previousSize - 1
                : "A found event must be removed exactly once";
        LOGGER.fine(() -> "Removed event; event count is now " + events.size());
        unsavedChanges = true;
        return removed;
    }

    //@@author 77chenchen
    /**
     * Assigns an existing member to an existing event once.
     *
     * @param eventTag case-insensitive tag of an existing event
     * @param member member already held by this data store
     * @param role optional responsibility
     * @return false if the member is already assigned to the event
     */
    public boolean assignMemberToEvent(String eventTag, Member member, String role) {
        Objects.requireNonNull(eventTag, "Event tag must not be null");
        Objects.requireNonNull(member, "Member must not be null");
        Event event = findEventByTag(eventTag)
                .orElseThrow(() -> new IllegalArgumentException("Event does not exist"));
        if (members.stream().noneMatch(existing -> existing == member)) {
            throw new IllegalArgumentException("Member does not belong to this club");
        }
        EventAssignment assignment = new EventAssignment(event, member, role);
        boolean alreadyAssigned = assignments.stream().anyMatch(existing -> existing.getEvent() == event
                && existing.getMember() == member);
        if (alreadyAssigned) {
            LOGGER.fine("Rejected duplicate event assignment");
            return false;
        }
        int previousSize = assignments.size();
        assignments.add(assignment);
        assert assignments.size() == previousSize + 1 : "Adding an assignment must increase its count";
        LOGGER.fine(() -> "Assigned a member to event " + event.getTag());
        unsavedChanges = true;
        return true;
    }

    /**
     * Removes one member's assignment without deleting the member or event.
     *
     * @param eventTag tag of an existing event
     * @param member existing member to unassign
     * @return true if an assignment was removed
     */
    public boolean unassignMemberFromEvent(String eventTag, Member member) {
        Objects.requireNonNull(eventTag, "Event tag must not be null");
        Objects.requireNonNull(member, "Member must not be null");
        Event event = findEventByTag(eventTag)
                .orElseThrow(() -> new IllegalArgumentException("Event does not exist"));
        if (members.stream().noneMatch(existing -> existing == member)) {
            throw new IllegalArgumentException("Member does not belong to this club");
        }
        boolean removed = assignments.removeIf(assignment -> assignment.getEvent() == event
                && assignment.getMember() == member);
        if (removed) {
            LOGGER.fine(() -> "Removed a member assignment from event " + event.getTag());
            unsavedChanges = true;
        }
        return removed;
    }

    /**
     * Returns assignments for one event in assignment order.
     *
     * @param eventTag event tag, ignoring case
     * @return a read-only list of assignments
     */
    public List<EventAssignment> getAssignmentsForEvent(String eventTag) {
        Objects.requireNonNull(eventTag, "Event tag must not be null");
        return assignments.stream()
                .filter(assignment -> assignment.getEvent().getTag().equalsIgnoreCase(eventTag))
                .toList();
    }

    /** Returns a read-only snapshot for persistence. */
    public List<EventAssignment> getAssignments() {
        return List.copyOf(assignments);
    }
    //@@author

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
        //@@author 77chenchen
        List<EventAssignment> loadedAssignments = loaded.getAssignments();
        //@@author

        members.clear();
        events.clear();
        //@@author 77chenchen
        assignments.clear();
        //@@author

        members.addAll(loadedMembers);
        events.addAll(loadedEvents);
        //@@author 77chenchen
        assignments.addAll(loadedAssignments);
        assert assignments.size() == loadedAssignments.size() : "All loaded assignments must be copied";
        //@@author

        assert members.size() == loadedMembers.size() : "All loaded members must be copied";
        assert events.size() == loadedEvents.size() : "All loaded events must be copied";
        LOGGER.info(() -> "Replaced club data with " + members.size() + " members and "
                + events.size() + " events");
        unsavedChanges = true;
    }
}
