# Chan Ray's Project Portfolio Page

### Project: ClubFlow

ClubFlow is a command-line application written in Java that helps student clubs manage events, members, and related information.

Given below are my contributions to the project.

- **New Feature: Command Parsing Framework**
    - What it does: Provides a centralized framework for registering and executing commands, parsing arguments, and validating user input.
    - Justification: Allows developers to implement new commands without duplicating parsing and validation logic.
    - Highlights: Supports required/optional arguments, quoted values, and comprehensive error handling. Designed the `Command`, `CommandParser`, and `UserInterface` classes as the foundation for the application's command system.

- **New Feature: Command Aliases and Help System**
    - What it does: Allows commands to have multiple aliases and provides a `help` command to display available commands and their syntax.
    - Justification: Improves usability by helping users discover commands and correct invalid inputs.
    - Highlights: Implemented `registerAlias()` and `addSyntax()`, supporting case-insensitive aliases, alias conflict detection, and automatic syntax display on invalid inputs. Integrated the syntax system into existing commands.

- **New Feature: Event Photo Management**
    - What it does: Allows users to import, open, list, and delete event photos, including bulk importing from directories.
    - Justification: Enables clubs to organize and preserve event-related photos directly within ClubFlow.
    - Highlights: Implemented automatic PNG conversion, event-specific photo storage, and integration with `SaveLoadManager` and autosave. Required handling filesystem operations, image processing, persistence, and data consistency across event modifications.

- **Code contributed**: [tP Code Dashboard](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/?search=czfray&sort=groupTitle&sortWithin=title&timeframe=commit&mergegroup=&groupSelect=groupByRepos&breakdown=true&checkedFileTypes=docs~functional-code~test-code~other&since=2026-09-18T00%3A00%3A00&filteredFileName=)

- **Enhancements to existing features**:
    - Added JUnit tests for command parsing and photo management.
    - Fixed parsing edge cases involving empty quoted arguments.
    - Fixed photo data loss when editing, marking, or unmarking events.
    - Improved photo persistence to prevent deleted photos from reappearing after loading.
    - Standardized command error messages and output formatting.

- **Documentation**:
    - **User Guide**:
        - Documented photo management commands, syntax, and usage examples.
        - Added documentation for bulk photo importing and automatic photo naming.
    - **Developer Guide**:

- **Team-based contributions**:
    - Developed the shared command framework used by teammates to implement their commands.
    - Integrated command syntax definitions into existing commands.
    - Provided implementation guides for the command parsing, alias, and syntax systems.
    - Integrated photo management with existing event management and persistence functionality.

- **Review and mentoring contributions**:
    - Reviewed [PR #46](https://github.com/AY2627S1-CS2113-W10-2/tp/pull/46), providing feedback on command implementations, JUnit tests, and User Guide documentation.
    - Provided reference implementations and guidance to teammates on using the command parsing, alias, and syntax systems.

- **Use of AI tools**:
    - Used ChatGPT to assist in identifying edge cases and generating JUnit tests for the photo management system. Given the numerous possible failure scenarios involving file operations, image processing, and data persistence, ChatGPT helped improve test coverage and identify potential issues.