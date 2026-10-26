package clubflow.command;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import clubflow.Club;
import clubflow.UserInterface;
import clubflow.exception.CommandParseException;
import clubflow.member.Member;

//@@author 77chenchen

/**
 * Replaces one member with a validated copy containing the requested changes.
 */
public class EditMemberCommand extends Command {
    private final Club club;

    /** Creates a member editing command that uses the shared club data. */
    public EditMemberCommand(UserInterface ui, Club club) {
        super(ui);
        this.club = Objects.requireNonNull(club, "Club data must not be null");
        addSyntax("i/Index [n/Name] [t/TeleHandle] [e/Email] [f/Function]...");
    }

    @Override
    public String[] requiredArgIds() {
        return new String[]{MemberArgumentIds.INDEX};
    }

    @Override
    public String[] optionalArgIds() {
        return new String[]{MemberArgumentIds.NAME, MemberArgumentIds.TELEGRAM,
            MemberArgumentIds.EMAIL, MemberArgumentIds.FUNCTION};
    }

    @Override
    public String[] repeatableArgIds() {
        return new String[]{MemberArgumentIds.FUNCTION};
    }

    /**
     * Validates the index and the complete replacement before changing stored data.
     * An empty t/ or e/ clears that contact; a single empty f/ clears all functions.
     */
    @Override
    public void validateArgs(HashMap<String, String> args) throws CommandParseException {
        int index;
        try {
            index = Integer.parseInt(args.get(MemberArgumentIds.INDEX));
        } catch (NumberFormatException e) {
            throw new CommandParseException("Member index must be a positive number.");
        }
        if (index < 1) {
            throw new CommandParseException("Member index must be a positive number.");
        }
        if (args.size() == 1) {
            throw new CommandParseException("Provide at least one detail to edit.");
        }

        List<String> functions = getArgumentValues(args, MemberArgumentIds.FUNCTION);
        if (functions.size() > 1 && functions.stream().anyMatch(String::isBlank)) {
            throw new CommandParseException("A blank f/ can only be used alone to clear functions.");
        }

        // A missing member is reported by execute; only an existing one can be fully validated here.
        Member previous = club.findMemberByIndex(index).orElse(null);
        if (previous != null) {
            try {
                buildReplacement(previous, args);
            } catch (IllegalArgumentException e) {
                throw new CommandParseException(e.getMessage());
            }
        }
    }

    @Override
    public boolean execute(HashMap<String, String> args) {
        Objects.requireNonNull(args, "Arguments must not be null");
        assert args.containsKey(MemberArgumentIds.INDEX) : "Validated arguments must include a member index";
        int index = Integer.parseInt(args.get(MemberArgumentIds.INDEX));
        Member previous = club.findMemberByIndex(index).orElse(null);
        if (previous == null) {
            ui.print("Member not found at index " + index + ".");
            return false;
        }

        Member replacement = buildReplacement(previous, args);
        boolean replaced = club.replaceMemberByIndex(index, replacement).isPresent();
        assert replaced : "An existing member must be replaceable";
        ui.print("Updated member at index " + index + ": " + replacement.getName());
        return false;
    }

    /** Builds a replacement without changing the stored member or its assignments. */
    private Member buildReplacement(Member previous, HashMap<String, String> args) {
        List<String> functions = args.containsKey(MemberArgumentIds.FUNCTION)
                ? getArgumentValues(args, MemberArgumentIds.FUNCTION) : previous.getFunctions();
        if (functions.size() == 1 && functions.getFirst().isBlank()) {
            functions = List.of();
        }
        return new Member(
                args.getOrDefault(MemberArgumentIds.NAME, previous.getName()),
                args.getOrDefault(MemberArgumentIds.TELEGRAM, previous.getTelegramHandle()),
                args.getOrDefault(MemberArgumentIds.EMAIL, previous.getEmail()),
                functions);
    }
}

//@@author
