//@@author camimaigrot
package clubflow;

import clubflow.member.Member;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests member details and validation.
 */
public class MemberTest {

    @Test
    public void constructor_validInputs_storesValuesCorrectly() {
        Member member = new Member(
                "Lucy",
                "lucy123",
                "lucy.club@yahoo.com",
                List.of("president", "publicity")
        );

        assertEquals("Lucy", member.getName());
        assertEquals("lucy123", member.getTelegramHandle());
        assertEquals("lucy.club@yahoo.com", member.getEmail());
        assertEquals(
                List.of("president", "publicity"),
                member.getFunctions()
        );
    }

    @Test
    public void constructor_noFunctions_storesEmptyFunctionList() {
        Member member = new Member(
                "Thomas",
                "tomthomas",
                "thomas.club@gmail.com",
                List.of()
        );

        assertEquals("Thomas", member.getName());
        assertEquals("tomthomas", member.getTelegramHandle());
        assertEquals("thomas.club@gmail.com", member.getEmail());
        assertEquals(List.of(), member.getFunctions());
    }

    @Test
    void constructor_fieldSeparators_rejectsEachMemberField() {
        assertThrows(IllegalArgumentException.class, () -> new Member("Ali|ce", "alice", "", List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Member("Alice", "ali|ce", "", List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new Member("Alice", "alice", "ali|ce@example.com", List.of()));
    }

    @Test
    void constructor_invalidFunctions_rejectsNullBlankAndSaveSeparators() {
        for (String function : Arrays.asList(null, "", "  ", "Host,Guest", "Host|Guest")) {
            assertThrows(IllegalArgumentException.class,
                    () -> new Member("Alice", "alice", "", Arrays.asList("President", function)));
        }
    }

    @Test
    void constructor_mutableFunctions_copiesInputAndExposesImmutableSnapshot() {
        List<String> functions = new ArrayList<>(List.of("President"));
        Member member = new Member("Alice", "alice", "", functions);
        List<String> snapshot = member.getFunctions();

        functions.clear();

        assertEquals(List.of("President"), member.getFunctions());
        assertEquals(List.of("President"), snapshot);
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add("Treasurer"));
    }

    @Test
    void constructor_nullTextFields_rejectsMissingDomainValues() {
        assertThrows(NullPointerException.class, () -> new Member(null, "alice", "", List.of()));
        assertThrows(NullPointerException.class, () -> new Member("Alice", null, "alice@example.com", List.of()));
        assertThrows(NullPointerException.class, () -> new Member("Alice", "alice", null, List.of()));
    }
}
