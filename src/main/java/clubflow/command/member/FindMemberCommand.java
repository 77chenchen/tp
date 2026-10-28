package clubflow.command.member;

import clubflow.command.Command;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import clubflow.Club;
import clubflow.UserInterface;
import clubflow.exception.CommandParseException;
import clubflow.member.Member;

//@@author 77chenchen

/**
 * Searches members by name, contact information, or club function.
 * Direct matches appear first; close name and Telegram spelling matches appear as suggestions.
 */
public class FindMemberCommand extends Command {
    private static final String QUERY = "q";
    private static final int EXACT_SCORE = 300;
    private static final int PREFIX_SCORE = 200;
    private static final int SUBSTRING_SCORE = 100;
    private static final int FUZZY_SCORE_BASE = 50;
    private static final int MAX_FUZZY_QUERY_LENGTH = 64;
    private static final int MAX_SUGGESTIONS = 5;
    private final Club club;

    /** Creates a member search command using the shared club data. */
    public FindMemberCommand(UserInterface ui, Club club) {
        super(ui);
        this.club = Objects.requireNonNull(club, "Club data must not be null");
        addSyntax("q/Keyword [q/Keyword]... [n/Name] [t/TeleHandle] [e/Email] [f/Function]...");
        addSyntax("n/Name");
        addSyntax("t/TeleHandle");
        addSyntax("e/Email");
        addSyntax("f/Function [f/Function]...");
    }

    @Override
    public String[] requiredArgIds() {
        return new String[]{};
    }

    @Override
    public String[] optionalArgIds() {
        return new String[]{QUERY, MemberArgumentIds.NAME, MemberArgumentIds.TELEGRAM,
            MemberArgumentIds.EMAIL, MemberArgumentIds.FUNCTION};
    }

    @Override
    public String[] repeatableArgIds() {
        return new String[]{QUERY, MemberArgumentIds.FUNCTION};
    }

    @Override
    public void validateArgs(HashMap<String, String> args) throws CommandParseException {
        Objects.requireNonNull(args, "Arguments must not be null");
        if (args.isEmpty()) {
            throw new CommandParseException("Provide a search filter: q/, n/, t/, e/, or f/.");
        }
        for (String argId : optionalArgIds()) {
            for (String value : getArgumentValues(args, argId)) {
                if (value.isBlank() || (argId.equals(MemberArgumentIds.TELEGRAM)
                        && value.strip().equals("@"))) {
                    throw new CommandParseException("Provide a non-blank search value after " + argId + "/.");
                }
            }
        }
    }

    @Override
    public boolean execute(HashMap<String, String> args) {
        Objects.requireNonNull(args, "Arguments must not be null");
        if (args.isEmpty()) {
            throw new IllegalArgumentException("At least one search filter is required");
        }
        List<Member> members = club.getMembers();
        List<SearchResult> directMatches = new ArrayList<>();
        List<SearchResult> possibleMatches = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            SearchResult result = searchMember(i + 1, members.get(i), args);
            if (result != null) {
                (result.fuzzy() ? possibleMatches : directMatches).add(result);
            }
        }

        Comparator<SearchResult> byRelevance = Comparator.comparingInt(SearchResult::score).reversed()
                .thenComparingInt(SearchResult::index);
        directMatches.sort(byRelevance);
        possibleMatches.sort(byRelevance);

        if (directMatches.isEmpty() && possibleMatches.isEmpty()) {
            ui.print("No matching members found.");
            return false;
        }
        if (!directMatches.isEmpty()) {
            ui.print("Matching members:");
            directMatches.forEach(result -> printMember(result.index(), result.member()));
        }
        if (!possibleMatches.isEmpty()) {
            ui.print("Possible matches (check details before using a member index):");
            possibleMatches.stream().limit(MAX_SUGGESTIONS)
                    .forEach(result -> printMember(result.index(), result.member()));
        }
        return false;
    }

    /**
     * Applies every supplied filter and records whether any term needed typo tolerance.
     * An unmatched filter rejects the member, so combinations still narrow the results.
     */
    private SearchResult searchMember(int index, Member member, HashMap<String, String> args) {
        int score = 0;
        boolean fuzzy = false;

        for (String keyword : getArgumentValues(args, QUERY)) {
            int termScore = scoreGeneral(member, keyword);
            if (termScore == 0) {
                return null;
            }
            score += termScore;
            fuzzy |= termScore < SUBSTRING_SCORE;
        }

        String name = args.get(MemberArgumentIds.NAME);
        if (name != null) {
            int termScore = scoreName(member.getName(), name);
            if (termScore == 0) {
                return null;
            }
            score += termScore;
            fuzzy |= termScore < SUBSTRING_SCORE;
        }

        String telegram = telegramSearchValue(args);
        if (telegram != null) {
            int termScore = scoreTelegram(member.getTelegramHandle(), telegram);
            if (termScore == 0) {
                return null;
            }
            score += termScore;
            fuzzy |= termScore < SUBSTRING_SCORE;
        }

        String email = args.get(MemberArgumentIds.EMAIL);
        if (email != null) {
            int termScore = scoreDirect(member.getEmail(), email);
            if (termScore == 0) {
                return null;
            }
            score += termScore;
        }

        for (String function : getArgumentValues(args, MemberArgumentIds.FUNCTION)) {
            int termScore = scoreFunctions(member, function);
            if (termScore == 0) {
                return null;
            }
            score += termScore;
        }
        return new SearchResult(index, member, score, fuzzy);
    }

    /** Returns a Telegram search value with an optional leading {@code @} removed. */
    private String telegramSearchValue(HashMap<String, String> args) {
        String value = args.get(MemberArgumentIds.TELEGRAM);
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.startsWith("@") ? trimmed.substring(1) : trimmed;
    }

    /** Finds the strongest match across all searchable fields. */
    private int scoreGeneral(Member member, String keyword) {
        int best = Math.max(scoreName(member.getName(), keyword), scoreTelegram(member.getTelegramHandle(), keyword));
        best = Math.max(best, scoreDirect(member.getEmail(), keyword));
        return Math.max(best, scoreFunctions(member, keyword));
    }

    /** Matches a name directly or against its full spelling and individual words. */
    private int scoreName(String name, String keyword) {
        int direct = scoreDirect(name, keyword);
        return direct > 0 ? direct : scoreFuzzy(name, keyword, true);
    }

    /** Accepts Telegram queries with or without {@code @}. */
    private int scoreTelegram(String handle, String keyword) {
        if (handle.isBlank()) {
            return 0;
        }
        String query = keyword.strip();
        int direct = Math.max(scoreDirect(handle, query), scoreDirect("@" + handle, query));
        if (direct > 0) {
            return direct;
        }
        return scoreFuzzy(handle, query.startsWith("@") ? query.substring(1) : query, false);
    }

    /** Matches functions by case-insensitive exact, prefix, or substring search. */
    private int scoreFunctions(Member member, String keyword) {
        return member.getFunctions().stream().mapToInt(function -> scoreDirect(function, keyword)).max().orElse(0);
    }

    /** Scores direct matches so exact results rank ahead of prefixes and substrings. */
    private int scoreDirect(String field, String keyword) {
        String normalizedField = field.toLowerCase(Locale.ROOT);
        String normalizedQuery = keyword.strip().toLowerCase(Locale.ROOT);
        if (normalizedField.equals(normalizedQuery)) {
            return EXACT_SCORE;
        }
        if (normalizedField.startsWith(normalizedQuery)) {
            return PREFIX_SCORE;
        }
        return normalizedField.contains(normalizedQuery) ? SUBSTRING_SCORE : 0;
    }

    /**
     * Scores spelling suggestions only for useful-length queries. A four- or five-character
     * query allows one edit; longer queries allow two. Very short queries stay strict.
     */
    private int scoreFuzzy(String field, String keyword, boolean compareNameWords) {
        String query = keyword.strip().toLowerCase(Locale.ROOT);
        if (query.length() < 4 || query.length() > MAX_FUZZY_QUERY_LENGTH || field.isBlank()) {
            return 0;
        }
        int allowedEdits = query.length() <= 5 ? 1 : 2;
        String normalizedField = field.toLowerCase(Locale.ROOT);
        int bestDistance = editDistanceWithin(normalizedField, query, allowedEdits);
        if (compareNameWords) {
            for (String word : normalizedField.split("\\s+")) {
                bestDistance = Math.min(bestDistance, editDistanceWithin(word, query, allowedEdits));
            }
        }
        return bestDistance <= allowedEdits ? FUZZY_SCORE_BASE - 10 * bestDistance : 0;
    }

    /**
     * Computes Levenshtein edit distance with bounded memory and stops once the
     * current row cannot reach the accepted distance.
     */
    private int editDistanceWithin(String left, String right, int allowedEdits) {
        if (Math.abs(left.length() - right.length()) > allowedEdits) {
            return allowedEdits + 1;
        }
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            current[0] = i;
            int rowMinimum = current[0];
            for (int j = 1; j <= right.length(); j++) {
                int replacementCost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(previous[j] + 1, current[j - 1] + 1),
                        previous[j - 1] + replacementCost);
                rowMinimum = Math.min(rowMinimum, current[j]);
            }
            if (rowMinimum > allowedEdits) {
                return allowedEdits + 1;
            }
            int[] oldPrevious = previous;
            previous = current;
            current = oldPrevious;
        }
        return previous[right.length()];
    }

    /** Prints the stable member index and details needed to distinguish similar names. */
    private void printMember(int index, Member member) {
        String telegram = member.getTelegramHandle().isBlank() ? "None" : "@" + member.getTelegramHandle();
        String email = member.getEmail().isBlank() ? "None" : member.getEmail();
        String functions = member.getFunctions().isEmpty() ? "None" : String.join(", ", member.getFunctions());

        ui.print(index + ". " + member.getName());
        ui.print("   Telegram: " + telegram);
        ui.print("   Email: " + email);
        ui.print("   Functions: " + functions);
    }

    /** Keeps the original member index alongside its relevance and match type. */
    private record SearchResult(int index, Member member, int score, boolean fuzzy) {
    }
}

//@@author
