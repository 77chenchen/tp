package clubflow.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import clubflow.Club;
import clubflow.ProjectTempDirectory;
import clubflow.SaveLoadManager;
import clubflow.UserInterface;
import clubflow.command.event.ViewEventCommand;
import clubflow.event.Event;
import clubflow.exception.CommandParseException;
import clubflow.member.Member;

/**
 * Verifies that loading replaces shared data only after the whole file has been validated.
 */
class LoadCommandTest {
    @TempDir(factory = ProjectTempDirectory.class)
    Path testDirectory;

    private final PrintStream originalOutput = System.out;
    private ByteArrayOutputStream output;
    private PrintStream capturedOutput;
    private Club club;
    private Event originalEvent;
    private CommandParser parser;

    @BeforeEach
    void setUp() {
        output = new ByteArrayOutputStream();
        capturedOutput = new PrintStream(output, true, StandardCharsets.UTF_8);
        System.setOut(capturedOutput);
        club = new Club();
        originalEvent = new Event("ORIGINAL", "Existing event", null, null, 0, "", "");
        club.addEvent(originalEvent);
        club.markSaved();
        UserInterface ui = new UserInterface("[TEST] ", "> ");
        parser = new CommandParser(ui);
        parser.register("load", new LoadCommand(ui, club));
        parser.register("viewEvent", new ViewEventCommand(ui, club));
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOutput);
        capturedOutput.close();
    }

    @Test
    void load_validFile_replacesEventsMembersAndAssignmentsInSharedStore() throws Exception {
        Club imported = new Club();
        Member member = new Member("Alice", "alice", "", List.of());
        imported.addMember(member);
        imported.addEvent(new Event("IMPORTED", "Imported event", null, null, 50, "Hall", ""));
        imported.assignMemberToEvent("IMPORTED", member, "Host");
        Path source = testDirectory.resolve("import.cf");
        new SaveLoadManager().save(imported, source);

        assertFalse(parser.parse(loadInput(source)));
        assertFalse(parser.parse("viewEvent e/IMPORTED"));

        assertTrue(club.findEventByTag("ORIGINAL").isEmpty());
        assertEquals("Alice", club.getMembers().getFirst().getName());
        assertEquals("Host", club.getAssignments().getFirst().getRole());
        assertSame(club.getMembers().getFirst(), club.getAssignments().getFirst().getMember());
        assertSame(club.getEvents().getFirst(), club.getAssignments().getFirst().getEvent());
        assertTrue(club.hasUnsavedChanges(), "Imported data must be eligible for autosaving to the active club");
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Data loaded successfully!"));
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("[IMPORTED] Imported event"));
    }

    @Test
    void load_emptyFile_clearsExistingData() throws Exception {
        Path source = Files.createFile(testDirectory.resolve("empty.cf"));

        assertFalse(parser.parse(loadInput(source)));

        assertTrue(club.getEvents().isEmpty());
        assertTrue(club.getMembers().isEmpty());
        assertTrue(club.getAssignments().isEmpty());
        assertTrue(club.hasUnsavedChanges());
    }

    @Test
    void load_corruptRecordAfterValidRecord_preservesExistingData() throws Exception {
        Path source = testDirectory.resolve("corrupt.cf");
        String content = "EVENT|PARTIAL|Partial|null|null|0.0||\nEVENT|BROKEN\n";
        Files.writeString(source, content);

        assertFalse(parser.parse(loadInput(source)));

        assertEquals(List.of(originalEvent), club.getEvents());
        assertFalse(club.hasUnsavedChanges());
        assertEquals(content, Files.readString(source));
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Data not loaded!"));
        assertFalse(output.toString(StandardCharsets.UTF_8).contains("Data loaded successfully!"));
    }

    @Test
    void load_missingFile_reportsFailureWithoutCreatingFile() throws Exception {
        Path source = testDirectory.resolve("missing.cf");

        assertFalse(parser.parse(loadInput(source)));

        assertFalse(Files.exists(source));
        assertEquals(List.of(originalEvent), club.getEvents());
        assertFalse(club.hasUnsavedChanges());
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Data not loaded!"));
    }

    @Test
    void load_directory_reportsFailureAndPreservesExistingData() throws Exception {
        assertFalse(parser.parse(loadInput(testDirectory)));

        assertEquals(List.of(originalEvent), club.getEvents());
        assertFalse(club.hasUnsavedChanges());
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Data not loaded!"));
    }

    @Test
    void load_invalidPaths_rejectsBeforeChangingData() {
        for (String input : List.of("load", "load p/\"\"", "load p/\"   \"",
                "load p/\"" + testDirectory.toAbsolutePath() + "\"", "load p/\"bad\0path\"")) {
            assertThrows(CommandParseException.class, () -> parser.parse(input), input);
            assertEquals(List.of(originalEvent), club.getEvents());
            assertFalse(club.hasUnsavedChanges());
        }
    }

    @Test
    void execute_nullArguments_rejectsInvalidCall() {
        LoadCommand command = new LoadCommand(new UserInterface("", ""), club);

        assertThrows(NullPointerException.class, () -> command.execute(null));
    }

    /**
     * Quotes a relative path so directories containing spaces also work through the parser.
     */
    private String loadInput(Path path) {
        Path relative = Path.of("").toAbsolutePath().relativize(path.toAbsolutePath());
        return "load p/\"" + relative + "\"";
    }
}
