package clubflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import clubflow.event.Event;
import clubflow.event.EventAssignment;
import clubflow.member.Member;

//@@author 77chenchen

/**
 * Verifies that event assignments remain correct across saves, loads, and deletions.
 */
class MemberAssignmentStorageTest {
    @Test
    void saveAndLoad_assignmentsSurviveMemberIndexChanges(@TempDir Path tempDir) throws IOException {
        Club data = new Club();
        Member first = new Member("Alice", "alice", "", List.of());
        Member second = new Member("Bob", "bob", "", List.of());
        data.addMember(first);
        data.addMember(second);
        data.addEvent(new Event("Party", "Party", null, null, 0, "", ""));
        data.assignMemberToEvent("party", second, "Host");
        data.removeMemberByIndex(1);

        Path saveFile = tempDir.resolve("club.cf");
        SaveLoadManager manager = new SaveLoadManager();
        manager.save(data, saveFile);
        Club loaded = manager.load(saveFile);

        EventAssignment assignment = loaded.getAssignmentsForEvent("PARTY").getFirst();
        assertSame(loaded.findMemberByIndex(1).orElseThrow(), assignment.getMember());
        assertSame(loaded.findEventByTag("party").orElseThrow(), assignment.getEvent());
        assertEquals("Host", assignment.getRole());
        assertTrue(Files.readString(saveFile).contains("ASSIGNMENT|Party|1|Host"));
        assertThrows(UnsupportedOperationException.class, () -> loaded.getAssignments().clear());
    }

    @Test
    void load_oldFormatWithoutAssignments_stillWorks(@TempDir Path tempDir) throws IOException {
        Path saveFile = tempDir.resolve("old.cf");
        Files.writeString(saveFile, "MEMBER|Alice|alice||\nEVENT|Party|Party|null|null|0.0||\n");

        Club loaded = new SaveLoadManager().load(saveFile);

        assertEquals(1, loaded.getMembers().size());
        assertEquals(1, loaded.getEvents().size());
        assertTrue(loaded.getAssignments().isEmpty());
    }

    @Test
    void load_invalidAssignment_rejectsFile(@TempDir Path tempDir) throws IOException {
        Path saveFile = tempDir.resolve("invalid.cf");
        String records = "MEMBER|Alice|alice||\nEVENT|Party|Party|null|null|0.0||\n";
        Files.writeString(saveFile, records + "ASSIGNMENT|Party|2|Host\n");
        assertThrows(IOException.class, () -> new SaveLoadManager().load(saveFile));

        Files.writeString(saveFile, records + "ASSIGNMENT|Party|1|Host\nASSIGNMENT|party|1|Other\n");
        assertThrows(IOException.class, () -> new SaveLoadManager().load(saveFile));
    }

    @Test
    void deletingEvent_removesItsAssignments() {
        Club data = new Club();
        Member member = new Member("Alice", "alice", "", List.of());
        data.addMember(member);
        data.addEvent(new Event("Party", "Party", null, null, 0, "", ""));
        data.assignMemberToEvent("party", member, "");

        data.removeEventByTag("PARTY");

        assertTrue(data.getAssignments().isEmpty());
    }

    @Test
    void duplicateNames_remainDistinctButSameObjectCannotBeAddedTwice(@TempDir Path tempDir) throws IOException {
        Club data = new Club();
        Member first = new Member("Alex", "alex", "", List.of());
        Member second = new Member("Alex", "alex", "", List.of());
        data.addMember(first);
        data.addMember(second);
        assertThrows(IllegalArgumentException.class, () -> data.addMember(first));
        data.addEvent(new Event("Party", "Party", null, null, 0, "", ""));
        data.assignMemberToEvent("party", second, "Host");

        Path saveFile = tempDir.resolve("duplicate-names.cf");
        new SaveLoadManager().save(data, saveFile);
        Club loaded = new SaveLoadManager().load(saveFile);

        assertSame(loaded.findMemberByIndex(2).orElseThrow(),
                loaded.getAssignmentsForEvent("party").getFirst().getMember());
    }

    @Test
    void memberAndAssignmentChanges_triggerAutosaveAndSurviveEventEdit(@TempDir Path tempDir) throws IOException {
        Club data = new Club();
        Member original = new Member("Alice", "alice", "", List.of());
        data.addMember(original);
        data.addEvent(new Event("Party", "Party", null, null, 0, "", ""));
        data.markSaved();

        assertTrue(data.assignMemberToEvent("party", original, "Host"));
        assertTrue(data.hasUnsavedChanges());
        data.markSaved();
        assertFalse(data.assignMemberToEvent("party", original, "Other"));
        assertFalse(data.hasUnsavedChanges());

        Member replacement = new Member("Alice Tan", "alice", "", List.of());
        data.replaceMemberByIndex(1, replacement);
        assertTrue(data.hasUnsavedChanges());
        data.markSaved();

        Event updatedEvent = new Event("Party", "Updated Party", null, null, 0, "", "");
        assertTrue(data.updateEvent(updatedEvent));
        assertSame(updatedEvent, data.getAssignmentsForEvent("party").getFirst().getEvent());

        Path saveFile = tempDir.resolve("updated.cf");
        SaveLoadManager manager = new SaveLoadManager();
        manager.save(data, saveFile);
        Club loaded = manager.load(saveFile);
        assertEquals("Updated Party", loaded.getAssignmentsForEvent("party").getFirst().getEvent().getName());
        assertEquals("Alice Tan", loaded.getAssignmentsForEvent("party").getFirst().getMember().getName());

        data.markSaved();
        assertTrue(data.unassignMemberFromEvent("party", replacement));
        assertTrue(data.hasUnsavedChanges());
    }
}

//@@author
