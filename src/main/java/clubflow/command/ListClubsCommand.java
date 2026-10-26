//@@author camimaigrot
package clubflow.command;

import clubflow.ClubManager;
import clubflow.UserInterface;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Displays all existing clubs.
 */
public class ListClubsCommand extends Command {

    private final ClubManager clubManager;

    public ListClubsCommand(UserInterface ui,
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

        try {
            List<String> clubs = clubManager.listClubs();

            if (clubs.isEmpty()) {
                ui.print("No saved clubs found.");
                return false;
            }

            for (String clubId : clubs) {
                String marker = clubId.equalsIgnoreCase(
                        clubManager.getActiveClubId())
                        ? " (active)" : "";

                ui.print(clubId + marker);
            }

        } catch (IOException e) {
            ui.print("Could not list clubs: "
                    + e.getMessage());
        }

        return false;
    }
}