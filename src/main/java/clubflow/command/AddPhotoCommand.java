package clubflow.command;

import clubflow.ClubData;
import clubflow.UserInterface;
import clubflow.event.Event;
import clubflow.exception.ClubFlowException;
import clubflow.exception.CommandParseException;

import java.util.HashMap;

public class AddPhotoCommand extends Command{
    private ClubData clubData;

    /**
     * Creates a command.
     *
     * @param ui User Interface to print command messages in.
     */
    public AddPhotoCommand(UserInterface ui, ClubData clubData) {
        super(ui);
        this.clubData = clubData;
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
            throw new CommandParseException("Event with tag \"" + "\" does not exist.");
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
        try {
            Event e = clubData.getEvent(args.get(EventArgumentIds.TAG));
            e.addPhoto(args.get(EventArgumentIds.PATH), args.get(EventArgumentIds.NAME));
        } catch (ClubFlowException e) {
            ui.print(e.getMessage());
        }
        return false;
    }
}
