package clubflow.command;

import clubflow.command.event.InviteCommand;
import clubflow.command.event.DeleteEventCommand;

import clubflow.Club;
import clubflow.UserInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests command suggestions through the parser's public API without executing commands.
 */
class CommandCorrectionTest {
    private CommandParser parser;
    private UserInterface ui;

    @BeforeEach
    void setUp() {
        ui = new UserInterface("[TEST] ", "> ");
        parser = new CommandParser(ui);
        parser.register("invite", new InviteCommand(ui, new Club()));
        parser.registerAlias("invite", "invitation");
    }

    @Test
    void suggestedCorrection_commonTypos_suggestsRegisteredCommand() {
        assertAll(
                () -> assertEquals(Optional.of("invite"), parser.suggestedCorrection("invte")),
                () -> assertEquals(Optional.of("invite"), parser.suggestedCorrection("inviite")),
                () -> assertEquals(Optional.of("invite"), parser.suggestedCorrection("invote")),
                () -> assertEquals(Optional.of("invite"), parser.suggestedCorrection("invtie"))
        );
    }

    @Test
    void suggestedCorrection_mixedCaseAndQuotedArguments_preservesArgumentText() {
        assertEquals(Optional.of("invite  e/\"Christmas  Party\""),
                parser.suggestedCorrection("  INVTE  e/\"Christmas  Party\"  "));
    }

    @Test
    void suggestedCorrection_camelCaseKeyword_preservesRegisteredSpellingAndArguments() {
        parser.register("deleteEvent", new DeleteEventCommand(ui, new Club()));
        parser.registerAlias("deleteEvent", "de");

        assertAll(
                () -> assertEquals(Optional.of("deleteEvent  e/\"TAG\""),
                        parser.suggestedCorrection("DELETEEVNT  e/\"TAG\"")),
                () -> assertTrue(parser.suggestedCorrection("DELETEEVENT e/TAG").isEmpty()),
                () -> assertTrue(parser.suggestedCorrection("DE e/TAG").isEmpty())
        );
    }

    @Test
    void suggestedCorrection_exactKeywordOrAlias_needsNoCorrection() {
        assertAll(
                () -> assertTrue(parser.suggestedCorrection("INVITE e/XMAS").isEmpty()),
                () -> assertTrue(parser.suggestedCorrection("INVITATION e/XMAS").isEmpty())
        );
    }

    @Test
    void suggestedCorrection_typoInAlias_doesNotFuzzyMatchAlias() {
        assertTrue(parser.suggestedCorrection("invitatio e/XMAS").isEmpty());
    }

    @Test
    void suggestedCorrection_invalidOrDistantInput_hasNoSuggestion() {
        assertAll(
                () -> assertTrue(parser.suggestedCorrection(null).isEmpty()),
                () -> assertTrue(parser.suggestedCorrection("").isEmpty()),
                () -> assertTrue(parser.suggestedCorrection("   ").isEmpty()),
                () -> assertTrue(parser.suggestedCorrection("inv").isEmpty()),
                () -> assertTrue(parser.suggestedCorrection("zzzzzz").isEmpty())
        );
    }

    @Test
    void suggestedCorrection_shortKeyword_allowsOnlyOneEdit() {
        parser.register("help", new HelpCommand(ui, parser));

        assertEquals(Optional.of("help"), parser.suggestedCorrection("helx"));
        assertTrue(parser.suggestedCorrection("hexx").isEmpty());
    }

    @Test
    void suggestedCorrection_equallyCloseCommands_doesNotChooseArbitrarily() {
        parser.register("invote", new InviteCommand(ui, new Club()));

        // Both "invite" and "invote" are one replacement away.
        assertTrue(parser.suggestedCorrection("invate e/XMAS").isEmpty());
    }

    @Test
    void suggestedCorrection_multipleEligibleCommands_selectsClosest() {
        parser.register("inviter", new InviteCommand(ui, new Club()));

        assertEquals(Optional.of("invite e/XMAS"), parser.suggestedCorrection("invate e/XMAS"));
    }
}
