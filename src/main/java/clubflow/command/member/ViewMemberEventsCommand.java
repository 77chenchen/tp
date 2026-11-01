package clubflow.command.member;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import clubflow.Club;
import clubflow.UserInterface;
import clubflow.command.Command;
import clubflow.event.Event;
import clubflow.event.EventAssignment;
import clubflow.exception.CommandParseException;
import clubflow.member.Member;

//@@author 77chenchen

/**
 * Displays every event assigned to one member together with the member's responsibilities.
 */
public class ViewMemberEventsCommand extends Command {
    private static final Comparator<EventAssignment> BY_EVENT_DATE = Comparator.comparing(
            (EventAssignment assignment) -> assignment.getEvent().getDate(),
            Comparator.nullsLast(Comparator.naturalOrder()));
    private static final Comparator<EventAssignment> BY_DATE_THEN_TAG = BY_EVENT_DATE.thenComparing(
            assignment -> assignment.getEvent().getTag(), String.CASE_INSENSITIVE_ORDER);

    /** Mutually exclusive statuses used to summarize a member's assigned events. */
    private enum AssignmentStatus {
        UPCOMING("UPCOMING"),
        OVERDUE("OVERDUE"),
        COMPLETED("COMPLETED"),
        NO_DATE("NO DATE");

        private final String displayName;

        AssignmentStatus(String displayName) {
            this.displayName = displayName;
        }
    }

    private final Club club;

    /**
     * Creates a command for viewing the events assigned to a member.
     *
     * @param ui user interface used to display assignment information
     * @param club shared club data containing members, events, and assignments
     */
    public ViewMemberEventsCommand(UserInterface ui, Club club) {
        super(ui);
        this.club = Objects.requireNonNull(club, "Club data must not be null");
        addSyntax("i/Index");
    }

    @Override
    public String[] requiredArgIds() {
        return new String[]{MemberArgumentIds.INDEX};
    }

    @Override
    public String[] optionalArgIds() {
        return new String[]{};
    }

    @Override
    public void validateArgs(HashMap<String, String> args) throws CommandParseException {
        try {
            if (Integer.parseInt(args.get(MemberArgumentIds.INDEX)) < 1) {
                throw new CommandParseException("Member index must be a positive number.");
            }
        } catch (NumberFormatException e) {
            throw new CommandParseException("Member index must be a positive number.");
        }
    }

    @Override
    public boolean execute(HashMap<String, String> args) {
        Objects.requireNonNull(args, "Arguments must not be null");
        assert args.containsKey(MemberArgumentIds.INDEX)
                : "Validated arguments must identify a member";

        int memberIndex = Integer.parseInt(args.get(MemberArgumentIds.INDEX));
        Member member = club.findMemberByIndex(memberIndex).orElse(null);
        if (member == null) {
            ui.print("Member not found at index " + memberIndex + ".");
            return false;
        }

        List<EventAssignment> assignments = new ArrayList<>(club.getAssignmentsForMember(member));
        assignments.sort(BY_DATE_THEN_TAG);
        if (assignments.isEmpty()) {
            ui.print("No events assigned to " + member.getName() + ".");
            return false;
        }

        LocalDate today = LocalDate.now();
        EnumMap<AssignmentStatus, Integer> statusCounts = countStatuses(assignments, today);
        ui.print("Events assigned to " + member.getName() + " (member " + memberIndex + "): "
                + assignments.size());
        ui.print("Summary: " + statusCounts.get(AssignmentStatus.UPCOMING) + " upcoming | "
                + statusCounts.get(AssignmentStatus.OVERDUE) + " overdue | "
                + statusCounts.get(AssignmentStatus.COMPLETED) + " completed | "
                + statusCounts.get(AssignmentStatus.NO_DATE) + " without date");
        for (EventAssignment assignment : assignments) {
            printAssignment(assignment, today);
        }
        return false;
    }

    /** Counts each assignment exactly once using the event state on the day of the query. */
    private EnumMap<AssignmentStatus, Integer> countStatuses(
            List<EventAssignment> assignments, LocalDate today) {
        EnumMap<AssignmentStatus, Integer> counts = new EnumMap<>(AssignmentStatus.class);
        for (AssignmentStatus status : AssignmentStatus.values()) {
            counts.put(status, 0);
        }
        for (EventAssignment assignment : assignments) {
            counts.compute(statusOf(assignment.getEvent(), today), (status, count) -> count + 1);
        }
        return counts;
    }

    /** Prints one event and the responsibility relevant to the selected member. */
    private void printAssignment(EventAssignment assignment, LocalDate today) {
        Event event = assignment.getEvent();
        String name = event.getName().isBlank() ? "Not set" : event.getName();
        LocalDate date = event.getDate();
        String role = assignment.getRole().isBlank() ? "Not set" : assignment.getRole();

        ui.print("[" + event.getTag() + "] " + name);
        ui.print("   Date: " + (date == null ? "Not set" : date));
        ui.print("   Role: " + role);
        ui.print("   Status: " + statusOf(event, today).displayName);
    }

    /**
     * Classifies an event for responsibility tracking. Completion takes precedence over
     * the date so a completed past event is not reported as overdue.
     */
    private AssignmentStatus statusOf(Event event, LocalDate today) {
        if (event.isCompleted()) {
            return AssignmentStatus.COMPLETED;
        }
        if (event.getDate() == null) {
            return AssignmentStatus.NO_DATE;
        }
        return event.getDate().isBefore(today) ? AssignmentStatus.OVERDUE : AssignmentStatus.UPCOMING;
    }
}

//@@author
