package clubflow.command;

import clubflow.ClubData;
import clubflow.UserInterface;
import clubflow.event.Event;
import clubflow.exception.ClubFlowException;
import clubflow.exception.CommandParseException;
import clubflow.photo.Photo;

import java.util.HashMap;

public class AddPhotoCommand extends Command{
    private ClubData clubData;

    /**
     * Creates an add photo command.
     *
     * @param ui User Interface to print command messages in.
     */
    public AddPhotoCommand(UserInterface ui, ClubData clubData) {
        super(ui);
        this.clubData = clubData;
        addSyntax("e/Tag n/Name p/Path");
    }

    @Override
    public String[] requiredArgIds() {
        return new String[] {EventArgumentIds.NAME, EventArgumentIds.TAG, EventArgumentIds.PATH};
    }

    @Override
    public String[] optionalArgIds() {
        return new String[0];
    }

    @Override
    public void validateArgs(HashMap<String, String> args) throws CommandParseException {
        if (clubData.getEvent(args.get(EventArgumentIds.TAG)) == null){
            throw new CommandParseException("Event with tag \"" + args.get(EventArgumentIds.TAG)
                    + "\" does not exist.");
        }

        if (args.get(EventArgumentIds.PATH).isBlank()){
            throw new CommandParseException("Photo source path cannot be blank.");
        }

        if (args.get(EventArgumentIds.NAME).isBlank()){
            throw new CommandParseException("Photo name cannot be blank.");
        }
    }

    @Override
    public boolean execute(HashMap<String, String> args) {
        String tag = args.get(EventArgumentIds.TAG);
        String name = args.get(EventArgumentIds.NAME);
        String path = args.get(EventArgumentIds.PATH);

        Photo p = null;
        try {
            Event e = clubData.getEvent(tag);
            p = e.addPhoto(path, name);
        } catch (ClubFlowException e) {
            ui.print(e.getMessage());
            return false;
        }

        ui.print("Photo \"" + name + "\" has been added to event \"" + tag
                + "\". (" + path + " -> " + p.getPath() + ")");
        return false;
    }
}
