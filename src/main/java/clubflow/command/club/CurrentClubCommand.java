//@@author camimaigrot
package clubflow.command.club;

import clubflow.command.Command;

import clubflow.ClubManager;
import clubflow.UserInterface;

import java.util.HashMap;
import java.util.Objects;

/**
 * Displays the currently selected club.
 */
public class CurrentClubCommand extends Command {

    private final ClubManager clubManager;

    public CurrentClubCommand(UserInterface ui,
                              ClubManager clubManager) {
        super(ui);
        this.clubManager = Objects.requireNonNull(clubManager);
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
        ui.print("Active club: "
                + clubManager.getActiveClubId());
        return false;
    }
}
