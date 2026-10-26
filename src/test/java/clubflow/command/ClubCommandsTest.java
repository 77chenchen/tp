package clubflow;

import clubflow.command.CommandParser;
import clubflow.command.CreateClubCommand;
import clubflow.command.CurrentClubCommand;
import clubflow.command.ListClubsCommand;
import clubflow.command.SwitchClubCommand;
import clubflow.exception.ClubNotFoundException;
import clubflow.exception.CommandParseException;
import clubflow.exception.DuplicateClubException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the four multi-club commands through their public interfaces. */
class ClubCommandsTest {
    @TempDir
    Path testDirectory;

    private final PrintStream originalOutput = System.out;
    private ByteArrayOutputStream capturedBytes;
    private PrintStream capturedOutput;
    private ClubManager manager;
    private CommandParser parser;

    @BeforeEach
    void setUp() {
        capturedBytes = new ByteArrayOutputStream();
        capturedOutput = new PrintStream(capturedBytes, true, StandardCharsets.UTF_8);
        System.setOut(capturedOutput);

        UserInterface ui = new UserInterface("[CF] ", "> ");
        manager = new ClubManager(new Club(), testDirectory.resolve("default.cf"));
        parser = new CommandParser(ui);
        parser.register("createClub", new CreateClubCommand(ui, manager));
        parser.register("switchClub", new SwitchClubCommand(ui, manager));
        parser.register("listClubs", new ListClubsCommand(ui, manager));
        parser.register("currentClub", new CurrentClubCommand(ui, manager));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOutput);
        capturedOutput.close();
    }

    private String output() {
        return capturedBytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    void commands_argumentMetadata_isCorrect() {
        UserInterface ui = new UserInterface("[CF] ", "> ");
        CreateClubCommand createCommand = new CreateClubCommand(ui, manager);
        SwitchClubCommand switchCommand = new SwitchClubCommand(ui, manager);
        ListClubsCommand listCommand = new ListClubsCommand(ui, manager);
        CurrentClubCommand currentCommand = new CurrentClubCommand(ui, manager);

        assertArrayEquals(new String[]{"n"}, createCommand.requiredArgIds());
        assertArrayEquals(new String[]{}, createCommand.optionalArgIds());
        assertArrayEquals(new String[]{"n"}, switchCommand.requiredArgIds());
        assertArrayEquals(new String[]{}, switchCommand.optionalArgIds());
        assertArrayEquals(new String[]{}, listCommand.requiredArgIds());
        assertArrayEquals(new String[]{}, listCommand.optionalArgIds());
        assertArrayEquals(new String[]{}, currentCommand.requiredArgIds());
        assertArrayEquals(new String[]{}, currentCommand.optionalArgIds());
    }

    @Test
    void createClub_validName_createsDirectoryAndPrintsSuccess() throws Exception {
        assertFalse(parser.parse("createClub n/Chess"));

        assertTrue(Files.isRegularFile(manager.getClubFile("chess")));
        assertTrue(output().contains("Created club: Chess"));
    }

    @Test
    void createClub_missingOrInvalidName_rejectsInput() {
        assertThrows(CommandParseException.class, () -> parser.parse("createClub"));
        assertThrows(CommandParseException.class, () -> parser.parse("createClub n/two words"));
        assertThrows(CommandParseException.class, () -> parser.parse("createClub n/../bad"));
        assertThrows(CommandParseException.class, () -> parser.parse("createClub n/\"\""));
    }

    @Test
    void createClub_duplicateName_throwsDomainException() throws Exception {
        parser.parse("createClub n/Chess");

        assertThrows(DuplicateClubException.class, () -> parser.parse("createClub n/chess"));
    }

    @Test
    void createClub_ioFailure_printsErrorAndContinues() throws Exception {
        Files.createFile(testDirectory.resolve("clubs"));

        assertFalse(parser.parse("createClub n/Chess"));
        assertTrue(output().contains("Could not create club:"));
    }

    @Test
    void listClubs_emptyAndPopulated_showsActiveClubMarker() throws Exception {
        assertFalse(parser.parse("listClubs"));
        assertTrue(output().contains("No saved clubs found."));

        manager.initialize();
        parser.parse("createClub n/chess");
        assertFalse(parser.parse("listClubs"));
        assertTrue(output().contains("default (active)"));
        assertTrue(output().contains("chess"));
    }

    @Test
    void listClubs_unreadableStorage_doesNotInventClubs() throws Exception {
        Files.createFile(testDirectory.resolve("clubs"));

        assertFalse(parser.parse("listClubs"));
        assertTrue(output().contains("No saved clubs found."));
    }

    @Test
    void currentClub_beforeAndAfterSwitch_printsCorrectClub() throws Exception {
        manager.initialize();
        parser.parse("createClub n/chess");

        assertFalse(parser.parse("currentClub"));
        assertFalse(parser.parse("switchClub n/Chess"));
        assertFalse(parser.parse("currentClub"));

        assertTrue(output().contains("Active club: default"));
        assertTrue(output().contains("Active club: chess"));
    }

    @Test
    void switchClub_missingOrInvalidName_rejectsCommand() throws Exception {
        manager.initialize();

        assertThrows(CommandParseException.class, () -> parser.parse("switchClub"));
        assertThrows(CommandParseException.class, () -> parser.parse("switchClub n/../bad"));
        assertThrows(ClubNotFoundException.class, () -> parser.parse("switchClub n/unknown"));
    }

    @Test
    void switchClub_corruptSave_printsIoFailure() throws Exception {
        manager.initialize();
        parser.parse("createClub n/chess");
        Files.writeString(manager.getClubFile("chess"), "BROKEN|record");

        assertFalse(parser.parse("switchClub n/chess"));

        assertTrue(output().contains("Could not switch club:"));
        assertFalse(output().contains("Active club: chess"));
    }

    @Test
    void commands_executeDirectly_useCorrectReturnValues() throws Exception {
        UserInterface ui = new UserInterface("[CF] ", "> ");
        CreateClubCommand createCommand = new CreateClubCommand(ui, manager);
        ListClubsCommand listCommand = new ListClubsCommand(ui, manager);
        SwitchClubCommand switchCommand = new SwitchClubCommand(ui, manager);
        CurrentClubCommand currentCommand = new CurrentClubCommand(ui, manager);
        manager.initialize();

        assertFalse(createCommand.execute(new HashMap<>(java.util.Map.of("n", "music"))));
        assertFalse(listCommand.execute(new HashMap<>()));
        assertFalse(switchCommand.execute(new HashMap<>(java.util.Map.of("n", "music"))));
        assertFalse(currentCommand.execute(new HashMap<>()));
        assertFalse(output().contains("music (active)"));
    }
}
