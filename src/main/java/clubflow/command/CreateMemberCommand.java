package clubflow.command;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

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
        this.clubData = Objects.requireNonNull(clubData, "Club data must not be null");
        addSyntax("n/Name [t/TeleHandle] [e/Email] [f/Function]...");
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

    /**
     * Validates member details and rejects characters reserved by the save file format.
     * Commas are allowed in ordinary fields but not in function names.
     *
     * @param args parsed member arguments
     * @throws CommandParseException if a required value or file-format constraint is invalid
     */
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

        for (String value : args.values()) {
            if (value.contains("|")) {
                throw new CommandParseException("Fields cannot contain |.");
            }
        }
        for (String function : getArgumentValues(args, MemberArgumentIds.FUNCTION)) {
            if (function.contains(",")) {
                throw new CommandParseException("Function names cannot contain commas.");
            }
        }
    }

    @Override
    public boolean execute(HashMap<String, String> args) {
        Objects.requireNonNull(args, "Arguments must not be null");
        assert args.containsKey(MemberArgumentIds.NAME) : "Validated arguments must contain a member name";
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
