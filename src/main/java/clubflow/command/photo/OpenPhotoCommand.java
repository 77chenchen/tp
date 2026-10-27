package clubflow.command.photo;

import clubflow.command.Command;
import clubflow.command.EventArgumentIds;

import clubflow.Club;
import clubflow.UserInterface;
import clubflow.event.Event;
import clubflow.exception.CommandParseException;
import clubflow.exception.PhotoOpenException;

import java.util.HashMap;

public class OpenPhotoCommand extends Command{

    private Club club;

    /**
     * Creates an open photo command.
     *
     * @param ui User Interface to print command messages in.
     */
    public OpenPhotoCommand(UserInterface ui, Club club) {
        super(ui);
        this.club = club;
        addSyntax("e/Tag");
        addSyntax("e/Tag n/Name");
    }

    @Override
    public String[] requiredArgIds() {
        return new String[] {EventArgumentIds.TAG};
    }

    @Override
    public String[] optionalArgIds() {
        return new String[] {EventArgumentIds.NAME};
    }

    @Override
    public void validateArgs(HashMap<String, String> args) throws CommandParseException {
        if (club.findEventByTag(args.get(EventArgumentIds.TAG)).isEmpty()){
            throw new CommandParseException("Event with tag \"" + args.get(EventArgumentIds.TAG)
                    + "\" does not exist.");
        }
    }

    @Override
    public boolean execute(HashMap<String, String> args) throws PhotoOpenException {
        String tag = args.get(EventArgumentIds.TAG);
        Event e = club.findEventByTag(tag).orElseThrow();

        if (!args.containsKey(EventArgumentIds.NAME)){
            if (!e.openPhotoDir()){
                throw new PhotoOpenException("Cannot open the photo directory now. Please try again.");
            }
            ui.print("Opened directory of photos in event \"" + tag + "\".");
            return false;
        }

        String name = args.get(EventArgumentIds.NAME);

        if (!e.openPhoto(name)){
            throw new PhotoOpenException("No photo has name \"" + name + "\" in event \"" + tag + "\".");
        }

        ui.print("Opened photo \"" + name + "\" in event \"" + tag + "\".");
        return false;
    }
}
