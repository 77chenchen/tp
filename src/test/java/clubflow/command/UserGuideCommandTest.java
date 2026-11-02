package clubflow.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import clubflow.UserInterface;
import clubflow.exception.CommandParseException;
import clubflow.exception.UGOpenException;

/** Tests user-guide requests and browser failures without starting an external application. */
class UserGuideCommandTest {
    private final List<String> messages = new ArrayList<>();
    private final UserInterface ui = new UserInterface("", "") {
        @Override
        public void print(String message) {
            messages.add(message);
        }
    };

    @Test
    void execute_browserAvailable_opensPublishedGuideOnceAndPrintsSuccess() throws Exception {
        List<URI> opened = new ArrayList<>();
        UserGuideCommand command = new UserGuideCommand(ui, opened::add);

        assertFalse(command.execute(new HashMap<>()));

        assertEquals(List.of(URI.create("https://ay2627s1-cs2113-w10-2.github.io/tp/UserGuide.html")), opened);
        assertEquals(List.of("Opened user guide in browser."), messages);
    }

    @Test
    void execute_browserIoFailure_throwsUserFacingExceptionWithoutSuccessMessage() {
        List<URI> attempts = new ArrayList<>();
        UserGuideCommand command = new UserGuideCommand(ui, uri -> {
            attempts.add(uri);
            throw new IOException("Simulated browser failure");
        });

        UGOpenException error = assertThrows(UGOpenException.class, () -> command.execute(new HashMap<>()));

        assertEquals(1, attempts.size());
        assertEquals("Failed to open user guide on browser: An IO error occurred.", error.getMessage());
        assertTrue(messages.isEmpty());
    }

    @Test
    void parse_guideAlias_executesWithoutArgumentsAndRejectsUnexpectedArguments() throws Exception {
        List<URI> opened = new ArrayList<>();
        CommandParser parser = new CommandParser(ui);
        parser.register("userGuide", new UserGuideCommand(ui, opened::add));
        parser.registerAlias("userGuide", "ug");

        assertFalse(parser.parse("ug"));
        assertThrows(CommandParseException.class, () -> parser.parse("ug p/unexpected"));

        assertEquals(1, opened.size());
        assertEquals(List.of("Opened user guide in browser."), messages);
    }
}
