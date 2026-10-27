package clubflow.command;

import clubflow.command.member.CreateMemberCommand;
import clubflow.command.member.DeleteMemberCommand;
import clubflow.command.member.ViewMemberCommand;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import clubflow.Club;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import clubflow.UserInterface;
import clubflow.exception.ClubFlowException;
import clubflow.exception.CommandParseException;
import clubflow.member.Member;

//@@author 77chenchen

/**
 * Tests creation, viewing, and deletion of members through the command parser.
 */
class MemberCommandsTest {
    private final InputStream originalInput = System.in;
    private final PrintStream originalOutput = System.out;
    private ByteArrayOutputStream output;

    @BeforeEach
    void redirectOutput() {
        output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreSystemStreams() {
        System.setIn(originalInput);
        System.setOut(originalOutput);
    }

    @Test
    void createMember_validDetails_storesMemberAndRepeatedFunctions() throws ClubFlowException {
        Club club = new Club();
        CommandParser parser = createMemberParser(club, "");

        parser.parse("createMember n/\"Lucy Tan\" t/lucytan e/lucy@example.com "
                + "f/president f/publicity");

        Member member = club.getMembers().getFirst();
        assertAll(
                () -> assertEquals("Lucy Tan", member.getName()),
                () -> assertEquals("lucytan", member.getTelegramHandle()),
                () -> assertEquals("lucy@example.com", member.getEmail()),
                () -> assertEquals(List.of("president", "publicity"), member.getFunctions()),
                () -> assertTrue(printedOutput().contains("Created member: Lucy Tan"))
        );
    }

    @Test
    void addMember_emailOnlyAlias_storesMemberWithoutFunctions() throws ClubFlowException {
        Club club = new Club();
        CommandParser parser = createMemberParser(club, "");

        parser.parse("addMember n/Thomas e/thomas@example.com");

        Member member = club.getMembers().getFirst();
        assertAll(
                () -> assertEquals("Thomas", member.getName()),
                () -> assertEquals("", member.getTelegramHandle()),
                () -> assertEquals(List.of(), member.getFunctions())
        );
    }

    @Test
    void createMember_missingOrInvalidRequiredDetails_throwsParseException() {
        Club club = new Club();
        CommandParser parser = createMemberParser(club, "");

        assertAll(
                () -> assertThrows(CommandParseException.class,
                        () -> parser.parse("createMember n/Lucy")),
                () -> assertThrows(CommandParseException.class,
                        () -> parser.parse("createMember n/\"\" e/lucy@example.com")),
                () -> assertThrows(CommandParseException.class,
                        () -> parser.parse("createMember n/Lucy t/@lucy")),
                () -> assertThrows(CommandParseException.class,
                        () -> parser.parse("createMember n/Lucy e/lucy@example.com f/\"\""))
        );
        assertTrue(club.getMembers().isEmpty());
    }

    @Test
    void viewMember_noMembers_printsEmptyMessage() throws ClubFlowException {
        Club club = new Club();
        CommandParser parser = createMemberParser(club, "");

        parser.parse("viewMember");

        assertTrue(printedOutput().contains("No members found."));
    }

    @Test
    void viewMember_membersExist_printsIndexesAndDetails() throws ClubFlowException {
        Club club = new Club();
        club.addMember(new Member("Lucy", "lucy", "", List.of("president")));
        club.addMember(new Member("Thomas", "", "thomas@example.com", List.of()));
        CommandParser parser = createMemberParser(club, "");

        parser.parse("viewMember");

        String result = printedOutput();
        assertAll(
                () -> assertTrue(result.contains("1. Lucy")),
                () -> assertTrue(result.contains("Telegram: @lucy")),
                () -> assertTrue(result.contains("Functions: president")),
                () -> assertTrue(result.contains("2. Thomas")),
                () -> assertTrue(result.contains("Email: thomas@example.com")),
                () -> assertTrue(result.contains("Functions: None"))
        );
    }

    @Test
    void deleteMember_forceFlag_removesMemberWithoutConfirmation() throws ClubFlowException {
        Club club = dataWithTwoMembers();
        CommandParser parser = createMemberParser(club, "");

        parser.parse("deleteMember i/1 /f");

        assertAll(
                () -> assertEquals(1, club.getMembers().size()),
                () -> assertEquals("Thomas", club.getMembers().getFirst().getName()),
                () -> assertTrue(printedOutput().contains("Deleted member: Lucy"))
        );
    }

    @Test
    void deleteMember_withoutForce_respectsConfirmation() throws ClubFlowException {
        Club club = dataWithTwoMembers();
        CommandParser parser = createMemberParser(club, "no\n");

        parser.parse("deleteMember i/1");

        assertAll(
                () -> assertEquals(2, club.getMembers().size()),
                () -> assertTrue(printedOutput().contains("Deletion cancelled."))
        );
    }

    @Test
    void deleteMember_confirmed_removesSelectedMember() throws ClubFlowException {
        Club club = dataWithTwoMembers();
        CommandParser parser = createMemberParser(club, "yes\n");

        parser.parse("deleteMember i/2");

        assertAll(
                () -> assertEquals(1, club.getMembers().size()),
                () -> assertEquals("Lucy", club.getMembers().getFirst().getName()),
                () -> assertTrue(printedOutput().contains("Deleted member: Thomas"))
        );
    }

    @Test
    void deleteMember_invalidIndex_rejectsOrReportsMissingMember() throws ClubFlowException {
        Club club = dataWithTwoMembers();
        CommandParser parser = createMemberParser(club, "");

        assertAll(
                () -> assertThrows(CommandParseException.class,
                        () -> parser.parse("deleteMember i/0 /f")),
                () -> assertThrows(CommandParseException.class,
                        () -> parser.parse("deleteMember i/not-a-number /f")),
                () -> assertFalse(parser.parse("deleteMember i/3 /f"))
        );
        assertAll(
                () -> assertEquals(2, club.getMembers().size()),
                () -> assertTrue(printedOutput().contains("Member not found at index 3."))
        );
    }

    /**
     * Creates a parser with all member commands and aliases registered.
     */
    private CommandParser createMemberParser(Club club, String input) {
        System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        UserInterface ui = new UserInterface("[TEST] ", "> ");
        CommandParser parser = new CommandParser(ui);
        CreateMemberCommand createMemberCommand = new CreateMemberCommand(ui, club);
        parser.register("createMember", createMemberCommand);
        parser.register("addMember", createMemberCommand);
        parser.register("viewMember", new ViewMemberCommand(ui, club));
        parser.register("deleteMember", new DeleteMemberCommand(ui, club));
        return parser;
    }

    /**
     * Returns sample data whose order makes index-based deletion easy to verify.
     */
    private Club dataWithTwoMembers() {
        Club club = new Club();
        club.addMember(new Member("Lucy", "lucy", "", List.of("president")));
        club.addMember(new Member("Thomas", "", "thomas@example.com", List.of()));
        return club;
    }

    private String printedOutput() {
        return output.toString(StandardCharsets.UTF_8);
    }
}

//@@author
