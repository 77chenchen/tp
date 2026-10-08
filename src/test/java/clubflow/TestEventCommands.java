package clubflow;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import clubflow.command.CommandParser;
import clubflow.command.CreateEventCommand;
import clubflow.command.DeleteEventCommand;
import clubflow.command.ViewEventCommand;
import clubflow.event.Event;
import clubflow.exception.CommandParseException;

/**
 * Tests event commands using one shared ClubData instance.
 */
public class TestEventCommands {

    @Test
    public void eventCommands_sharedClubData_createViewAndDeleteEvent() throws CommandParseException {
        ClubData clubData = new ClubData();
        CommandParser parser = createEventCommandParser(clubData);

        parser.parse("createEvent e/XMAS n/\"Christmas Party\" d/2026-12-25 "
                + "by/2026-12-20 b/500 v/\"Main Hall\" l/\"Tables and chairs\"");

        Event event = clubData.findEventByTag("XMAS").orElseThrow();
        assertEquals("Christmas Party", event.getName());
        assertEquals(LocalDate.of(2026, 12, 25), event.getDate());
        assertEquals(LocalDate.of(2026, 12, 20), event.getDeadline());
        assertEquals(500.0, event.getBudget());
        assertDoesNotThrow(() -> parser.parse("viewEvent e/XMAS"));
        assertDoesNotThrow(() -> parser.parse("viewEvent all"));

        parser.parse("deleteEvent e/XMAS");
        assertEquals(0, clubData.getEvents().size());
    }

    @Test
    public void createEvent_invalidValues_rejectsCommand() {
        ClubData clubData = new ClubData();
        CommandParser parser = createEventCommandParser(clubData);

        assertAll(
                () -> assertThrows(CommandParseException.class,
                        () -> parser.parse("createEvent e/")),
                () -> assertThrows(CommandParseException.class,
                        () -> parser.parse("createEvent e/XMAS d/25-12-2026")),
                () -> assertThrows(CommandParseException.class,
                        () -> parser.parse("createEvent e/XMAS by/tomorrow")),
                () -> assertThrows(CommandParseException.class,
                        () -> parser.parse("createEvent e/XMAS b/free")),
                () -> assertThrows(CommandParseException.class,
                        () -> parser.parse("createEvent e/XMAS b/-1")),
                () -> assertThrows(CommandParseException.class,
                        () -> parser.parse("deleteEvent e/"))
        );
        assertEquals(0, clubData.getEvents().size());
    }

    @Test
    public void createEvent_duplicateTag_keepsOriginalEvent() throws CommandParseException {
        ClubData clubData = new ClubData();
        CommandParser parser = createEventCommandParser(clubData);

        parser.parse("createEvent e/XMAS n/Original");
        parser.parse("createEvent e/xmas n/Duplicate");

        assertEquals(1, clubData.getEvents().size());
        assertEquals("Original", clubData.getEvents().getFirst().getName());
    }

    /**
     * Creates a parser whose event commands all share the supplied data store.
     *
     * @param clubData data store shared by the event commands
     * @return parser configured with create, view, and delete event commands
     */
    private CommandParser createEventCommandParser(ClubData clubData) {
        UserInterface ui = new UserInterface("[TEST] ", "> ");
        CommandParser parser = new CommandParser(ui);
        parser.register("createEvent", new CreateEventCommand(ui, clubData));
        parser.register("viewEvent", new ViewEventCommand(ui, clubData));
        parser.register("deleteEvent", new DeleteEventCommand(ui, clubData));
        return parser;
    }
}
