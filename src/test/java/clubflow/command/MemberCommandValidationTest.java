package clubflow.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import clubflow.Club;
import clubflow.UserInterface;
import clubflow.command.member.AssignMemberCommand;
import clubflow.command.member.CreateMemberCommand;
import clubflow.command.member.EditMemberCommand;
import clubflow.command.member.UnassignMemberCommand;
import clubflow.command.member.ViewMemberEventsCommand;
import clubflow.event.Event;
import clubflow.exception.CommandParseException;
import clubflow.member.Member;

/**
 * Checks invalid member commands and verifies that unsuccessful operations keep saved data unchanged.
 */
class MemberCommandValidationTest {
    private final List<String> messages = new ArrayList<>();
    private final UserInterface ui = new UserInterface("", "") {
        @Override
        public void print(String message) {
            messages.add(message);
        }
    };
    private Club club;
    private CommandParser parser;

    @BeforeEach
    void setUp() {
        club = new Club();
        club.addMember(new Member("Alice", "alice", "", List.of()));
        club.addEvent(new Event("AGM", "Meeting", null, null, 0, "", ""));
        club.markSaved();
        parser = new CommandParser(ui);
        parser.register("assignMember", new AssignMemberCommand(ui, club));
        parser.register("unassignMember", new UnassignMemberCommand(ui, club));
        parser.register("editMember", new EditMemberCommand(ui, club));
        parser.register("createMember", new CreateMemberCommand(ui, club));
        parser.register("viewMemberEvents", new ViewMemberEventsCommand(ui, club));
    }

    @Test
    void assignmentCommands_invalidTagsAndIndexes_rejectWithoutChangingData() {
        for (String command : List.of("assignMember", "unassignMember")) {
            assertThrows(CommandParseException.class, () -> parser.parse(command + " e/\"\" i/1"));
            for (String index : List.of("0", "-1", "abc", "2147483648")) {
                assertThrows(CommandParseException.class, () -> parser.parse(command + " e/AGM i/" + index));
            }
        }
        assertTrue(club.getAssignments().isEmpty());
        assertFalse(club.hasUnsavedChanges());
    }

    @Test
    void assignMember_invalidRole_rejectsSaveSeparatorsAndLineBreaks() {
        AssignMemberCommand command = new AssignMemberCommand(ui, club);

        for (String role : List.of("Host|Guest", "Host\nGuest", "Host\rGuest")) {
            HashMap<String, String> args = new HashMap<>(Map.of("e", "AGM", "i", "1", "r", role));
            assertThrows(CommandParseException.class, () -> command.validateArgs(args));
        }
        assertTrue(club.getAssignments().isEmpty());
        assertFalse(club.hasUnsavedChanges());
    }

    @Test
    void unassignMember_missingRecords_reportsSpecificErrorsAndKeepsAssignments() throws Exception {
        Member alice = club.getMembers().getFirst();
        club.assignMemberToEvent("AGM", alice, "Host");
        club.markSaved();

        assertFalse(parser.parse("unassignMember e/MISSING i/1"));
        assertFalse(parser.parse("unassignMember e/AGM i/99"));

        assertEquals(List.of("Event not found: MISSING", "Member not found at index 99."), messages);
        assertEquals(1, club.getAssignments().size());
        assertEquals("Host", club.getAssignments().getFirst().getRole());
        assertFalse(club.hasUnsavedChanges());
    }

    @Test
    void unassignMember_noAssignment_reportsNoChangeAndLeavesClubSaved() throws Exception {
        assertFalse(parser.parse("unassignMember e/AGM i/1"));

        assertEquals(List.of("Member is not assigned to event AGM."), messages);
        assertEquals(1, club.getMembers().size());
        assertEquals(1, club.getEvents().size());
        assertTrue(club.getAssignments().isEmpty());
        assertFalse(club.hasUnsavedChanges());
    }

    @Test
    void editMember_invalidOrMissingIndex_preservesExistingMember() throws Exception {
        assertThrows(CommandParseException.class, () -> parser.parse("editMember i/not-a-number n/Changed"));
        assertFalse(parser.parse("editMember i/99 n/Changed"));

        assertEquals(List.of("Member not found at index 99."), messages);
        assertEquals("Alice", club.getMembers().getFirst().getName());
        assertFalse(club.hasUnsavedChanges());
    }

    @Test
    void createMember_saveSeparators_rejectsWithoutAddingMember() {
        for (String input : List.of("createMember n/Ali|ce t/alice", "createMember n/Bob t/bob f/Host,Guest")) {
            assertThrows(CommandParseException.class, () -> parser.parse(input));
        }
        assertEquals(1, club.getMembers().size());
        assertFalse(club.hasUnsavedChanges());
    }

    @Test
    void viewMemberEvents_equalDates_sortsByTagIgnoringCase() throws Exception {
        Member alice = club.getMembers().getFirst();
        LocalDate date = LocalDate.of(2999, 1, 1);
        club.addEvent(new Event("zebra", "Meeting", date, null, 0, "", ""));
        club.addEvent(new Event("ALPHA", "Meeting", date, null, 0, "", ""));
        club.assignMemberToEvent("zebra", alice, "Host");
        club.assignMemberToEvent("ALPHA", alice, "Guest");
        club.markSaved();

        assertFalse(parser.parse("viewMemberEvents i/1"));

        assertEquals(List.of("[ALPHA] Meeting", "[zebra] Meeting"), messages.stream()
                .filter(message -> message.startsWith("[")).toList());
        assertFalse(club.hasUnsavedChanges());
    }
}
