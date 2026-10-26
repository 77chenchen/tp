//@@author camimaigrot
package clubflow.command;

import clubflow.ClubManager;
import clubflow.UserInterface;
import clubflow.exception.ClubFlowException;
import clubflow.exception.CommandParseException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

/**
 * Handles switching between clubs.
 */
public class SwitchClubCommand extends Command {

    private final ClubManager clubManager;

    public SwitchClubCommand(UserInterface ui,
                             ClubManager clubManager) {
        super(ui);
        this.clubManager = Objects.requireNonNull(clubManager);
        addSyntax("n/ClubId");
    }

    @Override
    public String[] requiredArgIds() {
        return new String[]{"n"};
    }

    @Override
    public String[] optionalArgIds() {
        return new String[]{};
    }

    @Override
    public void validateArgs(HashMap<String, String> args)
            throws CommandParseException {

        if (!args.get("n").matches("(?i)[a-z0-9_-]+")) {
            throw new CommandParseException(
                    "Use letters, digits, _ or - for a club ID.");
        }
    }

    @Override
    public boolean execute(HashMap<String, String> args)
            throws ClubFlowException {

        try {
            clubManager.switchClub(args.get("n"));

            ui.print("Active club: "
                    + clubManager.getActiveClubId());

        } catch (IOException e) {
            ui.print("Could not switch club: "
                    + e.getMessage());
        }

        return false;
    }
}