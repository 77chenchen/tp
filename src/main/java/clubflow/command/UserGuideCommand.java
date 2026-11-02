package clubflow.command;

import clubflow.UserInterface;
import clubflow.exception.UGOpenException;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Objects;

/** Opens the published user guide in the system browser and reports the outcome. */
public class UserGuideCommand extends Command{

    private static final String UG_URL = "https://ay2627s1-cs2113-w10-2.github.io/tp/UserGuide.html";

    /** Opens a URL; tests can record requests or simulate a browser I/O failure. */
    @FunctionalInterface
    interface BrowserOpener {
        void open(URI uri) throws IOException;
    }

    /** Browser operation kept separate from command validation and terminal output. */
    private final BrowserOpener browserOpener;

    /**
     * Creates a command that opens the user guide for them.
     *
     * @param ui User Interface to print command messages in.
     */
    public UserGuideCommand(UserInterface ui) {
        this(ui, uri -> Desktop.getDesktop().browse(uri));
    }

    /**
     * Creates a command with a replaceable browser operation for tests without a desktop.
     *
     * @param ui interface used to display the result
     * @param browserOpener operation used to open the guide URL
     */
    UserGuideCommand(UserInterface ui, BrowserOpener browserOpener) {
        super(ui);
        this.browserOpener = Objects.requireNonNull(browserOpener, "Browser opener must not be null");
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
            browserOpener.open(URI.create(UG_URL));
        } catch (IOException e) {
            throw new UGOpenException("An IO error occurred.");
        }
        ui.print("Opened user guide in browser.");
        return false;
    }

}
