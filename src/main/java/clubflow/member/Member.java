//@@author camimaigrot
package clubflow.member;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a member of the club.
 */
public class Member {
    private final String name;
    private final String telegramHandle;
    private final String email;
    private final ArrayList<String> functions;

    //@@author Eugene3129
    /**
     * Creates a member.
     *
     * @param name Name of the member.
     * @param telegramHandle Telegram handle of the member.
     * @param email Email address of the member.
     * @param functions Functions held by the member in the club.
     */
    public Member(String name, String telegramHandle, String email,
                  List<String> functions) {
        this.name = requireNonBlank(name, "Member name");
        this.telegramHandle = Objects.requireNonNull(telegramHandle, "Telegram handle must not be null");
        this.email = Objects.requireNonNull(email, "Email must not be null");
        Objects.requireNonNull(functions, "Functions must not be null");

        if (telegramHandle.isBlank() && email.isBlank()) {
            throw new IllegalArgumentException("A member must have at least one contact");
        }
        if (telegramHandle.startsWith("@")) {
            throw new IllegalArgumentException("Telegram handle must not start with @");
        }
        if (containsFieldSeparator(name) || containsFieldSeparator(telegramHandle)
                || containsFieldSeparator(email)) {
            throw new IllegalArgumentException("Member fields must not contain |");
        }
        if (functions.stream().anyMatch(Objects::isNull)
                || functions.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("Functions must not contain null or blank values");
        }
        if (functions.stream().anyMatch(function -> function.contains(",") || containsFieldSeparator(function))) {
            throw new IllegalArgumentException("Functions must not contain ',' or '|'");
        }

        this.functions = new ArrayList<>(functions);
        assert this.functions.size() == functions.size() : "All functions must be copied";
    }

    /**
     * Rejects a null or blank required value.
     *
     * @param value value to check
     * @param description field name used in the exception message
     * @return the validated value
     */
    private static String requireNonBlank(String value, String description) {
        Objects.requireNonNull(value, description + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(description + " must not be blank");
        }
        return value;
    }

    private static boolean containsFieldSeparator(String value) {
        return value.contains("|");
    }

    //@@author camimaigrot
    public String getName() {
        return name;
    }

    public String getTelegramHandle() {
        return telegramHandle;
    }

    public String getEmail() {
        return email;
    }

    public List<String> getFunctions() {
        return List.copyOf(functions);
    }
}
