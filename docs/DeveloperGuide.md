# Developer Guide

ClubFlow is a Java command-line application for helping student clubs manage
events, members, and related information. This guide describes the current implementation;
command examples are also available in the [User Guide](UserGuide.md).

## Contents

* [Acknowledgements](#acknowledgements)
* [Setting up, getting started](#setting-up-getting-started)
* [Design](#design)
* [Implementation](#implementation)
* [Documentation, logging, testing, and development workflow](#documentation-logging-testing-and-development-workflow)
* [Appendix: Requirements](#appendix-requirements)
* [Appendix: Instructions for manual testing](#appendix-instructions-for-manual-testing)

## Acknowledgements

* The organisation of this guide follows the
  [AddressBook Level 3 Developer Guide](https://se-education.org/addressbook-level3/DeveloperGuide.html) structure.
* [JUnit 5](https://junit.org/junit5/) provides the automated test framework.
* [Gradle](https://gradle.org/), [Shadow](https://gradleup.com/shadow/), and
  [Checkstyle](https://checkstyle.org/) support building, packaging, and style checks.
* [PlantUML](https://plantuml.com/) is used for UML diagrams.

## Setting up, getting started

Use JDK **25**, Git, and an IDE with Gradle support. Import the repository as a
Gradle project and select JDK 25 for both the project SDK and the Gradle JVM.
Run commands below from the repository root. On Windows, use PowerShell:

```powershell
java -version
.\gradlew.bat classes
java -ea -cp build/classes/java/main clubflow.ClubFlow
```

`classes` compiles the application. `-ea` enables assertions that check internal
assumptions. On macOS or Linux, replace `.\gradlew.bat` with `./gradlew`.
Alternatively, run `clubflow.ClubFlow.main()` in the IDE with assertions enabled.

To build the packaged application:

```powershell
.\gradlew.bat shadowJar
java -ea -cp build/libs/clubflow.jar clubflow.ClubFlow
```

**Current build configuration issue:** `build.gradle` sets `application.mainClass`
to `flow.ClubFlow`, but the actual entry point is `clubflow.ClubFlow`. Until that
setting is corrected, use the explicit classpath commands above rather than
`gradlew run` or `java -jar`. The root README also contains starter instructions
that do not describe the current application.

Paths such as `saves/`, `cache/`, and `clubflow.log` are relative to the process's
working directory. Launch from a separate folder for manual testing to avoid
changing development data.

## Design

### Architecture

<!-- Diagram widths use intrinsic pixels / 16 in em to match the body font size. -->
<div style="overflow-x: auto;">
  <a href="diagrams/Architecture.png"><img src="diagrams/Architecture.png" alt="ClubFlow architecture: logical view" style="width: 19.0625em; max-width: none; height: auto;"></a>
</div>

[PlantUML source](diagrams/Architecture.puml).

The following are logical responsibilities, not separate modules or a set of
component interfaces. Most core classes are in the `clubflow` package.

| Responsibility | Main classes | Purpose |
| --- | --- | --- |
| Application coordination | `ClubFlow` | Constructs and connects objects, loads startup data, runs the input loop, handles errors, and triggers autosave. |
| User interaction | `UserInterface` | Reads input and prints prompts, results, confirmation messages, and invitations. |
| Command processing | `CommandParser`, `Command`, concrete command classes, search and completion helpers | Resolves keywords and aliases, validates arguments, and executes requests, including event filtering and completion dispatch. |
| Club data | `Club`, `Event`, `Member`, `EventAssignment`, `Photo` | Holds the active club's data and maintains relationships. |
| Storage | `ClubManager`, `SaveLoadManager` | Selects club files, migrates legacy data, serializes records, and synchronizes photos. |

`ClubFlow` constructs one `UserInterface`, one `CommandParser`, one shared `Club`,
and one `ClubManager`. It creates command objects once and registers them with the
parser. Commands receive the objects they need through their constructors.

At startup, `ClubManager.initialize()` loads the default club. Each input-loop
iteration calls `UserInterface.prompt()` followed by `CommandParser.parse(input)`.
Commands print their results through the UI and return a boolean indicating
whether the application should exit. After parsing returns normally, `ClubFlow`
saves the active club if it has unsaved changes, then checks the exit flag.

### UI component

### Logic component

### Model component

The Model component represents and manages the data of the currently active club. Its main classes are `Club`, `Event`, `Member`, `EventAssignment`, and `Photo`.

The following class diagram illustrates the structure of the Model component and the relationships between its classes.

![Structure of the Model Component](diagrams/ModelClassDiagram.png)

[PlantUML source](diagrams/ModelClassDiagram.puml)

The Model component:

- Stores club members, events, and member-event assignments in a shared `Club` object.
- Represents each event using an `Event` object containing event details, completion status, and associated photos.
- Represents each member using a `Member` object containing contact information and club functions.
- Uses `EventAssignment` objects to associate members with events, optionally specifying their responsibilities.
- Uses `Photo` objects to manage images associated with events.
- Maintains consistency between members, events, and assignments when data is modified or deleted.

#### Data management

`Club` maintains separate lists of `Member`, `Event`, and `EventAssignment` objects. These lists are private, and their public getters return read-only snapshots using `List.copyOf()`. This prevents callers from modifying the collections directly.

Events are identified by unique, case-insensitive tags within a club. Members are accessed using one-based indices, corresponding to their positions in the member list.

An `EventAssignment` holds references to a specific `Event` and `Member`, together with an optional role. Using object references rather than member indices ensures that removing another member does not change the meaning of existing assignments.

#### Maintaining data consistency

The Model maintains consistency when existing objects are replaced or removed.

When an event is updated, `Club.updateEvent()` replaces the original `Event` object while preserving its position in the event list. It also transfers the original event's photos and recreates affected `EventAssignment` objects so that they reference the replacement event.

Similarly, `Club.replaceMemberByIndex()` transfers existing assignments to the replacement member. Deleting an event or member removes its associated assignments, preventing references to objects that are no longer part of the club.

#### Shared club instance

Commands operate on a shared `Club` instance rather than maintaining separate copies of club data.

When switching clubs, `ClubManager` loads the selected club and calls `Club.replaceWith()` to replace the shared object's contents. This allows existing command objects to continue using the same `Club` reference without being reconstructed.

#### Tracking unsaved changes

`Club` maintains an `unsavedChanges` flag to indicate whether its data has been modified.

Operations that modify club data set this flag to `true`. After the active club is successfully saved, `ClubManager` calls `markSaved()` to clear the flag.

This allows the application to identify changes requiring persistence without saving after every read-only operation.

### Storage component

### Common classes

The `clubflow.exception` package contains application exceptions, including
`ClubFlowException` and `CommandParseException`. `ClubFlow` displays expected
command failures and can print the selected command's syntax for parsing errors.
It also catches unexpected runtime exceptions and records them in the log.
There is no separate `commons` package.

## Implementation

### Event management

Use the following entry points when changing event behavior. Command syntax and
user-visible rules belong in the [User Guide](UserGuide.md).

| Feature | Start here |
| --- | --- |
| Create | [CreateEventCommand](../src/main/java/clubflow/command/CreateEventCommand.java) |
| Delete | [DeleteEventCommand](../src/main/java/clubflow/command/DeleteEventCommand.java) |
| View | [ViewEventCommand](../src/main/java/clubflow/command/ViewEventCommand.java) |
| Edit | [EditEventCommand](../src/main/java/clubflow/command/EditEventCommand.java) |
| Find | [FindEventCommand](../src/main/java/clubflow/command/FindEventCommand.java), [EventSearch](../src/main/java/clubflow/search/EventSearch.java) |
| Completed/pending | [CompletionCommand](../src/main/java/clubflow/command/CompletionCommand.java), [EventCompletion](../src/main/java/clubflow/completion/EventCompletion.java) |

Commands share a long-lived [Club](../src/main/java/clubflow/Club.java). Look up
events on each request: retaining an event reference across requests can leave
a command using an old object after editing, marking, or loading data. Route
mutations through the club so relationship maintenance and autosave stay coordinated.

Deleting an event also removes its member assignments, so deletion changes both
the event list and the assignment roster.

The creation flow below ends before the input loop's autosave step.

<div style="overflow-x: auto;">
  <a href="diagrams/CreateEventSequence.png"><img src="diagrams/CreateEventSequence.png" alt="Successful event creation sequence" style="width: 75.5em; max-width: none; height: auto;"></a>
</div>

[PlantUML source](diagrams/CreateEventSequence.puml).

#### Editing and replacement

Edits validate a replacement before installing it, avoiding partially applied
changes. The tradeoff is that replacing an event must preserve its position,
photos, and member assignments; this belongs in `Club.updateEvent()`.
When adding an event field, review both editing and completion copy paths,
the displayed/searchable details, and the save/load format together.

<div style="overflow-x: auto;">
  <a href="diagrams/EditEventSequence.png"><img src="diagrams/EditEventSequence.png" alt="Event editing and replacement sequence" style="width: 39.1875em; max-width: none; height: auto;"></a>
</div>

[PlantUML source](diagrams/EditEventSequence.puml).
Autosave follows this flow; the club is marked saved only after persistence succeeds.

#### Search

Search combines predicates over the current event snapshot. A linear scan keeps
ordering predictable and avoids maintaining indexes as events change. Keep
filter validation in `FindEventCommand` and matching in `EventSearch`; compile
filter values once per request. Consider indexing only if measured usage justifies
the additional update logic.

<div style="overflow-x: auto;">
  <a href="diagrams/FindEventSequence.png"><img src="diagrams/FindEventSequence.png" alt="Event search sequence" style="width: 46.6875em; max-width: none; height: auto;"></a>
</div>

[PlantUML source](diagrams/FindEventSequence.puml).

#### Completion

Mark and unmark share dispatch logic, with the desired state configured at
registration. The event handler owns lookup and replacement, keeping the command
independent of event details. Preserve idempotency: repeating a request must keep
the requested state, not toggle it. Completion uses the same replacement path as
editing, so both features must preserve unrelated data.

In the diagram, `completed` is `True` for `mark` and `False` for `unmark`.

<div style="overflow-x: auto;">
  <a href="diagrams/CompletionSequence.png"><img src="diagrams/CompletionSequence.png" alt="Event completion sequence" style="width: 57.5625em; max-width: none; height: auto;"></a>
</div>

[PlantUML source](diagrams/CompletionSequence.puml).

### Searching members

### Switching clubs without rebuilding commands

### Autosave, import, and file format

### Photo lifecycle and persistence limits

## Documentation, logging, testing, and development workflow

### Documentation

### Logging

### Automated testing and style checks

Event command regression tests are in
[`src/test/java/clubflow/command/`](../src/test/java/clubflow/command).
Prioritize checks across feature boundaries: editing a completed event, preserving
assignments/photos after replacement, fresh search results, and restart persistence.

```powershell
.\gradlew.bat test --tests 'clubflow.command.*Event*Test' --tests clubflow.command.CompletionCommandTest
```

### Extending commands and continuous integration

## Appendix: Requirements

### Product scope

#### Target user profile

#### Value proposition

### User Stories

### Use cases

For these use cases, the actor is a club organiser and the system is ClubFlow.

#### UC1: Edit an event

1. The organiser requests event details using its tag.
2. ClubFlow displays the event.
3. The organiser supplies the tag and at least one changed detail.
4. ClubFlow validates and replaces the event, then attempts autosave.

Extensions: an unknown tag or invalid detail produces an error without applying
the edit. An autosave failure leaves the edited data in memory and displays a warning.

#### UC2: Assign a member to an event

#### UC3: Switch clubs

#### UC4: Set an event's completion state

1. The organiser requests that an event be completed or pending.
2. ClubFlow applies the requested state and attempts to save any change.

Extensions: invalid input or an unknown event leaves data unchanged. Repeating
the request keeps the same state. A save failure is reported; the in-memory change
remains available for retry.

### Non-Functional Requirements

### Glossary

| Term | Meaning |
| --- | --- |
| Event tag | An event's identifier, matched without case within one club. |
| Unsaved flag | An indicator that the active data needs saving. |

## Appendix: Instructions for manual testing

Use Java 25 and a **new, empty working directory**. Launch the packaged application
with `java -ea -cp "<absolute-path-to-clubflow.jar>" clubflow.ClubFlow`, replacing
the placeholder with the actual JAR path. This uses the tested folder for saves,
photos, and logs. The cases below are a starting point for exploratory testing.

### Launch, help, and shutdown

### Event workflow

Start with an empty default club. This walkthrough checks interactions between
features; use the User Guide for full syntax and the automated tests for input boundaries.

1. Create `createEvent e/CAMP n/"Welcome Camp" b/100` and
   `createEvent e/TEMP`. Expect both to be pending.
2. Run `mark e/CAMP` twice, then `editEvent e/camp b/150`.
   Check with `viewEvent all`: completion and order are preserved.
3. Run `findEvent s/completed min/150 max/150`. Expect only `CAMP`.
4. Try `editEvent e/CAMP n/Changed b/-1` and `createEvent e/camp`.
   Expect errors with no partial edit or duplicate.
5. Restart and verify the completed state and edited budget. Run
   `unmark e/CAMP` twice and `deleteEvent e/temp`, then restart again.
   Expect `CAMP` to remain pending and `TEMP` to remain absent.

### Members, assignments, and deletion

### Multiple clubs and restart persistence

### Photo import and isolation

### Import and invalid save files

### Autosave failure and recovery
