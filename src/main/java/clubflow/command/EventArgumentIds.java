package clubflow.command;

/**
 * Defines the argument IDs shared by event commands.
 */
final class EventArgumentIds {
    static final String TAG = "e";
    static final String NAME = "n";
    static final String DATE = "d";
    static final String DEADLINE = "by";
    static final String BUDGET = "b";
    static final String VENUE = "v";
    static final String LOGISTICS = "l";
    static final String ALL = "all";

    private EventArgumentIds() {
        // Prevent instantiation because this class only contains constants.
    }
}
