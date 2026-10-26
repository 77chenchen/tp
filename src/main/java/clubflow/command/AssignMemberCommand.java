package clubflow.command;

import java.util.HashMap;
import java.util.Objects;

import clubflow.ClubData;
import clubflow.UserInterface;
import clubflow.exception.CommandParseException;
import clubflow.member.Member;

//@@author 77chenchen

/**
 * Assigns an existing club member to an existing event with an optional role.
 */
public class AssignMemberCommand extends Command {
    private static final String EVENT_TAG = "e";
    private static final String ROLE = "r";
    private final ClubData clubData;

    /** Creates an assignment command using the shared club data. */
    public AssignMemberCommand(UserInterface ui, ClubData clubData) {
        super(ui);
        this.clubData = Objects.requireNonNull(clubData, "Club data must not be null");
        addSyntax("e/Tag i/Index [r/Role]");
    }

    @Override
    public String[] requiredArgIds() {
        return new String[]{EVENT_TAG, MemberArgumentIds.INDEX};
    }

    @Override
    public String[] optionalArgIds() {
        return new String[]{ROLE};
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
        String role = args.getOrDefault(ROLE, "");
        if (role.contains("|") || role.contains("\n") || role.contains("\r")) {
            throw new CommandParseException("Role must not contain | or a line break.");
        }
    }

    @Override
    public boolean execute(HashMap<String, String> args) {
        Objects.requireNonNull(args, "Arguments must not be null");
        assert args.containsKey(EVENT_TAG) && args.containsKey(MemberArgumentIds.INDEX)
                : "Validated arguments must identify an event and a member";
        String eventTag = args.get(EVENT_TAG);
        if (clubData.findEventByTag(eventTag).isEmpty()) {
            ui.print("Event not found: " + eventTag);
            return false;
        }
        int memberIndex = Integer.parseInt(args.get(MemberArgumentIds.INDEX));
        Member member = clubData.findMemberByIndex(memberIndex).orElse(null);
        if (member == null) {
            ui.print("Member not found at index " + memberIndex + ".");
            return false;
        }

        if (clubData.assignMemberToEvent(eventTag, member, args.getOrDefault(ROLE, ""))) {
            ui.print("Assigned member " + member.getName() + " to event " + eventTag + ".");
        } else {
            ui.print("Member is already assigned to event " + eventTag + ".");
        }
        return false;
    }
}

//@@author
