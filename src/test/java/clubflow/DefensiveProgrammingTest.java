package clubflow;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import clubflow.command.CommandParser;
import clubflow.command.TestCommand;
import clubflow.event.Event;
import clubflow.exception.CommandParseException;
import clubflow.member.Member;
import clubflow.search.EventSearch;

//@@author Eugene3129
/**
 * Tests defensive checks at the application's public boundaries.
 */
public class DefensiveProgrammingTest {

    @Test
    void parser_invalidArgumentDeclarations_rejectsRegistration() {
        UserInterface ui = new UserInterface("", "");
        CommandParser parser = new CommandParser(ui);

        for (String id : Arrays.asList(null, "", "  ", "a/b", "UPPER", "d")) {
            TestCommand command = new TestCommand(ui) {
                @Override
                public String[] requiredArgIds() {
                    return new String[]{id};
                }
            };
            assertThrows(IllegalArgumentException.class, () -> parser.register("invalid", command));
        }
    }

    @Test
    void parser_invalidRepeatableArguments_rejectsDuplicateOrUndeclaredIds() {
        UserInterface ui = new UserInterface("", "");
        CommandParser parser = new CommandParser(ui);

        for (String[] repeatable : List.of(new String[]{"a", "a"}, new String[]{"unknown"})) {
            TestCommand command = new TestCommand(ui) {
                @Override
                public String[] repeatableArgIds() {
                    return repeatable;
                }
            };
            assertThrows(IllegalArgumentException.class, () -> parser.register("invalid", command));
        }
    }

    @Test
    void parser_unknownAliasTargetAndBlankKeyword_rejectsRegistration() {
        UserInterface ui = new UserInterface("", "");
        CommandParser parser = new CommandParser(ui);

        assertThrows(IllegalArgumentException.class, () -> parser.registerAlias("missing", "m"));
        assertThrows(IllegalArgumentException.class, () -> parser.register("  ", new TestCommand(ui)));
    }

    @Test
    void eventSearch_invalidFilters_rejectsEvenWhenClubIsEmpty() {
        EventSearch search = new EventSearch(new UserInterface("", ""), new Club());

        assertThrows(IllegalArgumentException.class, () -> search.search(Map.of("e", "  ")));
        assertThrows(IllegalArgumentException.class, () -> search.search(Map.of("unknown", "value")));
        assertThrows(IllegalArgumentException.class, () -> search.search(Map.of("s", "unknown")));
    }

    @Test
    void userInterface_invalidIndentationAndNullOutput_rejectsInvalidRequests() {
        UserInterface ui = new UserInterface("", "");

        assertThrows(IllegalArgumentException.class, () -> ui.print("message", -1));
        assertThrows(NullPointerException.class, () -> ui.print(null));
        assertThrows(NullPointerException.class, () -> ui.printWithoutPrefix(null));
    }

    @Test
    public void parser_invalidConfigurationOrInput_throwsHelpfulExceptions() {
        UserInterface ui = new UserInterface("[TEST] ", "> ");
        CommandParser parser = new CommandParser(ui);
        TestCommand command = new TestCommand(ui);
        parser.register("test", command);

        assertAll(
                () -> assertThrows(NullPointerException.class, () -> parser.register(null, command)),
                () -> assertThrows(NullPointerException.class, () -> parser.register("nullCommand", null)),
                () -> assertThrows(IllegalArgumentException.class, () -> parser.register("two words", command)),
                () -> assertThrows(IllegalArgumentException.class, () -> parser.register("TEST", command)),
                () -> assertThrows(CommandParseException.class, () -> parser.parse(null)),
                () -> assertThrows(CommandParseException.class, () -> parser.parse("   "))
        );
    }

    @Test
    public void domainObjects_invalidState_throwExceptions() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Member("", "lucy", "", List.of())),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Member("Lucy", "", "", List.of())),
                () -> assertThrows(NullPointerException.class,
                        () -> new Member("Lucy", "lucy", "", null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Event("", "Party", LocalDate.now(), null,
                                10.0, "Hall", "")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Event("PARTY", "Party", LocalDate.now(), null,
                                Double.NaN, "Hall", "")),
                () -> assertThrows(NullPointerException.class, () -> new Club().addMember(null)),
                () -> assertThrows(NullPointerException.class, () -> new Club().addEvent(null))
        );
    }

    @Test
    public void load_invalidDomainRecord_wrapsFailureAsIOException(@TempDir Path tempDir) throws IOException {
        Path invalidSave = tempDir.resolve("invalid.cf");
        Files.writeString(invalidSave, "MEMBER|Lucy|@lucy||");

        assertThrows(IOException.class, () -> new SaveLoadManager().load(invalidSave));
    }
}
