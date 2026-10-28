package clubflow.command.member;

import clubflow.command.Command;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import clubflow.Club;
import clubflow.UserInterface;
import clubflow.member.Member;

//@@author 77chenchen

/**
 * Displays all members currently stored in the club.
 */
public class ViewMemberCommand extends Command {
    private final Club club;

    /**
     * Creates a command for viewing members.
     *
     * @param ui user interface used to display member information
     * @param club shared club data containing the members to display
     */
    public ViewMemberCommand(UserInterface ui, Club club) {
        super(ui);
        this.club = Objects.requireNonNull(club, "Club data must not be null");
        addSyntax("");
    }

    @Override
    public String[] requiredArgIds() {
        return new String[]{};
    }

    @Override
    public String[] optionalArgIds() {
        return new String[]{};
    }

    @Override
    public boolean execute(HashMap<String, String> args) {
        Objects.requireNonNull(args, "Arguments must not be null");
        List<Member> members = club.getMembers();
        if (members.isEmpty()) {
            ui.print("No members found.");
            return false;
        }

        ui.print("Members:");
        for (int i = 0; i < members.size(); i++) {
            printMember(i + 1, members.get(i));
        }
        return false;
    }

    /**
     * Prints one member together with the one-based index used for deletion.
     *
     * @param index one-based member index
     * @param member member to display
     */
    private void printMember(int index, Member member) {
        String telegram = member.getTelegramHandle().isBlank()
                ? "None" : "@" + member.getTelegramHandle();
        String email = member.getEmail().isBlank() ? "None" : member.getEmail();
        String functions = member.getFunctions().isEmpty()
                ? "None" : String.join(", ", member.getFunctions());

        ui.print(index + ". " + member.getName());
        ui.print("   Telegram: " + telegram);
        ui.print("   Email: " + email);
        ui.print("   Functions: " + functions);
    }
}

//@@author
