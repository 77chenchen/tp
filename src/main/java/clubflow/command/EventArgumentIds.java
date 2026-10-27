package clubflow.command;

//@@author Eugene3129
/**
 * Defines argument IDs shared by event and photo commands.
 */
public final class EventArgumentIds {
    public static final String TAG = "e";
    public static final String NAME = "n";
    public static final String DATE = "d";
    public static final String DEADLINE = "by";
    public static final String BUDGET = "b";
    public static final String VENUE = "v";
    public static final String LOGISTICS = "l";
    public static final String ALL = "all";
    public static final String PATH = "p";

    private EventArgumentIds() {
        // Prevent instantiation because this class only contains constants.
    }
}
