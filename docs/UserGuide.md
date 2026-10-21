# User Guide

## Introduction

ClubFlow is a command-line application for managing club events and members.
You can create, view, find, and delete events, manage member contacts, and save or load club data.

## Quick Start

1. Ensure that you have Java 25 installed.
1. Place `clubflow.jar` in a folder of your choice and open a terminal in that folder.
1. Run `java -jar clubflow.jar`.
1. Enter commands at the `>` prompt. For example, enter `createEvent e/XMAS n/"Christmas Party"`.
1. Enter `save` to save your data before using `exit` to close the application.

## Features

Command names and argument prefixes ignore case. Enclose values containing spaces in double quotes.
Square brackets in the formats below indicate optional arguments; do not type the brackets.

### Creating an event: `createEvent`

Adds an event with a unique tag and optional details.

Format: `createEvent e/TAG [n/NAME] [d/DATE] [by/DEADLINE] [b/BUDGET] [v/VENUE] [l/LOGISTICS]`

* The tag cannot be blank and must be unique, ignoring case.
* Dates and deadlines use `YYYY-MM-DD` format.
* The budget must be a finite, non-negative number and defaults to zero.
* Event fields cannot contain `|`.

Example: `createEvent e/XMAS n/"Christmas Party" d/2026-12-25 b/500 v/"Main Hall"`

### Viewing events: `viewEvent`

Displays one event's full details or all events. Unspecified details appear as `Not set`.

Format: `viewEvent e/TAG` or `viewEvent all`

Example: `viewEvent e/XMAS`

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

### Saving and loading data: `save` and `load`

Saving writes the current events and members to a file, replacing any existing contents at that path.
Loading replaces the current events and members with the data from a saved file.

Formats: `save [p/PATH]` and `load p/PATH`

* Paths must be relative to the folder from which ClubFlow was started.
* `save` without a path uses `saves/clubs.cf`.
* Data is not automatically saved or loaded. Save before exiting and load your file when starting a new session.

Examples: `save p/saves/my-club.cf` and `load p/saves/my-club.cf`

### Exiting: `exit`

Closes ClubFlow. Use `save` first to keep your current data.

## FAQ

**Q**: How do I transfer my data to another computer? 

**A**: Use `save`, copy the resulting file to the other computer, and start ClubFlow there.
Load the copied file using a relative path, for example `load p/saves/clubs.cf`.

## Command Summary

* Create event `createEvent e/TAG [n/NAME] [d/DATE] [by/DEADLINE] [b/BUDGET] [v/VENUE] [l/LOGISTICS]`
* View event `viewEvent e/TAG` or view all events `viewEvent all`
* Delete event `deleteEvent e/TAG`
* Find data `find TYPE/KEYWORD` (currently supports `e` for events)
* Create member `createMember n/NAME [t/TELEGRAM] [e/EMAIL] [f/FUNCTION]...` (also `addMember`)
* View members `viewMember`
* Delete member `deleteMember i/INDEX [/f]`
* Save data `save [p/PATH]`
* Load data `load p/PATH`
* Exit `exit`
