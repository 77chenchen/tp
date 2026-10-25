package clubflow.event;

import java.util.Objects;

import clubflow.member.Member;

//@@author 77chenchen

/**
 * Associates one member with an event and an optional responsibility.
 * The member reference remains unambiguous even when two members have the same name.
 */
public class EventAssignment {
    private final Event event;
    private final Member member;
    private final String role;

    /**
     * Creates a validated event assignment.
     *
     * @param event event receiving the member
     * @param member assigned member
     * @param role optional responsibility, or an empty string
     */
    public EventAssignment(Event event, Member member, String role) {
        this.event = Objects.requireNonNull(event, "Event must not be null");
        this.member = Objects.requireNonNull(member, "Member must not be null");
        this.role = Objects.requireNonNull(role, "Role must not be null");
        if (role.contains("|") || role.contains("\n") || role.contains("\r")) {
            throw new IllegalArgumentException("Role must not contain | or a line break");
        }
    }

    public Event getEvent() {
        return event;
    }

    public Member getMember() {
        return member;
    }

    public String getRole() {
        return role;
    }
}

//@@author
