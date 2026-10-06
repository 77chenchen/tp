package clubflow.member;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a member of the club.
 */
public class Member {
    private final String name;
    private final String telegramHandle;
    private final String email;
    private final ArrayList<String> functions;

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
        this.name = name;
        this.telegramHandle = telegramHandle;
        this.email = email;
        this.functions = new ArrayList<>(functions);
    }

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
        return new ArrayList<>(functions);
    }
}
