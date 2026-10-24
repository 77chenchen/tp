package clubflow.command;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import clubflow.UserInterface;
import clubflow.exception.CommandParseException;

/**
 * Validates a search request and delegates it to the handler for the selected type.
 */
public class FindCommand extends Command {
    /** Maps argument prefixes to search handlers; each handler searches and displays its results. */
    private final Map<String, Consumer<String>> searchHandlers;

    /**
     * Creates a find command with the search types supported by the application.
     * The map is copied so accepted prefixes cannot change after parser registration.
     *
     * @param ui user interface used to display matches
     * @param searchHandlers handlers keyed by their lowercase argument prefixes
     * @throws IllegalArgumentException if no search types are supplied
     */
    public FindCommand(UserInterface ui, Map<String, Consumer<String>> searchHandlers) {
        super(ui);
        this.searchHandlers = Map.copyOf(Objects.requireNonNull(searchHandlers, "Search handlers must not be null"));
        if (this.searchHandlers.isEmpty()) {
            throw new IllegalArgumentException("At least one search type must be supplied");
        }
        addSyntax("e/Keyword");
    }

    @Override
    public String[] requiredArgIds() {
        return new String[]{};
    }

    @Override
    public String[] optionalArgIds() {
        // No individual type is mandatory; validation requires exactly one of the supported types.
        return searchHandlers.keySet().toArray(String[]::new);
    }

    @Override
    public void validateArgs(HashMap<String, String> args) throws CommandParseException {
        if (args.size() != 1) {
            throw new CommandParseException("Specify exactly one search type and keyword.");
        }
        Map.Entry<String, String> search = args.entrySet().iterator().next();
        if (!searchHandlers.containsKey(search.getKey())) {
            throw new CommandParseException("Unsupported search type: " + search.getKey() + ".");
        }
        if (search.getValue().isBlank()) {
            throw new CommandParseException("A search keyword must be provided after " + search.getKey() + "/.");
        }
    }

    @Override
    public boolean execute(HashMap<String, String> args) {
        Objects.requireNonNull(args, "Arguments must not be null");
        assert args.size() == 1 : "Validated arguments must select exactly one search type";
        Map.Entry<String, String> search = args.entrySet().iterator().next();
        assert searchHandlers.containsKey(search.getKey()) : "Validated arguments must select a supported search type";
        searchHandlers.get(search.getKey()).accept(search.getValue().strip());
        return false;
    }
}
