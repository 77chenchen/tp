package clubflow.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import clubflow.Club;
import clubflow.UserInterface;
import clubflow.command.member.AssignMemberCommand;
import clubflow.command.member.FindMemberCommand;
import clubflow.command.member.UnassignMemberCommand;
import clubflow.command.member.ViewMemberEventsCommand;
import clubflow.event.Event;
import clubflow.event.EventAssignment;
import clubflow.exception.ClubFlowException;
import clubflow.exception.CommandParseException;
import clubflow.member.Member;

//@@author 77chenchen

/**
 * Tests member assignment queries through the real command parser and shared club model.
 */
class MemberAssignmentQueriesTest {
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
    void viewMemberEvents_multipleAssignments_sortsDatesAndShowsResponsibilities()
            throws ClubFlowException {
        Club club = clubWithMembersAndEvents();
        CommandParser parser = parserFor(club);
        parser.parse("assignMember e/LATE i/1 r/Logistics");
        parser.parse("assignMember e/NODATE i/1");
        parser.parse("assignMember e/EARLY i/1 r/\"Lead Host\"");
        parser.parse("assignMember e/OVERDUE i/1 r/Publicity");
        parser.parse("assignMember e/OTHER i/2 r/Treasurer");
        club.markSaved();
        output.reset();

        parser.parse("vme i/1");

        String result = output();
        assertTrue(result.contains("Events assigned to Alice (member 1): 4"));
        assertTrue(result.contains("Summary: 1 upcoming | 1 overdue | 1 completed | 1 without date"));
        assertTrue(result.indexOf("[EARLY]") < result.indexOf("[LATE]"));
        assertTrue(result.indexOf("[OVERDUE]") < result.indexOf("[LATE]"));
        assertTrue(result.indexOf("[LATE]") < result.indexOf("[NODATE]"));
        assertTrue(result.contains("Date: 2001-01-05"));
        assertTrue(result.contains("Role: Lead Host"));
        assertTrue(result.contains("Status: COMPLETED"));
        assertTrue(result.contains("Status: UPCOMING"));
        assertTrue(result.contains("Status: OVERDUE"));
        assertTrue(result.contains("Status: NO DATE"));
        assertTrue(result.contains("Role: Not set"));
        assertTrue(result.contains("[NODATE] Not set"));
        assertFalse(result.contains("[OTHER]"));
        assertFalse(club.hasUnsavedChanges());
    }

    @Test
    void viewMemberEvents_emptyMissingAndInvalidMembers_reportClearly() throws ClubFlowException {
        Club club = clubWithMembersAndEvents();
        CommandParser parser = parserFor(club);

        parser.parse("viewMemberEvents i/1");
        assertTrue(output().contains("No events assigned to Alice."));

        output.reset();
        parser.parse("viewMemberEvents i/99");
        assertTrue(output().contains("Member not found at index 99."));

        assertThrows(CommandParseException.class, () -> parser.parse("viewMemberEvents"));
        assertThrows(CommandParseException.class, () -> parser.parse("viewMemberEvents i/0"));
        assertThrows(CommandParseException.class, () -> parser.parse("viewMemberEvents i/not-a-number"));
        assertThrows(CommandParseException.class, () -> parser.parse("viewMemberEvents i/1 e/EARLY"));
    }

    @Test
    void findMember_unassignedFlag_preservesIndexesAndCombinesWithFilters()
            throws ClubFlowException {
        Club club = clubWithMembersAndEvents();
        club.addMember(new Member("Robert", "robert", "robert@example.com", List.of("Logistics")));
        CommandParser parser = parserFor(club);
        parser.parse("assignMember e/EARLY i/1");
        parser.parse("assignMember e/LATE i/3");
        club.markSaved();
        output.reset();

        parser.parse("findMember /u");
        String unassignedResult = output();
        assertTrue(unassignedResult.contains("Matching unassigned members:"));
        assertTrue(unassignedResult.contains("2. Bob"));
        assertFalse(unassignedResult.contains("1. Alice"));
        assertFalse(unassignedResult.contains("3. Robert"));
        assertFalse(club.hasUnsavedChanges());

        output.reset();
        parser.parse("findMember /u f/Logistics q/bob");
        assertTrue(output().contains("2. Bob"));
        assertFalse(output().contains("3. Robert"));

        output.reset();
        parser.parse("findMember /u f/Publicity");
        assertTrue(output().contains("No unassigned members matched the specified criteria."));
    }

    @Test
    void findMember_unassignedFlag_reflectsAssignmentChangesAndFuzzySearch()
            throws ClubFlowException {
        Club club = clubWithMembersAndEvents();
        CommandParser parser = parserFor(club);
        parser.parse("assignMember e/EARLY i/1");

        output.reset();
        parser.parse("findMember /u q/alce");
        assertTrue(output().contains("No unassigned members matched the specified criteria."));

        parser.parse("unassignMember e/EARLY i/1");
        output.reset();
        parser.parse("findMember /u q/alce");
        assertTrue(output().contains("Possible unassigned matches"));
        assertTrue(output().contains("1. Alice"));

        parser.parse("assignMember e/LATE i/1");
        club.removeEventByTag("LATE");
        output.reset();
        parser.parse("findMember /u n/Alice");
        assertTrue(output().contains("1. Alice"));
    }

    @Test
    void findMember_unassignedFlag_handlesNoEventsAndMultipleAssignments()
            throws ClubFlowException {
        Club club = clubWithMembersAndEvents();
        CommandParser parser = parserFor(club);

        for (Event event : club.getEvents()) {
            club.removeEventByTag(event.getTag());
        }
        output.reset();
        parser.parse("findMember /u");
        assertTrue(output().contains("1. Alice"));
        assertTrue(output().contains("2. Bob"));

        club.addEvent(new Event("FIRST", "First", LocalDate.now().plusDays(1),
                null, 0, "", ""));
        club.addEvent(new Event("SECOND", "Second", LocalDate.now().plusDays(2),
                null, 0, "", ""));
        parser.parse("assignMember e/FIRST i/1");
        parser.parse("assignMember e/SECOND i/1");
        parser.parse("unassignMember e/FIRST i/1");
        output.reset();
        parser.parse("findMember /u");
        assertFalse(output().contains("1. Alice"));

        parser.parse("unassignMember e/SECOND i/1");
        output.reset();
        parser.parse("findMember /u");
        assertTrue(output().contains("1. Alice"));
    }

    @Test
    void findMember_unassignedFlag_reportsSpecificEmptyMessages()
            throws ClubFlowException {
        Club emptyClub = new Club();
        parserFor(emptyClub).parse("findMember /u");
        assertTrue(output().contains("No unassigned members found."));

        output.reset();
        Club club = clubWithMembersAndEvents();
        CommandParser parser = parserFor(club);
        parser.parse("assignMember e/EARLY i/1");
        parser.parse("assignMember e/LATE i/2");
        output.reset();
        parser.parse("findMember /u");
        assertTrue(output().contains("No unassigned members found."));
    }

    @Test
    void assignmentQueries_foreignMemberRejectedAndSnapshotsCannotBeModified() {
        Club club = clubWithMembersAndEvents();
        Member member = club.findMemberByIndex(1).orElseThrow();
        club.assignMemberToEvent("EARLY", member, "Host");

        List<EventAssignment> assignments = club.getAssignmentsForMember(member);
        assertEquals(1, assignments.size());
        assertThrows(UnsupportedOperationException.class, () -> assignments.add(assignments.getFirst()));

        Member foreignMember = new Member("Foreign", "foreign", "", List.of());
        assertThrows(IllegalArgumentException.class,
                () -> club.getAssignmentsForMember(foreignMember));
        assertThrows(NullPointerException.class,
                () -> club.getAssignmentsForMember(null));
    }

    /** Creates sample members and dated, undated, completed, and unrelated events. */
    private Club clubWithMembersAndEvents() {
        Club club = new Club();
        club.addMember(new Member("Alice", "alice", "alice@example.com", List.of("Publicity")));
        club.addMember(new Member("Bob", "bob", "bob@example.com", List.of("Logistics")));
        club.addEvent(new Event("LATE", "Year End Party", LocalDate.of(2999, 12, 31),
                null, 0, "Hall", ""));
        club.addEvent(new Event("NODATE", "", null,
                null, 0, "", ""));
        club.addEvent(new Event("EARLY", "Orientation", LocalDate.of(2001, 1, 5),
                null, 0, "Room 1", "", true));
        club.addEvent(new Event("OTHER", "Finance Meeting", LocalDate.of(2026, 6, 1),
                null, 0, "Room 2", ""));
        club.addEvent(new Event("OVERDUE", "Publicity Check", LocalDate.of(2000, 1, 1),
                null, 0, "Room 3", ""));
        return club;
    }

    /** Registers both query commands and the assignment commands used to prepare scenarios. */
    private CommandParser parserFor(Club club) {
        UserInterface ui = new UserInterface("[TEST] ", "> ");
        CommandParser parser = new CommandParser(ui);
        parser.register("findMember", new FindMemberCommand(ui, club));
        ViewMemberEventsCommand viewMemberEvents = new ViewMemberEventsCommand(ui, club);
        parser.register("viewMemberEvents", viewMemberEvents);
        parser.registerAlias("viewMemberEvents", "vme");
        parser.register("assignMember", new AssignMemberCommand(ui, club));
        parser.register("unassignMember", new UnassignMemberCommand(ui, club));
        return parser;
    }

    private String output() {
        return output.toString(StandardCharsets.UTF_8);
    }
}

//@@author
