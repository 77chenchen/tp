package clubflow.command;

import clubflow.Club;
import clubflow.UserInterface;
import clubflow.event.Event;
import clubflow.exception.CommandParseException;
import clubflow.exception.PhotoDeleteException;

import java.util.HashMap;

public class DeletePhotoCommand extends Command{

    private Club club;

    /**
     * Creates a delete photo command.
     *
     * @param ui User Interface to print command messages in.
     */
    public DeletePhotoCommand(UserInterface ui, Club club) {
        super(ui);
        this.club = club;
        addSyntax("e/Tag all");
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
    public String[] flagArgIds() {
        return new String[]{EventArgumentIds.ALL};
    }

    @Override
    public void validateArgs(HashMap<String, String> args) throws CommandParseException {
        if (club.getEvent(args.get(EventArgumentIds.TAG)) == null){
            throw new CommandParseException("Event with tag \"" + args.get(EventArgumentIds.TAG)
                    + "\" does not exist.");
        }

        if (!args.containsKey(EventArgumentIds.NAME) && !args.containsKey(EventArgumentIds.ALL)){
            throw new CommandParseException("No name of photo to delete provided.");
        }

        if (args.containsKey(EventArgumentIds.NAME) && args.containsKey(EventArgumentIds.ALL)){
            throw new CommandParseException("Provided a name of photo to delete AND " +
                    "instruction to delete all photos at the same time.");
        }
    }

    @Override
    public boolean execute(HashMap<String, String> args) {
        String tag = args.get(EventArgumentIds.TAG);
        Event e = club.getEvent(tag);

        if (args.containsKey(EventArgumentIds.ALL)) {
            int before = e.getPhotoNames().size();
            int errors = e.deleteAllPhotos();

            if (e.getPhotoNames().size() < before) {
                club.markUnsaved();
            }

            ui.print("Deleted all photos in event \"" + tag + "\". (" + errors + " failures)");
            return false;
        }

        String name = args.get(EventArgumentIds.NAME);
        try {
            if (!e.deletePhoto(name)){
                ui.print("No photo has name \"" + name + "\" in event \"" + tag + "\".");
                return false;
            }
        } catch (PhotoDeleteException ex) {
            ui.print(ex.getMessage());
            return false;
        }
        club.markUnsaved();
        ui.print("Deleted photo \"" + name + "\" in event \"" + tag + "\".");
        return false;
    }
}
