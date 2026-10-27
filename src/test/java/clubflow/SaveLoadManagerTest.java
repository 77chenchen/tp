package clubflow;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import clubflow.event.Event;
import clubflow.member.Member;

//@@author Eugene3129
/**
 * Checks persistence of complete and optional fields and rejection of malformed save files.
 * Every test writes only to a temporary directory managed by JUnit.
 */
class SaveLoadManagerTest {
    @TempDir
    private Path tempDir;

    private final SaveLoadManager storage = new SaveLoadManager();

    @Test
    void saveAndLoad_mixedData_preservesEveryFieldAndRecordOrder() throws IOException {
        Club original = new Club();
        original.addMember(new Member("Chloé Tan", "chloe", "chloe@example.com",
                List.of("president", "treasurer")));
        original.addMember(new Member("Thomas", "", "thomas@example.com", List.of()));
        original.addEvent(new Event("XMAS", "Christmas Party", LocalDate.of(2026, 12, 25),
                LocalDate.of(2026, 12, 20), 500.25, "Main Hall", "Tables and chairs", true));
        original.addEvent(new Event("DRAFT", "", null, null, 0, "", ""));
        Path saveFile = tempDir.resolve("club.cf");

        storage.save(original, saveFile);
        Club loaded = storage.load(saveFile);

        assertEquals(List.of("Chloé Tan", "Thomas"),
                loaded.getMembers().stream().map(Member::getName).toList());
        Member member = loaded.getMembers().getFirst();
        assertAll(
                () -> assertEquals("chloe", member.getTelegramHandle()),
                () -> assertEquals("chloe@example.com", member.getEmail()),
                () -> assertEquals(List.of("president", "treasurer"), member.getFunctions()),
                () -> assertEquals("", loaded.getMembers().get(1).getTelegramHandle()),
                () -> assertEquals("thomas@example.com", loaded.getMembers().get(1).getEmail()),
                () -> assertEquals(List.of(), loaded.getMembers().get(1).getFunctions())
        );
        assertEquals(List.of("XMAS", "DRAFT"), loaded.getEvents().stream().map(Event::getTag).toList());
        Event event = loaded.getEvents().getFirst();
        assertAll(
                () -> assertEquals("Christmas Party", event.getName()),
                () -> assertEquals(LocalDate.of(2026, 12, 25), event.getDate()),
                () -> assertEquals(LocalDate.of(2026, 12, 20), event.getDeadline()),
                () -> assertEquals(500.25, event.getBudget()),
                () -> assertEquals("Main Hall", event.getVenue()),
                () -> assertEquals("Tables and chairs", event.getLogistics()),
                () -> assertTrue(event.isCompleted())
        );
        Event draft = loaded.getEvents().get(1);
        assertAll(
                () -> assertEquals("", draft.getName()),
                () -> assertNull(draft.getDate()),
                () -> assertNull(draft.getDeadline()),
                () -> assertEquals(0, draft.getBudget()),
                () -> assertEquals("", draft.getVenue()),
                () -> assertEquals("", draft.getLogistics()),
                () -> assertFalse(draft.isCompleted())
        );
    }

    @Test
    void save_emptyData_createsParentDirectoriesAndLoadsEmptyFile() throws IOException {
        Path saveFile = tempDir.resolve("nested/saves/empty.cf");

        storage.save(new Club(), saveFile);
        Club loaded = storage.load(saveFile);

        assertTrue(Files.isRegularFile(saveFile));
        assertEquals(0, Files.size(saveFile));
        assertTrue(loaded.getMembers().isEmpty());
        assertTrue(loaded.getEvents().isEmpty());
    }

    @Test
    void save_existingFile_replacesPreviousRecords() throws IOException {
        Path saveFile = tempDir.resolve("club.cf");
        Files.writeString(saveFile, "old contents that must be removed", StandardCharsets.UTF_8);
        Club replacement = new Club();
        replacement.addEvent(new Event("NEW", "", null, null, 0, "", ""));

        storage.save(replacement, saveFile);

        assertEquals(List.of("EVENT|NEW||null|null|0.0|||false"),
                Files.readAllLines(saveFile, StandardCharsets.UTF_8));
        assertEquals(1, storage.load(saveFile).getEvents().size());
    }

    @Test
    void save_parentIsAFile_throwsIOException() throws IOException {
        Path parentFile = tempDir.resolve("parent.cf");
        Files.writeString(parentFile, "existing data");

        assertThrows(IOException.class, () -> storage.save(new Club(), parentFile.resolve("club.cf")));
        assertEquals("existing data", Files.readString(parentFile));
    }

    @Test
    void load_missingFile_throwsIOException() {
        assertThrows(IOException.class, () -> storage.load(tempDir.resolve("missing.cf")));
    }

    @Test
    void load_unknownRecordType_reportsCorrectLineNumber() throws IOException {
        assertInvalidFile("EVENT|VALID||null|null|0.0||\nUNKNOWN|data\n",
                "Unknown record type at line 2");
    }

    @Test
    void load_legacyEventRecords_defaultToNotCompleted() throws IOException {
        Path saveFile = tempDir.resolve("legacy.cf");
        Files.writeString(saveFile, "EVENT|XMAS|Party|2026-12-25|2026-12-20|500.0|Hall|Chairs\n"
                + "EVENT|DRAFT||null|null|0.0||\n", StandardCharsets.UTF_8);

        Club loaded = storage.load(saveFile);

        assertEquals(List.of("XMAS", "DRAFT"), loaded.getEvents().stream().map(Event::getTag).toList());
        assertTrue(loaded.getEvents().stream().noneMatch(Event::isCompleted));
        assertEquals("Chairs", loaded.getEvents().getFirst().getLogistics());
    }

    @Test
    void load_invalidCompletionState_rejectsInsteadOfSilentlyDefaulting() {
        for (String state : List.of("", "yes", "1", "TRUE", "false ")) {
            assertAll(() -> assertInvalidFile("EVENT|XMAS||null|null|0.0|||" + state,
                    "Invalid event record at line 1"));
        }
    }

    @Test
    void load_wrongFieldCounts_rejectsMemberAndEventRecords() {
        assertAll(
                () -> assertInvalidFile("MEMBER|Lucy|lucy|", "Invalid member record at line 1"),
                () -> assertInvalidFile("MEMBER|Lucy|lucy|||extra", "Invalid member record at line 1"),
                () -> assertInvalidFile("EVENT|XMAS||null|null|0.0|", "Invalid event record at line 1"),
                () -> assertInvalidFile("EVENT|XMAS||null|null|0.0|||false|extra", "Invalid event record at line 1")
        );
    }

    @Test
    void load_invalidMemberFields_rejectsRecord() {
        assertAll(
                () -> assertInvalidFile("MEMBER||lucy||", "Invalid member record at line 1"),
                () -> assertInvalidFile("MEMBER|Lucy|||", "Invalid member record at line 1"),
                () -> assertInvalidFile("MEMBER|Lucy|@lucy||", "Invalid member record at line 1"),
                () -> assertInvalidFile("MEMBER|Lucy|lucy||president,,treasurer",
                        "Invalid member record at line 1"),
                () -> assertInvalidFile("MEMBER|Lucy|lucy||president,", "Invalid member record at line 1")
        );
    }

    @Test
    void load_invalidEventFields_rejectsRecord() {
        assertAll(
                () -> assertInvalidFile("EVENT|||null|null|0.0||", "Invalid event record at line 1"),
                () -> assertInvalidFile("EVENT|XMAS||2026-02-30|null|0.0||", "Invalid event record at line 1"),
                () -> assertInvalidFile("EVENT|XMAS||null|tomorrow|0.0||", "Invalid event record at line 1"),
                () -> assertInvalidFile("EVENT|XMAS||null|null|free||", "Invalid event record at line 1"),
                () -> assertInvalidFile("EVENT|XMAS||null|null|-1||", "Invalid event record at line 1"),
                () -> assertInvalidFile("EVENT|XMAS||null|null|NaN||", "Invalid event record at line 1"),
                () -> assertInvalidFile("EVENT|XMAS||null|null|Infinity||", "Invalid event record at line 1")
        );
    }

    @Test
    void load_duplicateEventTags_rejectsCaseInsensitiveDuplicates() throws IOException {
        assertInvalidFile("EVENT|XMAS||null|null|0.0||\nEVENT|xmas||null|null|0.0||\n",
                "Duplicate event tag at line 2");
    }

    /**
     * Writes a malformed fixture and checks its user-facing error without depending on platform messages.
     *
     * @param contents complete save-file contents
     * @param expectedMessage expected validation error
     * @throws IOException if the temporary fixture cannot be written
     */
    private void assertInvalidFile(String contents, String expectedMessage) throws IOException {
        Path saveFile = tempDir.resolve("invalid.cf");
        Files.writeString(saveFile, contents, StandardCharsets.UTF_8);

        IOException exception = assertThrows(IOException.class, () -> storage.load(saveFile));

        assertEquals(expectedMessage, exception.getMessage());
    }
}
