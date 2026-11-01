package clubflow.command;

import clubflow.UserInterface;
import clubflow.exception.UGOpenException;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;

public class UserGuideCommand extends Command{

    private static final String UG_URL = "https://ay2627s1-cs2113-w10-2.github.io/tp/UserGuide.html";

    /**
     * Creates a command that opens the user guide for them.
     *
     * @param ui User Interface to print command messages in.
     */
    public UserGuideCommand(UserInterface ui) {
        super(ui);
        addSyntax("");
    }

    @Override
    public String[] requiredArgIds() {
        return new String[0];
    }

    @Override
    public String[] optionalArgIds() {
        return new String[0];
    }

    @Override
    public boolean execute(HashMap<String, String> args) throws UGOpenException {
        try {
            openUrl(UG_URL);
        } catch (IOException e) {
            throw new UGOpenException("An IO error occurred.");
        }
        ui.print("Opened user guide in browser.");
        return false;
    }

    private void openUrl(String url) throws IOException {
        Desktop.getDesktop().browse(URI.create(url));
    }
}
