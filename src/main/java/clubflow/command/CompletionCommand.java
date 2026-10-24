package clubflow.command;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import clubflow.UserInterface;
import clubflow.completion.CompletionHandler;
import clubflow.exception.ClubFlowException;
import clubflow.exception.CommandParseException;

/**
 * Dispatches mark or unmark requests to the handler for the selected item type.
 * Item-specific lookup and state changes belong to handlers registered by the application.
 */
public class CompletionCommand extends Command {
    /** Maps type prefixes to handlers accepting an identifier and the desired completion state. */
    private final Map<String, CompletionHandler> completionHandlers;

    /** True for mark and false for unmark; repeated requests set the same state rather than toggling it. */
    private final boolean completed;

    /**
     * Creates a command for setting completion on a supported item type.
     * Copies the handlers so accepted prefixes cannot change after parser registration.
     *
     * @param ui user interface shared with the registered handlers
     * @param completionHandlers handlers keyed by their lowercase type prefixes
     * @param completed true for marking completed, false for marking not completed
     * @throws IllegalArgumentException if no item types are supplied
     */
    public CompletionCommand(UserInterface ui, Map<String, CompletionHandler> completionHandlers,
                             boolean completed) {
        super(ui);
        this.completionHandlers = Map.copyOf(Objects.requireNonNull(completionHandlers,
                "Completion handlers must not be null"));
        if (this.completionHandlers.isEmpty()) {
            throw new IllegalArgumentException("At least one item type must be supplied");
        }
        this.completed = completed;
        addSyntax("e/Identifier");
    }

    @Override
    public String[] requiredArgIds() {
        return new String[]{};
    }

    @Override
    public String[] optionalArgIds() {
        // Exactly one type is required, but no individual type is mandatory.
        return completionHandlers.keySet().toArray(String[]::new);
    }

    @Override
    public void validateArgs(HashMap<String, String> args) throws CommandParseException {
        if (args.size() != 1) {
            throw new CommandParseException("Specify exactly one item type and identifier.");
        }
        Map.Entry<String, String> item = args.entrySet().iterator().next();
        if (!completionHandlers.containsKey(item.getKey())) {
            throw new CommandParseException("Unsupported item type: " + item.getKey() + ".");
        }
        if (item.getValue().isBlank()) {
            throw new CommandParseException("An identifier must be provided after " + item.getKey() + "/.");
        }
    }

    @Override
    public boolean execute(HashMap<String, String> args) throws ClubFlowException {
        Objects.requireNonNull(args, "Arguments must not be null");
        assert args.size() == 1 : "Validated arguments must select exactly one item type";
        Map.Entry<String, String> item = args.entrySet().iterator().next();
        assert completionHandlers.containsKey(item.getKey()) : "Validated arguments must select a supported item type";
        completionHandlers.get(item.getKey()).setCompleted(item.getValue(), completed);
        return false;
    }
}
