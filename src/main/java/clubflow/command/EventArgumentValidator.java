package clubflow.command;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;

import clubflow.exception.CommandParseException;

//@@author Eugene3129
/**
 * Shares event field validation between creation and editing commands.
 */
final class EventArgumentValidator {
    private EventArgumentValidator() {
        // This utility only provides validation and does not hold command state.
    }

    /**
     * Validates the required tag and any supplied event details before the store is changed.
     *
     * @param args parsed arguments containing a tag and optional event fields
     * @throws CommandParseException if a tag, date, budget, or file-format separator is invalid
     */
    static void validate(HashMap<String, String> args) throws CommandParseException {
        if (args.get(EventArgumentIds.TAG).isBlank()) {
            throw new CommandParseException("An event tag must be provided after e/.");
        }

        for (String value : args.values()) {
            if (value.contains("|")) {
                throw new CommandParseException("Fields cannot contain |.");
            }
        }

        validateDate(args, EventArgumentIds.DATE, "Event date");
        validateDate(args, EventArgumentIds.DEADLINE, "Deadline");
        validateBudget(args);
    }

    /**
     * Validates an optional ISO-8601 date argument.
     *
     * @param args parsed command arguments
     * @param argId ID of the date argument
     * @param description user-facing name of the date
     * @throws CommandParseException if the date is not in YYYY-MM-DD format
     */
    private static void validateDate(HashMap<String, String> args, String argId, String description)
            throws CommandParseException {
        if (!args.containsKey(argId)) {
            return;
        }
        try {
            LocalDate.parse(args.get(argId));
        } catch (DateTimeParseException e) {
            throw new CommandParseException(description + " must use YYYY-MM-DD format.");
        }
    }

    /**
     * Validates an optional budget as a finite, non-negative number.
     *
     * @param args parsed command arguments
     * @throws CommandParseException if the supplied budget is invalid
     */
    private static void validateBudget(HashMap<String, String> args) throws CommandParseException {
        if (!args.containsKey(EventArgumentIds.BUDGET)) {
            return;
        }
        try {
            double budget = Double.parseDouble(args.get(EventArgumentIds.BUDGET));
            if (!Double.isFinite(budget) || budget < 0) {
                throw new CommandParseException("Budget must be a non-negative number.");
            }
        } catch (NumberFormatException e) {
            throw new CommandParseException("Budget must be a non-negative number.");
        }
    }
}
