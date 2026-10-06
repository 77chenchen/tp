package clubflow.member;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestMember {

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
}