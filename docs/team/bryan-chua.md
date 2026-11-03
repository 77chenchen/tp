# Bryan Chua's Project Portfolio Page

### Project: ClubFlow

ClubFlow is a Java command-line application for managing student-club events, members, and related information. My contributions are listed below.

- **Code contributed**: [tP Code Dashboard](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/?search=Bryan-chua&sort=groupTitle&sortWithin=title&timeframe=commit&mergegroup=&groupSelect=groupByRepos&breakdown=true&checkedFileTypes=docs~functional-code~test-code~other)

- **New Feature: Saving and Loading Club Data** ([implementation](https://github.com/AY2627S1-CS2113-W10-2/tp/commit/3951aa2822597e2df283f65c47f3d8a3602ecc33))
    - Preserves member and event records between sessions and imports club data using `load p/PATH`, so users need not recreate records.
    - Implemented the original storage component (later renamed `SaveLoadManager`) and save/load commands. It serialises dates, budgets, and member functions as UTF-8 text records, validates malformed records, and loads into a separate store before replacing active data. The initial manual save was later replaced by autosave.

- **New Feature: Automatic Saving and Startup Loading** ([PR #48](https://github.com/AY2627S1-CS2113-W10-2/tp/pull/48))
    - Automatically saves command changes and restores data at startup, reducing repetitive work and forgotten saves.
    - Introduced unsaved-change tracking for member and event operations. The application clears this state only after a successful save; failures preserve in-memory changes for retry and display a warning. A configurable save path enables isolated tests. Teammates later extended this foundation for multiple clubs, assignments, and photos.

- **New Feature: Event Invitation Generation** ([PR #53](https://github.com/AY2627S1-CS2113-W10-2/tp/pull/53))
    - `invite e/TAG` generates an invitation from an event, optionally fills missing details, and copies it to the clipboard, saving organisers from retyping information.
    - Implemented `InviteCommand`, command registration, and unprefixed UI output. Added clipboard copying with a manual fallback ([change](https://github.com/AY2627S1-CS2113-W10-2/tp/commit/3a23e177f8ec8e3c98468466a35f6e1eefced596)), plus confirmation and validated prompts for missing fields ([change](https://github.com/AY2627S1-CS2113-W10-2/tp/commit/f618bc9c418626afe1b075d7f00c4ed683e2eb36)). Accepted edits are applied atomically and autosaved without losing other event data; declining leaves the event unchanged and uses placeholders.

- **New Feature: Restoring the Last Active Club** ([implementation](https://github.com/AY2627S1-CS2113-W10-2/tp/commit/22adb41b0c45b2e2480d1cd547ed63d773d5c327))
    - Remembers the last selected club across restarts, allowing multi-club organisers to resume work immediately.
    - Extended `ClubManager` to store the normalised club ID after successful switches. Startup validates and loads that preference, retaining the default club and warning on invalid or unloadable data. Preference-write failures do not undo a successful switch, and creating a club alone does not alter the preference.

- **New Feature: Command Spelling Correction** ([PR #56](https://github.com/AY2627S1-CS2113-W10-2/tp/pull/56))
    - Suggests and confirms likely commands after a typo, avoiding re-entry of the command and its arguments.
    - Implemented edit-distance-based selection in `CommandParser`, with length-based limits, rejection of tied suggestions, and aliases excluded from fuzzy matching. Argument text, including quoted values, is preserved. Correction occurs before normal parsing, so confirmed commands still undergo argument validation.

- **Enhancements to Existing Features**:
    - Added validation preventing field values from conflicting with save-file separators, plus `replaceWith()` logic that updates loaded records while retaining the shared data-store object referenced by commands.
    - Added unit and application tests for autosave/restoration, command correction and confirmation, invitation prompting/clipboard failures/data preservation, and last-active-club restoration/fallback. Tests use temporary directories, separate headless sessions, and an injectable clipboard operation to avoid external side effects.
    - Updated CI to use Intel macOS runners to address ARM64 runner-capacity constraints ([change](https://github.com/AY2627S1-CS2113-W10-2/tp/commit/7bbd8d3)).

- **Documentation**:
    - **User Guide:** Documented autosave, startup loading, data import, failure recovery, invitation generation and validation, clipboard fallback, and last-active-club restoration.
    - **Developer Guide:** Documented storage, autosave/import/file format, invitation updates and clipboard integration, and startup-preference recovery. Updated related stories, use cases, requirements, and testing instructions ([storage documentation](https://github.com/AY2627S1-CS2113-W10-2/tp/commit/0d6210d83daa3cfffbcf7aba84c482ba9e806371), [latest updates](https://github.com/AY2627S1-CS2113-W10-2/tp/commit/c21145795531528e5f9e4da7ddbfdeb176c311b5)).

- **Team-based Contributions**:
    - Provided the persistence foundation later extended for assignments, photos, and multiple clubs, and integrated storage, autosave, invitations, and correction with shared components.
    - Resolved autosave merge conflicts and added scoped RepoSense annotations that distinguish my work in shared files while preserving teammates' credit.
