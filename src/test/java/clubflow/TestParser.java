package clubflow;

import clubflow.command.TestCommand;
import clubflow.exception.CommandParseException;
import org.junit.jupiter.api.Test;
import clubflow.command.CommandParser;

import static org.junit.jupiter.api.Assertions.*;

public class TestParser {
    @Test
    void testCommandParser() {
        UserInterface ui = new UserInterface("[TEST] ", "> ");
        CommandParser parser = new CommandParser(ui);
        parser.register("test", new TestCommand(ui));
        assertAll(
                // Valid inputs
                () -> assertDoesNotThrow(() -> parser.parse("test a/1 b/2 c/3")),
                () -> assertDoesNotThrow(() -> parser.parse("test a/1 b/2 c/3 d/4")),
                () -> assertDoesNotThrow(() -> parser.parse("test a/1 b/2 c/3 e/5")),
                () -> assertDoesNotThrow(() -> parser.parse("test a/1 b/2 c/3 d/4 e/5")),
                () -> assertDoesNotThrow(() -> parser.parse("test e/5 d/4 c/3 b/2 a/1")),
                () -> assertDoesNotThrow(() -> parser.parse("TEST A/1 B/2 C/3 D/4 E/5")),
                () -> assertDoesNotThrow(() -> parser.parse("test a/\"hello world\" b/2 c/3")),
                () -> assertDoesNotThrow(() -> parser.parse("test a/1 b/\"hello world\" c/3")),
                () -> assertDoesNotThrow(() -> parser.parse("test a/1 b/2 c/\"hello world\"")),
                () -> assertDoesNotThrow(() -> parser.parse("test a/1 b/2 c/3 d/\"hello world\"")),
                () -> assertDoesNotThrow(() -> parser.parse("test a/1 b/2 c/3 e/\"hello world\"")),
                () -> assertDoesNotThrow(() -> parser.parse("test a/\"\" b/2 c/3")),
                () -> assertDoesNotThrow(() -> parser.parse("test a/1 b/2 c/3 d/\"\" e/\"\"")),

                // Invalid inputs
                () -> assertThrows(CommandParseException.class, () -> parser.parse("")),
                () -> assertThrows(CommandParseException.class, () -> parser.parse("unknown a/1 b/2 c/3")),
                () -> assertThrows(CommandParseException.class, () -> parser.parse("test")),
                () -> assertThrows(CommandParseException.class, () -> parser.parse("test a/1")),
                () -> assertThrows(CommandParseException.class, () -> parser.parse("test a/1 b/2")),
                () -> assertThrows(CommandParseException.class, () -> parser.parse("test a/1 c/3")),
                () -> assertThrows(CommandParseException.class, () -> parser.parse("test b/2 c/3")),
                () -> assertThrows(CommandParseException.class, () -> parser.parse("test d/4 e/5")),
                () -> assertThrows(CommandParseException.class, () -> parser.parse("test a/1 b/2 d/4 e/5")),
                () -> assertThrows(CommandParseException.class, () -> parser.parse("test a/1 c/3 d/4 e/5")),
                () -> assertThrows(CommandParseException.class, () -> parser.parse("test b/2 c/3 d/4 e/5")),
                () -> assertThrows(CommandParseException.class, () -> parser.parse("test a/1 b/2 c/3 f/6")),
                () -> assertThrows(CommandParseException.class, () -> parser.parse("test a/1 b/2 c/3 x/7")),
                () -> assertThrows(CommandParseException.class, () -> parser.parse("test a/1 b/2 c/3 z/8")),
                () -> assertThrows(CommandParseException.class, () -> parser.parse("test a/1 b/2 c/3 a/4")),
                () -> assertThrows(CommandParseException.class, () -> parser.parse("test a/1 b/2 c/3 d/4 d/5")),
                () -> assertThrows(CommandParseException.class, () -> parser.parse("test a/1 b/2 c/3 e/4 e/5")),
                () -> assertThrows(CommandParseException.class, () -> parser.parse("test a/1 b/2 c/3 D/4 d/5")),
                () -> assertThrows(CommandParseException.class, () -> parser.parse("test a/1 b/2 c/3 A/4")),
                () -> assertThrows(CommandParseException.class, () -> parser.parse("test a/1 b/2 c/3 f/6 d/4")),
                () -> assertThrows(CommandParseException.class, () -> parser.parse("test a/1 b/2 c/3 e/5 f/6")),
                () -> assertThrows(CommandParseException.class, () -> parser.parse("test a/1 b/2 c/3 d/4 e/5 f/6")),
                () -> assertThrows(CommandParseException.class, () -> parser.parse("test a/1 b/2 c/3 d")),
                () -> assertThrows(CommandParseException.class, () -> parser.parse("test a/1 b/2 c/3 /value")),
                () -> assertThrows(CommandParseException.class, () -> parser.parse("test a/\"hello world b/2 c/3")),
                () -> assertThrows(CommandParseException.class, () -> parser.parse("test a/hello\"world b/2 c/3")),
                () -> assertThrows(CommandParseException.class, () -> parser.parse("test a/\"hello\"world b/2 c/3")),
                () -> assertThrows(CommandParseException.class, () -> parser.parse("test a/\"hello world\"garbage b/2 c/3")),
                () -> assertThrows(CommandParseException.class, () -> parser.parse("test a/\"hello\" \"world\" b/2 c/3"))
        );
    }

}
