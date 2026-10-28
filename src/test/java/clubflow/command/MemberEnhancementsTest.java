package clubflow.command;

import clubflow.command.event.ViewEventCommand;
import clubflow.command.member.AssignMemberCommand;
import clubflow.command.member.EditMemberCommand;
import clubflow.command.member.FindMemberCommand;
import clubflow.command.member.UnassignMemberCommand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import clubflow.Club;
import clubflow.UserInterface;
import clubflow.event.Event;
import clubflow.exception.ClubFlowException;
import clubflow.exception.CommandParseException;
import clubflow.member.Member;

//@@author 77chenchen

/**
 * Tests member editing, searching, and event assignment through the real command parser.
 */
class MemberEnhancementsTest {
    private final PrintStream originalOutput = System.out;
    private ByteArrayOutputStream output;

    @BeforeEach
    void redirectOutput() {
        output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOutput);
    }

    @Test
    void editMember_partialUpdate_preservesOtherFieldsAndPosition() throws ClubFlowException {
        Club data = sampleData();
        CommandParser parser = parserFor(data);

        parser.parse("editMember i/2 e/new@example.com f/Logistics f/Treasurer");

        Member updated = data.findMemberByIndex(2).orElseThrow();
        assertEquals("Bob", updated.getName());
        assertEquals("bob", updated.getTelegramHandle());
        assertEquals("new@example.com", updated.getEmail());
        assertEquals(List.of("Logistics", "Treasurer"), updated.getFunctions());
        assertEquals("Alice", data.findMemberByIndex(1).orElseThrow().getName());
        assertTrue(output().contains("Updated member at index 2"));
    }

    @Test
    void editMember_clearContactAndFunctions_retainsValidMember() throws ClubFlowException {
        Club data = sampleData();
        CommandParser parser = parserFor(data);

        parser.parse("editMember i/2 t/\"\" f/\"\"");

        Member updated = data.findMemberByIndex(2).orElseThrow();
        assertEquals("", updated.getTelegramHandle());
        assertEquals("bob@example.com", updated.getEmail());
        assertEquals(List.of(), updated.getFunctions());
    }

    @Test
    void editMember_invalidReplacement_doesNotChangeMemberOrAssignments() throws ClubFlowException {
        Club data = sampleData();
        CommandParser parser = parserFor(data);
        parser.parse("assignMember e/party i/2 r/Host");
        Member original = data.findMemberByIndex(2).orElseThrow();

        assertThrows(CommandParseException.class, () -> parser.parse("editMember i/2 t/\"\" e/\"\""));
        assertThrows(CommandParseException.class, () -> parser.parse("editMember i/2 n/\"\""));
        assertThrows(CommandParseException.class, () -> parser.parse("editMember i/2 f/Host f/\"\""));
        assertThrows(CommandParseException.class, () -> parser.parse("editMember i/2"));
        assertThrows(CommandParseException.class, () -> parser.parse("editMember i/0 n/Test"));
        assertSame(original, data.findMemberByIndex(2).orElseThrow());
        assertSame(original, data.getAssignmentsForEvent("party").getFirst().getMember());
    }

    @Test
    void findMember_caseInsensitiveMatches_keepOriginalIndexes() throws ClubFlowException {
        Club data = sampleData();
        CommandParser parser = parserFor(data);

        parser.parse("findMember q/LOGISTICS");
        assertTrue(output().contains("2. Bob"));
        assertFalse(output().contains("1. Alice"));

        output.reset();
        parser.parse("findMember q/example.com");
        assertTrue(output().contains("1. Alice"));
        assertTrue(output().contains("2. Bob"));

        output.reset();
        parser.parse("findMember q/nobody");
        assertTrue(output().contains("No matching members found."));
        assertThrows(CommandParseException.class, () -> parser.parse("findMember q/\"\""));
    }

    @Test
    void findMember_multipleKeywordsAndFilters_narrowResults() throws ClubFlowException {
        Club data = sampleData();
        CommandParser parser = parserFor(data);

        parser.parse("findMember q/EXAMPLE q/logistics");
        assertTrue(output().contains("2. Bob"));
        assertFalse(output().contains("1. Alice"));

        output.reset();
        parser.parse("findMember n/bo t/@BOB e/example f/log");
        assertTrue(output().contains("2. Bob"));
        assertFalse(output().contains("1. Alice"));

        output.reset();
        parser.parse("findMember n/Bob f/publicity");
        assertTrue(output().contains("No matching members found."));
    }

    @Test
    void findMember_quotedNamesRepeatedFunctionsAndDetails_disambiguateMembers() throws ClubFlowException {
        Club data = sampleData();
        data.addMember(new Member("Alice Tan", "alicetan", "tan@example.com",
                List.of("Publicity", "Logistics")));
        CommandParser parser = parserFor(data);

        parser.parse("findMember n/\"Alice Tan\" f/PUBLIC f/logistics");
        assertTrue(output().contains("3. Alice Tan"));
        assertTrue(output().contains("Telegram: @alicetan"));
        assertTrue(output().contains("Email: tan@example.com"));
        assertTrue(output().contains("Functions: Publicity, Logistics"));
        assertFalse(output().contains("1. Alice"));

        output.reset();
        parser.parse("findMember q/@alicetan");
        assertTrue(output().contains("3. Alice Tan"));
    }

    @Test
    void findMember_rejectsMissingOrBlankFilters() {
        Club data = sampleData();
        CommandParser parser = parserFor(data);

        assertThrows(CommandParseException.class, () -> parser.parse("findMember"));
        assertThrows(CommandParseException.class, () -> parser.parse("findMember q/Alice q/\"\""));
        assertThrows(CommandParseException.class, () -> parser.parse("findMember n/\"   \""));
        assertThrows(CommandParseException.class, () -> parser.parse("findMember t/@"));
        assertThrows(CommandParseException.class, () -> parser.parse("findMember f/Logistics f/\"\""));
    }

    @Test
    void findMember_fuzzyName_showsDirectResultsBeforeMultipleSuggestions() throws ClubFlowException {
        Club data = sampleData();
        data.addMember(new Member("Alie", "alie", "alie@example.com", List.of()));
        data.addMember(new Member("Alce", "alce", "alce@example.com", List.of()));
        CommandParser parser = parserFor(data);

        parser.parse("findMember q/alce");

        String result = output();
        assertTrue(result.contains("Matching members:"));
        assertTrue(result.contains("Possible matches"));
        assertTrue(result.indexOf("4. Alce") < result.indexOf("1. Alice"));
        assertTrue(result.indexOf("1. Alice") < result.indexOf("3. Alie"));
        assertFalse(result.contains("2. Bob"));
    }

    @Test
    void findMember_directMatches_rankExactThenPrefixThenSubstring() throws ClubFlowException {
        Club data = sampleData();
        data.addMember(new Member("Alice Tan", "alicetan", "tan@example.com", List.of()));
        data.addMember(new Member("Dr Alice", "dralice", "doctor@example.com", List.of()));
        CommandParser parser = parserFor(data);

        parser.parse("findMember n/alice");

        String result = output();
        assertTrue(result.indexOf("1. Alice") < result.indexOf("3. Alice Tan"));
        assertTrue(result.indexOf("3. Alice Tan") < result.indexOf("4. Dr Alice"));
    }

    @Test
    void findMember_fuzzyTelegramAndLongName_workWithStrictOtherFilters() throws ClubFlowException {
        Club data = sampleData();
        data.addMember(new Member("Marianne", "johndoe", "marianne@example.com", List.of("Host")));
        CommandParser parser = parserFor(data);

        parser.parse("findMember t/@johndoo f/host");
        assertTrue(output().contains("Possible matches"));
        assertTrue(output().contains("3. Marianne"));

        output.reset();
        parser.parse("findMember n/marianx e/marianne@example.com");
        assertTrue(output().contains("3. Marianne"));

        output.reset();
        parser.parse("findMember n/marianx e/bob@example.com");
        assertTrue(output().contains("No matching members found."));

        output.reset();
        parser.parse("findMember e/mariane@example.com");
        assertTrue(output().contains("No matching members found."));
    }

    @Test
    void findMember_shortQueriesStayStrictAndSuggestionsStopAtFive() throws ClubFlowException {
        Club data = new Club();
        CommandParser parser = parserFor(data);
        data.addMember(new Member("Bob", "bob", "bob@example.com", List.of()));

        parser.parse("findMember q/bb");
        assertTrue(output().contains("No matching members found."));

        output.reset();
        for (char suffix = 'a'; suffix <= 'g'; suffix++) {
            String name = "maria" + suffix;
            data.addMember(new Member(name, name, name + "@example.com", List.of()));
        }
        parser.parse("findMember q/mariax");
        for (int index = 2; index <= 6; index++) {
            assertTrue(output().contains(index + ". maria"));
        }
        assertFalse(output().contains("7. maria"));
        assertFalse(output().contains("8. maria"));
    }

    @Test
    void assignMember_rejectsMissingAndDuplicate_andShowsRoster() throws ClubFlowException {
        Club data = sampleData();
        CommandParser parser = parserFor(data);

        parser.parse("assignMember e/PARTY i/2 r/\"Lead Host\"");
        parser.parse("assignMember e/party i/2 r/Another");
        parser.parse("viewEvent e/party");

        assertEquals(1, data.getAssignmentsForEvent("party").size());
        assertTrue(output().contains("Bob (Lead Host)"));
        assertTrue(output().contains("already assigned"));
        assertThrows(CommandParseException.class, () -> parser.parse("assignMember e/party i/0"));
        assertThrows(CommandParseException.class, () -> parser.parse("assignMember e/party i/2 r/bad|role"));

        parser.parse("assignMember e/missing i/1");
        parser.parse("assignMember e/party i/9");
        assertTrue(output().contains("Event not found: missing"));
        assertTrue(output().contains("Member not found at index 9"));
        assertEquals(1, data.getAssignmentsForEvent("party").size());
    }

    @Test
    void editAndDeleteMember_updateAssignmentReferences() throws ClubFlowException {
        Club data = sampleData();
        CommandParser parser = parserFor(data);
        parser.parse("assignMember e/party i/2");

        parser.parse("editMember i/2 n/Robert");
        assertSame(data.findMemberByIndex(2).orElseThrow(),
                data.getAssignmentsForEvent("party").getFirst().getMember());
        parser.parse("viewEvent e/party");
        assertTrue(output().contains("- Robert"));

        data.removeMemberByIndex(1);
        assertSame(data.findMemberByIndex(1).orElseThrow(),
                data.getAssignmentsForEvent("party").getFirst().getMember());
        data.removeMemberByIndex(1);
        assertTrue(data.getAssignmentsForEvent("party").isEmpty());
    }

    @Test
    void unassignMember_removesOnlySelectedAssignment() throws ClubFlowException {
        Club data = sampleData();
        CommandParser parser = parserFor(data);
        parser.parse("assignMember e/party i/1");
        parser.parse("assignMember e/party i/2 r/Host");

        parser.parse("unassignMember e/PARTY i/1");
        assertEquals(1, data.getAssignmentsForEvent("party").size());
        assertSame(data.findMemberByIndex(2).orElseThrow(),
                data.getAssignmentsForEvent("party").getFirst().getMember());
        assertEquals(2, data.getMembers().size());

        parser.parse("unassignMember e/party i/1");
        assertTrue(output().contains("not assigned"));
        parser.parse("assignMember e/party i/1");
        assertEquals(2, data.getAssignmentsForEvent("party").size());
    }

    /** Builds independent sample data for each command scenario. */
    private Club sampleData() {
        Club data = new Club();
        data.addMember(new Member("Alice", "", "alice@example.com", List.of("Publicity")));
        data.addMember(new Member("Bob", "bob", "bob@example.com", List.of("Logistics")));
        data.addEvent(new Event("party", "Club Party", null, null, 0, "Hall", ""));
        return data;
    }

    /** Registers the new commands together with the event viewer they extend. */
    private CommandParser parserFor(Club data) {
        UserInterface ui = new UserInterface("[TEST] ", "> ");
        CommandParser parser = new CommandParser(ui);
        parser.register("editMember", new EditMemberCommand(ui, data));
        parser.register("findMember", new FindMemberCommand(ui, data));
        parser.register("assignMember", new AssignMemberCommand(ui, data));
        parser.register("unassignMember", new UnassignMemberCommand(ui, data));
        parser.register("viewEvent", new ViewEventCommand(ui, data));
        return parser;
    }

    private String output() {
        return output.toString(StandardCharsets.UTF_8);
    }
}

//@@author
