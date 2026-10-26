package clubflow.command;

import clubflow.Club;
import clubflow.UserInterface;
import clubflow.exception.CommandParseException;
import clubflow.exception.PhotoLoadException;

import java.util.HashMap;

public class AddAllPhotoCommand extends Command {
    private Club club;

    /**
     * Creates an add all photos command.
     *
     * @param ui User Interface to print command messages in.
     * @param club Club data containing events.
     */
    public AddAllPhotoCommand(UserInterface ui, Club club) {
        super(ui);
        this.club = club;
        addSyntax("e/Tag p/Path");
    }

    @Override
    public String[] requiredArgIds() {
        return new String[] {EventArgumentIds.TAG, EventArgumentIds.PATH};
    }

    @Override
    public String[] optionalArgIds() {
        return new String[0];
    }

    @Override
    public void validateArgs(HashMap<String, String> args) throws CommandParseException {
        if (club.findEventByTag(args.get(EventArgumentIds.TAG)).isEmpty()) {
            throw new CommandParseException("Event with tag \"" + args.get(EventArgumentIds.TAG)
                    + "\" does not exist.");
        }

        if (args.get(EventArgumentIds.PATH).isBlank()) {
            throw new CommandParseException("Photo source directory path cannot be blank.");
        }
    }

    @Override
    public boolean execute(HashMap<String, String> args) throws PhotoLoadException {
        String tag = args.get(EventArgumentIds.TAG);
        String path = args.get(EventArgumentIds.PATH);

        int[] result = club.addPhotos(tag, path);
        ui.print("Photos from \"" + path + "\" have been added to event \"" + tag + "\". ("
                + result[0] + " additions, " + result[1] + " failures)");

        return false;
    }
}
