# User Guide

## Introduction

ClubFlow is a command-line application for managing club events and members.
You can create, edit, view, find, complete, and delete events, manage member contacts, and load club data. Changes are saved automatically.

## Quick Start

1. Ensure that you have Java 25 installed.
1. Place `clubflow.jar` in a folder of your choice and open a terminal in that folder.
1. Run `java -jar clubflow.jar`.
1. Enter commands at the `>` prompt. For example, enter `createEvent e/XMAS n/"Christmas Party"`.
1. Enter `exit` to close the application. Changes are saved automatically.

## Features

Command names and argument prefixes ignore case. Enclose values containing spaces in double quotes.
Square brackets in the formats below indicate optional arguments; do not type the brackets.

### Creating an event: `createEvent`

Adds an event with a unique tag and optional details.

Format: `createEvent e/TAG [n/NAME] [d/DATE] [by/DEADLINE] [b/BUDGET] [v/VENUE] [l/LOGISTICS]`

* The tag cannot be blank and must be unique, ignoring case.
  A duplicate tag displays `Event already exists: TAG` and leaves existing events unchanged.
* Dates and deadlines use `YYYY-MM-DD` format.
* The budget must be a finite, non-negative number and defaults to zero.
* Event fields cannot contain `|`.

Example: `createEvent e/XMAS n/"Christmas Party" d/2026-12-25 b/500 v/"Main Hall"`

### Editing an event: `editEvent`

Updates an existing event selected by its tag. Supply at least one detail to change.

Format: `editEvent e/TAG [n/NAME] [d/DATE] [by/DEADLINE] [b/BUDGET] [v/VENUE] [l/LOGISTICS]`

* Tag matching ignores case. The tag identifies the event and is not changed by editing.
* Omitted fields keep their existing values. The event stays in the same list position.
* Editing preserves the event's completion state.
* Dates and deadlines use `YYYY-MM-DD`; the budget must be a finite, non-negative number.
* Use an empty quoted value to clear a name, venue, or logistics field, such as `v/""`.
  Dates and deadlines require valid dates; they cannot be cleared with empty values.
* Invalid input leaves the event unchanged. An unknown tag displays `Event not found: TAG`.
* Edited details are saved automatically.

Example: `editEvent e/XMAS v/"New Hall" b/600`

### Viewing events: `viewEvent`

Displays one event's full details or all events. Unspecified details appear as `Not set`.
The `Status` line shows `Completed` or `Not completed`.
An unknown tag displays `Event not found: TAG`.

Format: `viewEvent e/TAG` or `viewEvent all`

Example: `viewEvent e/XMAS`

### Marking and unmarking completion: `mark` and `unmark`

Marks an event as completed or not completed using its tag.

Formats: `mark e/TAG` and `unmark e/TAG`

* New events start as not completed. Tag matching ignores case.
* `mark` sets the status to completed; `unmark` sets it to not completed.
  Repeating either command keeps the same status.
* Both commands preserve event details and list order. Use `viewEvent` to see the status.
* Supply exactly one event tag. Unknown tags display `Event not found: TAG`.
* Only events are currently supported. Member marking is not implemented.
* Completion changes are saved automatically.

Examples: `mark e/XMAS` and `unmark e/XMAS`

### Deleting an event: `deleteEvent`

Removes the event with the given tag. Tag matching ignores case.

Format: `deleteEvent e/TAG`

Example: `deleteEvent e/XMAS`

### Finding data: `find`

Searches club data using a type prefix and a keyword or phrase.
The currently supported type is `e`, which searches event tags and names.

Format: `find TYPE/KEYWORD`

Example: `find e/party`

* Matching ignores case and accepts partial text. For example, `find e/mas` matches the tag `XMAS`.
* Enclose phrases in double quotes: `find e/"Christmas Party"`.
* The keyword cannot be blank. Spaces at the start and end of the keyword are ignored.
* Specify exactly one supported search type per command.
* Results show each matching event's tag and name in creation order. Use `viewEvent e/EVENT_TAG` for full details.
* If there are no matches, ClubFlow displays `No matching events found.`
* Only event searching is currently supported by `find`.

### Creating a member: `createMember` or `addMember`

Adds a member with a name and at least one contact method.

Format: `createMember n/NAME [t/TELEGRAM] [e/EMAIL] [f/FUNCTION]...`

* Supply at least one of `t/` or `e/`. Enter Telegram handles without `@`.
* Repeat `f/` to assign multiple functions. Function names cannot be blank or contain commas.
* Member fields cannot contain `|`.
* `addMember` is an alias for `createMember`.

Example: `createMember n/"Lucy Tan" t/lucy f/Treasurer f/Logistics`

### Viewing members: `viewMember`

Lists all members with their contacts, functions, and the indices used for deletion.

Format: `viewMember`

### Deleting a member: `deleteMember`

Removes a member using the positive index shown by `viewMember`.
Enter `y` or `yes` when prompted to confirm, or use `/f` to skip confirmation.
Indices of later members shift after deletion, so check `viewMember` before deleting another member.

Format: `deleteMember i/INDEX [/f]`

Example: `deleteMember i/1`

### Automatic saving and loading data

Changes to events and members are automatically saved to `saves/clubs.cf`.
ClubFlow loads this file at startup. If it does not exist, the app starts with empty data.
If an existing file cannot be loaded, the app displays an error and stops.

To import another saved file, use `load p/PATH`.
Loading replaces the current events and members, then automatically saves them to `saves/clubs.cf`.

* Paths are relative to the folder from which ClubFlow was started.
* If autosaving fails, ClubFlow displays a warning and keeps the changes in memory.
  It retries after the next successfully processed command, including `exit`.
  If saving still fails during `exit`, the app closes and those changes may be lost.
* Saved events include their completion state. Older save files without this field load as not completed.
  Files saved with completion states require this version of ClubFlow or later.

Example: `load p/saves/my-club.cf`

### Exiting: `exit`

Closes ClubFlow after attempting to save any outstanding changes.

## FAQ

**Q**: How do I transfer my data to another computer? 

**A**: Close ClubFlow after checking for any save errors, then copy `saves/clubs.cf` to the same relative location
on the other computer. ClubFlow loads it automatically at startup.

## Command Summary

* Create event `createEvent e/TAG [n/NAME] [d/DATE] [by/DEADLINE] [b/BUDGET] [v/VENUE] [l/LOGISTICS]`
* Edit event `editEvent e/TAG [n/NAME] [d/DATE] [by/DEADLINE] [b/BUDGET] [v/VENUE] [l/LOGISTICS]`
* View event `viewEvent e/TAG` or view all events `viewEvent all`
* Mark event completed `mark e/TAG`
* Mark event not completed `unmark e/TAG`
* Delete event `deleteEvent e/TAG`
* Find data `find TYPE/KEYWORD` (currently supports `e` for events)
* Create member `createMember n/NAME [t/TELEGRAM] [e/EMAIL] [f/FUNCTION]...` (also `addMember`)
* View members `viewMember`
* Delete member `deleteMember i/INDEX [/f]`
* Load data `load p/PATH`
* Exit `exit`
