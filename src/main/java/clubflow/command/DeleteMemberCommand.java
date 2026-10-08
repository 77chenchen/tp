package clubflow.command;

import java.util.HashMap;
import java.util.Optional;

import clubflow.ClubData;
import clubflow.UserInterface;
import clubflow.exception.CommandParseException;
import clubflow.member.Member;

/**
 * Deletes a member selected by the one-based index shown by {@code viewMember}.
 */
public class DeleteMemberCommand extends Command {
    private final ClubData clubData;

    /**
     * Creates a command that deletes members from the supplied data store.
     *
     * @param ui user interface used to confirm and display command results
     * @param clubData shared club data from which members are deleted
     */
    public DeleteMemberCommand(UserInterface ui, ClubData clubData) {
        super(ui);
        this.clubData = clubData;
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
    public String[] flagArgIds() {
        return new String[]{MemberArgumentIds.FORCE};
    }

    @Override
    public void validateArgs(HashMap<String, String> args) throws CommandParseException {
        try {
            int index = Integer.parseInt(args.get(MemberArgumentIds.INDEX));
            if (index < 1) {
                throw new CommandParseException("Member index must be a positive number.");
            }
        } catch (NumberFormatException e) {
            throw new CommandParseException("Member index must be a positive number.");
        }
    }

    @Override
    public boolean execute(HashMap<String, String> args) {
        int index = Integer.parseInt(args.get(MemberArgumentIds.INDEX));
        Optional<Member> memberToDelete = clubData.findMemberByIndex(index);
        if (memberToDelete.isEmpty()) {
            ui.print("Member not found at index " + index + ".");
            return false;
        }

        Member member = memberToDelete.get();
        if (!args.containsKey(MemberArgumentIds.FORCE) && !confirmDeletion(member)) {
            ui.print("Deletion cancelled.");
            return false;
        }

        clubData.removeMemberByIndex(index);
        ui.print("Deleted member: " + member.getName());
        return false;
    }

    /**
     * Asks the user to confirm deletion when the force flag was not supplied.
     *
     * @param member member selected for deletion
     * @return true only when the user enters y or yes
     */
    private boolean confirmDeletion(Member member) {
        ui.print("Delete member \"" + member.getName() + "\"? Enter y to confirm.");
        String response = ui.prompt();
        return response.equalsIgnoreCase("y") || response.equalsIgnoreCase("yes");
    }
}
