# ClubFlow

ClubFlow is a Java-based command-line application designed to help club managers organise their clubs efficiently. It provides a centralised platform for managing events, members, photos, and event-related logistics across multiple clubs.

## Features

ClubFlow provides the following features:

- **Multi-club management:** Create and switch between independent clubs, each with its own events, members, and photos.
- **Event management:** Create, edit, delete, and track events, including their dates, deadlines, budgets, venues, and logistics.
- **Event searching:** Search events using multiple filters, including keywords, dates, date ranges, budgets, and completion status. View upcoming events with a dedicated command.
- **Member management:** Add, edit, search, and remove members. Assign members to events, manage their roles, and view their assignments.
- **Photo management:** Import, view, rename, and delete event photos, including importing multiple photos at once.
- **Invitation generation:** Generate event invitations and automatically copy them to the clipboard for sharing.
- **Automatic persistence:** Automatically save changes and restore club data when restarting the application.
- **Command-line assistance:** Access command descriptions, use shortcuts, and receive suggestions when a command name is misspelled.


## Quick Start

### Requirements
- Java Development Kit (JDK) 25

### Running ClubFlow

1. Download `clubflow.jar`.
2. Open a terminal in the folder containing the JAR file.
3. Run:

   `java -jar clubflow.jar`

4. Enter `help` to see the available commands.
5. Enter `exit` to close the application.

Changes are saved automatically.

### Example commands

| Command                                               | Description             |
|-------------------------------------------------------|-------------------------|
| `createClub n/chess`                                  | Create a club           |
| `switchClub n/chess`                                  | Switch to another club  |
| `createEvent e/XMAS n/"Christmas Party" d/2026-12-25` | Create an event         |
| `findEvent from/2026-11-01 to/2026-11-30`             | Search by date range    |
| `upcomingEvents`                                      | Display upcoming events |
| `listClubs`                                           | List available clubs    |
| `help`                                                | Display command help    |

For more information, refer to the [User Guide](docs/UserGuide.md).

## Setting up in Intellij

Prerequisites: JDK 25 (use the exact version), update Intellij to the most recent version.

1. **Ensure Intellij JDK 25 is defined as an SDK**, as described [here](https://www.jetbrains.com/help/idea/sdk.html#set-up-jdk) -- this step is not needed if you have used JDK 25 in a previous Intellij project.
1. **Import the project _as a Gradle project_**, as described [here](https://se-education.org/guides/tutorials/intellijImportGradleProject.html).
1. **Verify the setup**: After the importing is complete, locate the `src/main/java/clubflow/ClubFlow.java` file, right-click it, and choose `Run ClubFlow.main()`. If the setup is correct, you should see something like the below:
   ```
    > Task :compileJava UP-TO-DATE
    > Task :processResources NO-SOURCE
    > Task :classes UP-TO-DATE
    
    > Task :clubflow.ClubFlow.main()
                               ...........................
                    ......        ........................
               .....       ......                        ....::...
             ...       .:..             ............            ..::..
      #@@####   @@@   .:.      @@@  .::  ########## #@@#           .:::
    #@@#    #@  #@@   ::       #@@ .:.    #@@#   #@  @@#:           .::.
   #@@#    :.#  #@@  ###  ###  #@@####    #@@     #  @@#::  ###   ####:.     ##
   @@@     :.   #@@  @@@  #@@  #@@#:#@@#  #@@#  #    @@# :@@# @@# #@@#::#@   @
   @@@     :.   #@@  #@@  #@@  #@@:: @@@  #@@@##@#   @@# #@@  #@@  #@@::@@# @#
   @@@     :.#  #@@  #@@  #@@  #@@.: @@@  #@@   #    @@# #@@  #@@   @@#@#@@#@
    @@@    .#@  #@@  #@@. #@@  #@@ . @@#  #@@..      @@#  @@  @@@   #@@#.#@@
     #@@@###@#  @@@#  @@@##@@#  @@##@@#. #@@@@#     #@@@ .#@@#@#     @@:  @@
            .:.        .:::.           .......         .::.         .::.
              ....         ..::....               ......         .:::.
                  .....           ................          ..:::..
                       ............              .....::::::..
    
    Welcome to ClubFlow!
    To get started, type "help" to explore the available commands, or "ug" to see the detailed user guide.
    [CF] Saved data loaded.
    [CF] Active club: default
    >
   ```
   Type some command (`help` for example) and press enter.

**Warning:** Keep the `src\main\java` folder as the root folder for Java files (i.e., don't rename those folders or move Java files to another folder outside of this folder path), as this is the default location some tools (e.g., Gradle) expect to find Java files.

## Build automation using Gradle

* This project uses Gradle for build automation and dependency management. It includes a basic build script as well (i.e. the `build.gradle` file).
* If you are new to Gradle, refer to the [Gradle Tutorial at se-education.org/guides](https://se-education.org/guides/tutorials/gradle.html).


### Running and building ClubFlow

To run ClubFlow from the source code:

```bash
./gradlew run
```

To build the executable JAR:

```bash
./gradlew shadowJar
```

The generated JAR is located at `build/libs/clubflow.jar`.

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Testing

### JUnit tests

JUnit tests are located in `src/test/java/clubflow/`
and its subpackages. They cover the application's
core functionality, including club management,
events, members, commands, and data persistence.

To run all JUnit tests:

```bash
./gradlew test
```

If you are new to JUnit, refer to the
[JUnit Tutorial](https://se-education.org/guides/tutorials/junit.html).

## Checkstyle

* The project uses Checkstyle to enforce consistent Java coding standards.
* If you are new to Checkstyle, refer to the [Checkstyle Tutorial at se-education.org/guides](https://se-education.org/guides/tutorials/checkstyle.html).
* To run all Checkstyle tests:
```bash
./gradlew checkstyleMain checkstyleTest
```

## CI using GitHub Actions

The project uses [GitHub actions](https://github.com/features/actions) for CI. When you push a commit to this repo or PR against it, GitHub actions will run automatically to build and verify the code as updated by the commit/PR.

## Documentation

- [User Guide](docs/UserGuide.md): Instructions and examples for using ClubFlow.
- [Developer Guide](docs/DeveloperGuide.md): Architecture, design decisions, implementation details, and testing.
- [About Us](docs/AboutUs.md): Information about the development team.

## Development Team

ClubFlow was developed by team CS2113-W10-2 as part of
the CS2113 Software Engineering & Object-Oriented
Programming module at the National University of Singapore.

See [About Us](docs/AboutUs.md) for the team members
and their project portfolios.
