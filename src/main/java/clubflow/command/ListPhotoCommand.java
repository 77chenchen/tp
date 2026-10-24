package clubflow.command;

import clubflow.ClubData;
import clubflow.UserInterface;
import clubflow.event.Event;
import clubflow.exception.CommandParseException;

import java.util.HashMap;

public class ListPhotoCommand extends Command{
    private ClubData clubData;

    /**
     * Creates a list photo command.
     *
     * @param ui User Interface to print command messages in.
     */
    public ListPhotoCommand(UserInterface ui, ClubData clubData) {
        super(ui);
        this.clubData = clubData;
        addSyntax("");
        addSyntax("e/Tag");
    }

    @Override
    public String[] requiredArgIds() {
        return new String[0];
    }

    @Override
    public String[] optionalArgIds() {
        return new String[] {EventArgumentIds.TAG};
    }

    @Override
    public void validateArgs(HashMap<String, String> args) throws CommandParseException {
        if (!args.containsKey(EventArgumentIds.TAG)) {
            return;
        }
        if (clubData.getEvent(args.get(EventArgumentIds.TAG)) == null){
            throw new CommandParseException("Event with tag \"" + args.get(EventArgumentIds.TAG)
                    + "\" does not exist.");
        }
    }

    @Override
    public boolean execute(HashMap<String, String> args) {
        ui.print("The photo name list is as following:");
        if (args.containsKey(EventArgumentIds.TAG)){
            Event e = clubData.getEvent(args.get(EventArgumentIds.TAG));
            printEventPhotoNames(e);
            return false;
        }

        for (Event e : clubData.getEvents()){
            printEventPhotoNames(e);
        }
        return false;
    }

    private void printEventPhotoNames(Event e){
        ui.print("Photos of " + e.getTag() + ": ");
        int index = 0;
        for (String name : e.getPhotoNames()){
            index++;
            ui.print(index + ". " + name, 1);
        }
    }

}
