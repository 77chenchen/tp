//@@author camimaigrot
package clubflow.command.club;

import clubflow.command.Command;

import clubflow.ClubManager;
import clubflow.UserInterface;
import clubflow.exception.ClubFlowException;
import clubflow.exception.CommandParseException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

/**
 * Handles the createClub command.
 */
public class CreateClubCommand extends Command {

    private final ClubManager clubManager;

    public CreateClubCommand(UserInterface ui,
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

        String clubName = args.get("n");

        if (!clubName.matches("(?i)[a-z0-9_-]+")) {
            throw new CommandParseException(
                    "Use letters, digits, _ or - for a club ID.");
        }
    }

    @Override
    public boolean execute(HashMap<String, String> args)
            throws ClubFlowException {

        String clubName = args.get("n");

        try {
            clubManager.createClub(clubName);
            ui.print("Created club: " + clubName);
        } catch (IOException e) {
            ui.print("Could not create club: "
                    + e.getMessage());
        }

        return false;
    }
}
