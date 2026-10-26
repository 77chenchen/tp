package clubflow.command;

import clubflow.ClubData;
import clubflow.UserInterface;
import clubflow.exception.CommandParseException;
import clubflow.exception.PhotoLoadException;

import java.util.HashMap;

public class AddAllPhotoCommand extends Command {
    private ClubData clubData;

    /**
     * Creates an add all photos command.
     *
     * @param ui User Interface to print command messages in.
     * @param clubData Club data containing events.
     */
    public AddAllPhotoCommand(UserInterface ui, ClubData clubData) {
        super(ui);
        this.clubData = clubData;
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
        if (clubData.getEvent(args.get(EventArgumentIds.TAG)) == null) {
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

        int[] result = clubData.addPhotos(tag, path);
        ui.print("Photos from \"" + path + "\" have been added to event \"" + tag + "\". ("
                + result[0] + " additions, " + result[1] + " failures)");

        return false;
    }
}
