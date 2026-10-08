package clubflow.command;

import java.util.HashMap;
import java.util.List;

import clubflow.ClubData;
import clubflow.UserInterface;
import clubflow.exception.CommandParseException;
import clubflow.member.Member;

/**
 * Creates a club member and stores the member in the shared club data.
 */
public class CreateMemberCommand extends Command {
    private final ClubData clubData;

    /**
     * Creates a command that adds members to the supplied data store.
     *
     * @param ui user interface used to display command results
     * @param clubData shared club data in which members are stored
     */
    public CreateMemberCommand(UserInterface ui, ClubData clubData) {
        super(ui);
        this.clubData = clubData;
    }

    @Override
    public String[] requiredArgIds() {
        return new String[]{MemberArgumentIds.NAME};
    }

    @Override
    public String[] optionalArgIds() {
        return new String[]{
            MemberArgumentIds.TELEGRAM,
            MemberArgumentIds.EMAIL,
            MemberArgumentIds.FUNCTION
        };
    }

    @Override
    public String[] repeatableArgIds() {
        return new String[]{MemberArgumentIds.FUNCTION};
    }

    @Override
    public void validateArgs(HashMap<String, String> args) throws CommandParseException {
        if (args.get(MemberArgumentIds.NAME).isBlank()) {
            throw new CommandParseException("A member name must be provided after n/.");
        }

        String telegram = args.getOrDefault(MemberArgumentIds.TELEGRAM, "");
        String email = args.getOrDefault(MemberArgumentIds.EMAIL, "");
        if (telegram.isBlank() && email.isBlank()) {
            throw new CommandParseException("Provide at least one contact using t/ or e/.");
        }
        if (telegram.startsWith("@")) {
            throw new CommandParseException("Enter the Telegram handle without @.");
        }

        if (getArgumentValues(args, MemberArgumentIds.FUNCTION).stream().anyMatch(String::isBlank)) {
            throw new CommandParseException("A function must be provided after f/.");
        }
    }

    @Override
    public boolean execute(HashMap<String, String> args) {
        List<String> functions = getArgumentValues(args, MemberArgumentIds.FUNCTION);
        Member member = new Member(
                args.get(MemberArgumentIds.NAME),
                args.getOrDefault(MemberArgumentIds.TELEGRAM, ""),
                args.getOrDefault(MemberArgumentIds.EMAIL, ""),
                functions
        );
        clubData.addMember(member);
        ui.print("Created member: " + member.getName());
        return false;
    }
}
