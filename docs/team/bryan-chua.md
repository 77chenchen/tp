# Bryan Chua's Project Portfolio Page

### Project: ClubFlow

ClubFlow is a command-line application written in Java that helps student clubs manage events, members, and related information.

Given below are my contributions to the project.

- **Code contributed**: [tP Code Dashboard](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/?search=Bryan-chua&sort=groupTitle&sortWithin=title&timeframe=commit&mergegroup=&groupSelect=groupByRepos&breakdown=true&checkedFileTypes=docs~functional-code~test-code~other)

- **New Feature: Saving and Loading Club Data** ([implementation](https://github.com/AY2627S1-CS2113-W10-2/tp/commit/3951aa2822597e2df283f65c47f3d8a3602ecc33))
    - **What it does:** Allows users to preserve member and event records between sessions and import club data using `load p/PATH`.
    - **Justification:** Enables organisers to continue managing their clubs without recreating their records each time they start the application.
    - **Highlights:** Implemented the original storage component, initially named `Storage` and later renamed `SaveLoadManager`, together with the save and load commands. Used UTF-8 text records with field separators, including conversion of dates, budgets, and member functions. Added validation for malformed records and loaded data into a separate store before replacing the active records. The initial manual save command was later replaced by autosave.

- **New Feature: Automatic Saving and Startup Loading** ([PR #48](https://github.com/AY2627S1-CS2113-W10-2/tp/pull/48))
    - **What it does:** Saves changes automatically after commands and restores saved club data when the application starts.
    - **Justification:** Reduces repetitive administration and the risk of users forgetting to save their changes.
    - **Highlights:** Introduced unsaved-change tracking and connected it to member and event operations. Integrated saving into the application loop, clearing the flag only after a successful save. Failed saves retain the in-memory changes for a later retry and display a warning. Added a configurable save path so application tests can use temporary files. Teammates subsequently extended this foundation for multiple clubs, assignments, and photos.

- **New Feature: Event Invitation Generation** ([PR #53](https://github.com/AY2627S1-CS2113-W10-2/tp/pull/53))
    - **What it does:** Generates an invitation from an existing event using `invite e/TAG`, offers to fill in missing details, and automatically copies the invitation to the clipboard.
    - **Justification:** Saves organisers from repeatedly typing event details when preparing invitations to share with members.
    - **Highlights:** Implemented `InviteCommand`, connected it to the application's command registration, and added `printWithoutPrefix()` to the user interface. Invitations use the event's name, date, and venue. Added automatic clipboard copying with a manual-copy fallback when clipboard access fails ([change](https://github.com/AY2627S1-CS2113-W10-2/tp/commit/3a23e177f8ec8e3c98468466a35f6e1eefced596)). Added warnings and Y/N confirmation for missing details, followed by validated prompts for only the missing fields ([change](https://github.com/AY2627S1-CS2113-W10-2/tp/commit/f618bc9c418626afe1b075d7f00c4ed683e2eb36)). Completed edits are applied together and saved through autosave while preserving other event details, completion state, photos, and assignments. Declining the prompts leaves the event unchanged and uses placeholders.

- **New Feature: Restoring the Last Active Club** ([implementation](https://github.com/AY2627S1-CS2113-W10-2/tp/commit/22adb41b0c45b2e2480d1cd547ed63d773d5c327))
    - **What it does:** Remembers the last successfully selected club and restores it when the application restarts, displaying the active club ID at startup.
    - **Justification:** Lets organisers managing several clubs resume their work without selecting the same club again on every launch.
    - **Highlights:** Extended `ClubManager` to store the normalised club ID in `saves/clubs/last-active.txt` after a successful switch, including switches back to `default`. Startup validates the preference and loads the remembered club, retaining the default club and logging a warning if the preference or remembered club cannot be loaded. Preference-write failures are logged without undoing a successful switch. Creating a club alone does not change the startup preference.

- **New Feature: Command Spelling Correction** ([PR #56](https://github.com/AY2627S1-CS2113-W10-2/tp/pull/56))
    - **What it does:** Suggests a likely intended command when users mistype a command name, then lets them confirm or cancel it.
    - **Justification:** Helps users recover from typing mistakes without re-entering the entire command and its arguments.
    - **Highlights:** Implemented an edit-distance algorithm and closest-command selection in `CommandParser`. Applied limits based on command length, rejected equally close suggestions, and excluded aliases from fuzzy matching. Preserved argument text, including quoted values. Added the confirmation prompt and integrated correction into `ClubFlow.run()` before normal parsing, so confirmed commands still undergo argument validation. A teammate later enhanced the preservation of registered command casing.

- **Enhancements to Existing Features**:
    - Added storage-format validation to reject `|` in member and event fields and commas in member functions, preventing values from conflicting with save-file separators.
    - Added the original `replaceWith()` logic to replace loaded member and event records while retaining the shared data-store object already referenced by commands.
    - Adapted application tests from manual saving to autosave, including restoration across separate application sessions and completion-state persistence.
    - Added parser tests for common typos, case-insensitive matching, quoted arguments, exact commands and aliases, short or distant inputs, tied suggestions, and closest-match selection.
    - Added six application-level correction tests covering confirmation, cancellation, repeated invalid confirmation input, argument validation after correction, exact commands, and end-of-input during confirmation.
    - Added `InviteCommandTest` coverage for clipboard copying and failures, missing-detail prompts, Y/N handling, field validation, preservation of existing event data, and interrupted input. Injected a replaceable clipboard operation so tests can verify copied text without accessing the system clipboard.
    - Added `InvitationPersistenceTest` using separate headless application sessions and temporary directories to verify that invitation edits are autosaved and restored after restart.
    - Added `ClubManagerLastActiveClubTest` for preference normalisation, restoration, fallback, failed switches, and preference-write failures. Extended `ClubFlowMultiClubTest` to check the restored club's startup message and event data.
    - Updated CI to use Intel macOS runners to address ARM64 runner-capacity constraints ([change](https://github.com/AY2627S1-CS2113-W10-2/tp/commit/7bbd8d3)).

- **Documentation**:
    - **User Guide:** Updated instructions and examples when manual saving was replaced by autosave. Explained startup loading, importing saved data, save-failure warnings and retries, and saving on exit. Updated related event-editing and completion instructions. Documented invitation generation, clipboard copying and fallback, missing-detail prompts and validation, and restoration of the last active club.
    - **Developer Guide:** Expanded the storage component and autosave/import/file-format explanations. Documented invitation prompting, event updates, clipboard integration, and startup preference storage and recovery. Updated related user stories, use cases, requirements, automated-test descriptions, and manual testing instructions ([storage documentation](https://github.com/AY2627S1-CS2113-W10-2/tp/commit/0d6210d83daa3cfffbcf7aba84c482ba9e806371), [latest guide updates](https://github.com/AY2627S1-CS2113-W10-2/tp/commit/c21145795531528e5f9e4da7ddbfdeb176c311b5)).

- **Team-based Contributions**:
    - Provided the original persistence foundation used by the team's member and event workflows and later extended for assignments, photos, and multiple clubs.
    - Integrated storage, autosave, invitations, and command correction with shared application components rather than leaving them as standalone commands.
    - Resolved merge conflicts while integrating the autosave changes with teammates' work.
    - Added scoped RepoSense author annotations to identify my contributions within shared files while retaining credit for teammates' later additions.