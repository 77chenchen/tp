package clubflow.command.member;

import clubflow.command.Command;

import java.util.HashMap;
import java.util.Objects;

import clubflow.Club;
import clubflow.UserInterface;
import clubflow.exception.CommandParseException;
import clubflow.member.Member;

//@@author 77chenchen

/**
 * Removes a member's event assignment while preserving both records.
 */
public class UnassignMemberCommand extends Command {
    private static final String EVENT_TAG = "e";
    private final Club club;

    /** Creates an unassignment command using the shared club data. */
    public UnassignMemberCommand(UserInterface ui, Club club) {
        super(ui);
        this.club = Objects.requireNonNull(club, "Club data must not be null");
        addSyntax("e/Tag i/Index");
    }

    @Override
    public String[] requiredArgIds() {
        return new String[]{EVENT_TAG, MemberArgumentIds.INDEX};
    }

    @Override
    public String[] optionalArgIds() {
        return new String[]{};
    }

    @Override
    public void validateArgs(HashMap<String, String> args) throws CommandParseException {
        if (args.get(EVENT_TAG).isBlank()) {
            throw new CommandParseException("An event tag must be provided after e/.");
        }
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
        assert args.containsKey(EVENT_TAG) && args.containsKey(MemberArgumentIds.INDEX)
                : "Validated arguments must identify an event and a member";
        String eventTag = args.get(EVENT_TAG);
        if (club.findEventByTag(eventTag).isEmpty()) {
            ui.print("Event not found: " + eventTag);
            return false;
        }
        int memberIndex = Integer.parseInt(args.get(MemberArgumentIds.INDEX));
        Member member = club.findMemberByIndex(memberIndex).orElse(null);
        if (member == null) {
            ui.print("Member not found at index " + memberIndex + ".");
            return false;
        }

        if (club.unassignMemberFromEvent(eventTag, member)) {
            ui.print("Unassigned member " + member.getName() + " from event " + eventTag + ".");
        } else {
            ui.print("Member is not assigned to event " + eventTag + ".");
        }
        return false;
    }
}

//@@author
