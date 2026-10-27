package clubflow.command;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import clubflow.Club;
import clubflow.UserInterface;
import clubflow.command.event.DeleteEventCommand;
import clubflow.exception.ClubFlowException;
import clubflow.exception.CommandParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests canonical command spelling in help and syntax-error guidance. */
class CommandHelpTest {
    private final PrintStream originalOutput = System.out;
    private ByteArrayOutputStream output;
    private PrintStream capturedOutput;
    private CommandParser parser;

    @BeforeEach
    void setUp() {
        output = new ByteArrayOutputStream();
        capturedOutput = new PrintStream(output, true, StandardCharsets.UTF_8);
        System.setOut(capturedOutput);
        UserInterface ui = new UserInterface("[TEST] ", "> ");
        parser = new CommandParser(ui);
        parser.register("deleteEvent", new DeleteEventCommand(ui, new Club()));
        parser.registerAlias("deleteEvent", "de");
        parser.register("help", new HelpCommand(ui, parser));
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOutput);
        capturedOutput.close();
    }

    @Test
    void help_fullKeyword_preservesRegisteredCase() throws ClubFlowException {
        assertFalse(parser.parse("help c/DELETEEVENT"));

        String result = output.toString(StandardCharsets.UTF_8);
        assertTrue(result.contains("Syntax of command \"deleteEvent\":"));
        assertTrue(result.contains("> deleteEvent e/Tag"));
        assertFalse(result.contains("deleteevent"));
    }

    @Test
    void help_alias_displaysFullRegisteredKeyword() throws ClubFlowException {
        assertFalse(parser.parse("help c/DE"));

        String result = output.toString(StandardCharsets.UTF_8);
        assertTrue(result.contains("Syntax of command \"deleteEvent\":"));
        assertTrue(result.contains("> deleteEvent e/Tag"));
        assertTrue(result.contains("Aliases: [de]"));
    }

    @Test
    void help_allCommands_listsEachCanonicalKeywordOnce() throws ClubFlowException {
        assertFalse(parser.parse("HELP"));

        String result = output.toString(StandardCharsets.UTF_8);
        assertEquals(1, result.lines()
                .filter(line -> line.contains("Syntax of command \"deleteEvent\":")).count());
        assertFalse(result.contains("Syntax of command \"de\":"));
        assertFalse(result.contains("deleteevent"));
    }

    @Test
    void parse_invalidAliasArguments_syntaxUsesRegisteredCase() {
        CommandParseException error = assertThrows(CommandParseException.class, () -> parser.parse("DE"));

        assertTrue(error.willPrintSyntax());
        error.printCorrectSyntax(parser);
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Syntax of command \"deleteEvent\":"));
    }
}
