package clubflow.command.photo;

import clubflow.Club;
import clubflow.UserInterface;
import clubflow.command.Command;
import clubflow.command.EventArgumentIds;
import clubflow.exception.CommandParseException;
import clubflow.exception.PhotoRenameException;

import java.util.HashMap;

public class RenamePhotoCommand extends Command{

    private Club club;

    /**
     * Creates a rename photo command.
     *
     * @param ui User Interface to print command messages in.
     */
    public RenamePhotoCommand(UserInterface ui, Club club) {
        super(ui);
        this.club = club;
        addSyntax("e/Tag n/OldName new/NewName");
    }

    @Override
    public String[] requiredArgIds() {
        return new String[] {EventArgumentIds.TAG, EventArgumentIds.NAME, "new"};
    }

    @Override
    public String[] optionalArgIds() {
        return new String[] {};
    }

    @Override
    public void validateArgs(HashMap<String, String> args) throws CommandParseException {
        if (club.findEventByTag(args.get(EventArgumentIds.TAG)).isEmpty()){
            throw new CommandParseException("Event with tag \"" + args.get(EventArgumentIds.TAG)
                    + "\" does not exist.");
        }
    }

    @Override
    public boolean execute(HashMap<String, String> args) throws PhotoRenameException {
        String tag = args.get(EventArgumentIds.TAG);
        String oldName = args.get(EventArgumentIds.NAME);
        String newName = args.get("new");

        if (!club.renamePhoto(tag, oldName, newName)){
            throw new PhotoRenameException("No photo has name \"" + oldName + "\" in event \"" + tag + "\".");
        }

        ui.print("Renamed photo \"" + oldName + "\" to \"" + newName + "\" in event \"" + tag + "\".");
        return false;
    }
}
