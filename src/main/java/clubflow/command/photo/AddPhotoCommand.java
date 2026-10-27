package clubflow.command.photo;

import clubflow.command.Command;
import clubflow.command.EventArgumentIds;

import clubflow.Club;
import clubflow.UserInterface;
import clubflow.exception.CommandParseException;
import clubflow.exception.PhotoLoadException;
import clubflow.photo.Photo;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.HashMap;

public class AddPhotoCommand extends Command{
    private Club club;

    /**
     * Creates an add photo command.
     *
     * @param ui User Interface to print command messages in.
     */
    public AddPhotoCommand(UserInterface ui, Club club) {
        super(ui);
        this.club = club;
        addSyntax("e/Tag [n/Name] p/Path");

    }

    @Override
    public String[] requiredArgIds() {
        return new String[] {EventArgumentIds.TAG, EventArgumentIds.PATH};
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

        String path = args.get(EventArgumentIds.PATH);

        if (path.isBlank()) {
            throw new CommandParseException("Photo source path cannot be blank.");
        }

        if (args.containsKey(EventArgumentIds.NAME)) {
            if (args.get(EventArgumentIds.NAME).isBlank()) {
                throw new CommandParseException("Photo name cannot be blank.");
            }
        } else {
            try {
                Path filePath = Path.of(path);
                if (filePath.getFileName() == null || filePath.getFileName().toString().isBlank()) {
                    throw new CommandParseException("Invalid photo source path: " + path);
                }
            } catch (InvalidPathException e) {
                throw new CommandParseException("Invalid photo source path: " + path);
            }
        }
    }

    @Override
    public boolean execute(HashMap<String, String> args) throws PhotoLoadException {
        String tag = args.get(EventArgumentIds.TAG);
        String name = args.get(EventArgumentIds.NAME);
        String path = args.get(EventArgumentIds.PATH);

        if (name == null) {
            String filename = Path.of(path).getFileName().toString();
            int dotIndex = filename.lastIndexOf('.');
            name = dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
        }

        Photo p = club.addPhoto(tag, path, name);

        ui.print("Photo \"" + name + "\" has been added to event \"" + tag
                + "\". (" + path + " -> " + p.getPath() + ")");

        return false;
    }
}
