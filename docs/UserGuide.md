
# User Guide

## Introduction

ClubFlow is a command-line application for managing club events and members.
You can create, edit, view, find, complete, and delete events, manage members and their event assignments,
organise event photos, and work with multiple independent clubs. Changes are saved automatically.

## Quick Start

1. Ensure that you have Java 25 installed.
2. Place `clubflow.jar` in a folder of your choice and open a terminal in that folder.
3. Run `java -jar clubflow.jar`.
4. Enter commands at the `>` prompt. For example, enter `createEvent e/XMAS n/"Christmas Party"`.
5. Enter `exit` to close the application. Changes are saved automatically.

## Features

Command names and argument prefixes ignore case. Enclose values containing spaces in double quotes.
Square brackets in the formats below indicate optional arguments; do not type the brackets.
Enter `help` to list command formats, or `help c/findMember` to see one command's formats and aliases.

### Managing multiple clubs

Each club has its own events, members, assignments, and photos. On every launch,
ClubFlow starts in the `default` club. To manage another club, create it and
switch to it. Creating a club does not switch to it automatically.

Club IDs use letters (`a` to `z`), digits (`0` to `9`), underscores (`_`), or
hyphens (`-`) only, with **no spaces**. Club IDs are case-insensitive and are
stored in lowercase, so `Chess` and `CHESS` refer to the same club. The ID
`default` is reserved for the initial club. ClubFlow does not currently have
a command for deleting or renaming clubs.

#### Creating a club: `createClub`

Creates an empty club without changing the active club.

Format: `createClub n/CLUB_ID`

Example: `createClub n/chess`

* Success displays `Created club: chess` (using the spelling supplied).
* An existing ID, regardless of case, displays `Club already exists: CLUB_ID`.
* Invalid IDs, such as `school club` or `../school`, are rejected.

#### Switching clubs: `switchClub`

Saves the current club and loads the specified existing club. Subsequent event,
member, and photo commands apply to the selected club only.

Format: `switchClub n/CLUB_ID`

Example: `switchClub n/chess`

* Success displays `Active club: chess` (the lowercase ID).
* Switching to a club that is already active leaves the active club unchanged.
* An unknown ID displays `Club not found: CLUB_ID`; ClubFlow keeps the current
  club selected. If saving or loading fails, it displays an error rather than
  selecting the requested club.
* To return to the initial club, enter `switchClub n/default`.

#### Listing clubs: `listClubs`

Displays clubs that have a valid save-file location, with `(active)` next to
the currently selected club. The default club appears first, followed by
other clubs in alphabetical order.

Format: `listClubs`

Example output:

```text
[CF] default (active)
[CF] chess
```

#### Checking the active club: `currentClub`

Displays the ID of the club currently being managed.

Format: `currentClub`

Example output: `[CF] Active club: default`

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
When viewing one event, assigned members and their optional roles appear below its details.
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

### Finding events: `findEvent`

Searches events using one or more optional field filters. At least one filter is required; `e/` is optional.

Format: `findEvent [e/KEYWORD] [q/KEYWORD] [n/NAME] [v/VENUE] [l/LOGISTICS] [d/DATE] [by/DEADLINE] [b/BUDGET] [min/BUDGET] [max/BUDGET] [s/STATUS]`

Examples: `findEvent e/party`, `fe v/"Main Hall"`, `fe n/Party v/Hall`, `fe d/2026-12-25`

Budget and status examples: `fe b/100`, `fe min/100 max/500`, `fe s/pending max/500`

Alias: `fe`

* Text matching ignores case and accepts partial text. `e/` searches tags or names, preserving existing behavior.
* `q/` searches across tags, names, venues, and logistics notes. For example, `fe q/chairs` can match logistics notes.
* `n/`, `v/`, and `l/` search only the name, venue, and logistics respectively.
* `d/` and `by/` match the exact event date and planning deadline in `YYYY-MM-DD` format.
  Events with an unset date or deadline do not match the corresponding date filter.
* `b/` matches an exact numeric budget, so `b/100` and `b/100.00` are equivalent.
  `min/` and `max/` set inclusive lower and upper budget bounds; either can be used alone.
  Budget values must be finite non-negative numbers. A draft event's default budget is zero.
  The minimum must not exceed the maximum, and an exact budget must fall within any supplied bounds.
* `s/` accepts `completed` or `pending` (case-insensitive). Status comes from the event's `mark`/`unmark` state,
  not its date. Newly created events are pending.
* When multiple filters are supplied, an event must match all of them. Supply each prefix at most once.
* Enclose phrases in double quotes: `fe v/"Main Hall"`.
* Every supplied filter must have a non-blank value. Spaces at the start and end of each value are ignored.
* Results show each matching event's tag and name in creation order. Use `viewEvent e/EVENT_TAG` for full details.
* If there are no matches, ClubFlow displays `No matching events found.`

### Creating a member: `createMember` or `addMember`

Adds a member with a name and at least one contact method.

Format: `createMember n/NAME [t/TELEGRAM] [e/EMAIL] [f/FUNCTION]...`

* Supply at least one of `t/` or `e/`. Enter Telegram handles without `@`.
* Repeat `f/` to assign multiple functions. Function names cannot be blank or contain commas.
* Member fields cannot contain `|`.
* `addMember` is an alias for `createMember`.

Example: `createMember n/"Lucy Tan" t/lucy f/Treasurer f/Logistics`

### Viewing members: `viewMember`

Lists all members with their contacts, functions, and the indices used for editing, assigning, and deletion.

Format: `viewMember`

### Editing a member: `editMember`

Changes an existing member selected by the positive index shown by `viewMember` or `findMember`.
Supply at least one detail to change. Omitted details keep their existing values.

Format: `editMember i/INDEX [n/NAME] [t/TELEGRAM] [e/EMAIL] [f/FUNCTION]...`

* Repeat `f/` to replace the member's complete list of functions; a single `f/""` clears all functions.
* Use `t/""` or `e/""` to clear a contact, but at least one contact must remain.
  Telegram handles are entered without `@`.
* The name cannot be blank. Invalid input leaves the member and their event assignments unchanged.
* Editing keeps the same member index and updates existing assignments to refer to the edited member.
  The change is saved automatically.

Example: `editMember i/2 n/"Bob Tan" f/Logistics f/Treasurer`

### Finding members: `findMember`

Searches names, Telegram handles, email addresses, and club functions. Supply at least one filter.

Format: `findMember q/KEYWORD [q/KEYWORD]... [n/NAME] [t/TELEGRAM] [e/EMAIL] [f/FUNCTION]...`

You may also search using only `n/`, `t/`, `e/`, or `f/`, for example `findMember e/example.com`.

* `q/` searches across all member fields. `n/`, `t/`, `e/`, and `f/` restrict the search to one field.
  Repeat `q/` or `f/` to narrow results; every supplied filter must match.
* Searches ignore case and surrounding spaces. For one filter, direct results rank exact matches before
  prefix and partial matches; combined filters are ranked by their total relevance.
* For names and Telegram handles, close spellings also appear under `Possible matches` (up to five results).
  Four- or five-character queries allow one spelling edit; queries of six to 64 characters allow two.
  Shorter queries, email addresses, and functions do not use spelling suggestions.
* You may include `@` when searching for a Telegram handle. Put multi-word values in double quotes.
* Each result shows the member's current index, contact details, and functions.
  A possible match is only a suggestion; no member is selected or changed automatically.

Examples: `findMember q/alce`, `findMember n/"Alice Tan" f/Logistics`, `findMember t/@alice`

### Assigning a member to an event: `assignMember`

Adds an existing member to an existing event, optionally with a role.
Use a current index from `viewMember` or `findMember`; member indices can change after deletion.

Format: `assignMember e/TAG i/INDEX [r/ROLE]`

* Event tags ignore case. A member can be assigned to the same event only once.
* A role may contain spaces (use quotes), but cannot contain `|` or a line break.
* Use `viewEvent e/TAG` to see the event's assigned members and roles.
* To change a role, unassign the member and assign them again with the new role.
* Assignment changes are saved automatically.

Example: `assignMember e/XMAS i/2 r/"Lead Host"`

### Unassigning a member from an event: `unassignMember`

Removes an event assignment without deleting the member or the event.

Format: `unassignMember e/TAG i/INDEX`

Example: `unassignMember e/XMAS i/2`

### Deleting a member: `deleteMember`

Removes a member using the positive index shown by `viewMember`.
Enter `y` or `yes` when prompted to confirm, or use `/f` to skip confirmation.
Indices of later members shift after deletion, so check `viewMember` before deleting another member.
Deleting a member also removes their event assignments. Deleting an event removes its assignments.

Format: `deleteMember i/INDEX [/f]`

Example: `deleteMember i/1`

### Adding a photo: `addPhoto`

Imports a photo into an event. If no name is provided, the photo is automatically named after its original filename, excluding the file extension.

Format: `addPhoto e/TAG [n/NAME] p/PATH`

* The event must already exist.
* `n/` optionally specifies a custom name for the photo. If omitted, the original filename without its extension is used for convenience.
* `p/` specifies the path to the image being imported.
* Imported photos are stored in PNG format.

Examples:
* `addPhoto e/XMAS p/"images/group.jpg"` - Automatically names the photo `group`.
* `addPhoto e/XMAS n/GroupPhoto p/"images/group.jpg"` - Names the photo `GroupPhoto`.

### Adding All Photos to an Event: `addAllPhoto`

Imports all photos from a specified directory into an event.

**Format:**
`addAllPhoto e/TAG p/PATH`

**Parameters:**
- `e/TAG`: Tag of the event to add photos to.
- `p/PATH`: Path to the directory containing the photos.

**Example:**
`addAllPhoto e/CAMP p/"C:\Users\Ray\Pictures\Camp"`

**Notes:**
* For convenience, each imported photo is automatically named after its original filename, excluding file extension.
* All supported images in the specified directory are imported and converted to PNG format.
* Each photo retains its original filename without the file extension.
* Subdirectories are ignored.
* If a photo fails to import, the remaining photos will still be processed.
* The number of successful and failed imports will be displayed.
* Imported photos are automatically saved.

### Listing photos: `listPhoto`

Lists photos belonging to a specific event or all events.

Format: `listPhoto` or `listPhoto e/TAG`

* Without arguments, lists photos across all events.
* With `e/TAG`, lists only photos belonging to the specified event.

Examples: `listPhoto` and `listPhoto e/XMAS`

### Opening photos: `openPhoto`

Opens photos belonging to an event using the system's default image viewer.

Format: `openPhoto e/TAG` or `openPhoto e/TAG n/NAME`

* Without `n/`, opens the photos belonging to the specified event.
* With `n/NAME`, opens only the specified photo.

Examples: `openPhoto e/XMAS` and `openPhoto e/XMAS n/GroupPhoto`

### Deleting photos: `deletePhoto`

Deletes a specific photo or all photos belonging to an event.

Format: `deletePhoto e/TAG n/NAME` or `deletePhoto e/TAG all`

* Use `n/NAME` to delete a specific photo.
* Use `all` to delete all photos belonging to the event.
* Deleting photos does not delete the event itself.

Examples: `deletePhoto e/XMAS n/GroupPhoto` and `deletePhoto e/XMAS all`

### Automatic saving and loading data

Changes to the active club are saved automatically in that club's own directory.
The default club is stored at `saves/clubs/default/club.cf`; a club with ID
`chess` is stored at `saves/clubs/chess/club.cf`. At startup, ClubFlow always
loads the default club. If its save file is missing, ClubFlow creates an empty
one. If an existing default save file cannot be loaded, it displays an error
and stops. Existing data in the legacy `saves/clubs.cf` location is migrated
to the default club when the new default file has not yet been created.

To import another saved file into the **currently active club**, use
`load p/PATH`. Importing replaces that club's in-memory events, members,
and assignments; ClubFlow then attempts to save them to the active club's
`club.cf` file. It does not switch clubs.

* Paths are relative to the folder from which ClubFlow was started.
* If autosaving fails, ClubFlow displays a warning and keeps the changes in memory.
  It retries after the next successfully processed command, including `exit`.
  If saving still fails during `exit`, the app closes and those changes may be lost.
* Saved events include their completion state. Older save files without this field load as not completed.
  Files saved with completion states require this version of ClubFlow or later.
* Saved assignments refer to an event tag and a member index. Older save files without assignments still load.

Example: `load p/saves/my-club.cf`

### Photo storage and loading

Saved photos are placed in a `photos/` directory alongside each club's
`club.cf` file, separated by event tag. For example:

```text
saves/
└── clubs/
    ├── default/
    │   ├── club.cf
    │   └── photos/
    │       └── XMAS/
    │           ├── GroupPhoto.png
    │           └── Venue.png
    └── chess/
        ├── club.cf
        └── photos/
            └── AGM/
                └── TeamPhoto.png
```

* Imported photos are stored in PNG format. During use, ClubFlow works with
  `cache/photos/` and saves the active club's photos into its club-specific
  directory. Switching clubs replaces the working photo cache with the
  selected club's photos.
* When using `load p/PATH`, ClubFlow reads photos from a `photos/` directory
  next to the imported file. The successful import is then saved to the active
  club's directory.
* To transfer a club to another computer, copy its `club.cf` file **and**
  adjacent `photos/` directory, keeping their relative layout.

### Exiting: `exit`

Closes ClubFlow after attempting to save any outstanding changes.

## FAQ

**Q**: How do I transfer my data to another computer?

**A**: Close ClubFlow after checking for save errors. Copy the entire
`saves/clubs/` directory to the same relative location on the other computer,
including each club's `club.cf` and `photos/` folder if present. ClubFlow
loads `default` at startup; use `switchClub n/CLUB_ID` to access other clubs.

## Command Summary

* Show commands and aliases `help` or `help c/COMMAND`
* Create club `createClub n/CLUB_ID`
* Switch club `switchClub n/CLUB_ID`
* List saved clubs `listClubs`
* Check the active club `currentClub`
* Create event `createEvent e/TAG [n/NAME] [d/DATE] [by/DEADLINE] [b/BUDGET] [v/VENUE] [l/LOGISTICS]` (alias `ce`)
* Edit event `editEvent e/TAG [n/NAME] [d/DATE] [by/DEADLINE] [b/BUDGET] [v/VENUE] [l/LOGISTICS]` (alias `ee`)
* View event `viewEvent e/TAG` or view all events `viewEvent all` (alias `ve`)
* Mark event completed `mark e/TAG`
* Mark event not completed `unmark e/TAG`
* Delete event `deleteEvent e/TAG` (alias `de`)
* Find events `findEvent [e/KEYWORD] [q/KEYWORD] [n/NAME] [v/VENUE] [l/LOGISTICS] [d/DATE] [by/DEADLINE] [b/BUDGET] [min/BUDGET] [max/BUDGET] [s/STATUS]`
  (alias `fe`; at least one filter is required)
* Create member `createMember n/NAME [t/TELEGRAM] [e/EMAIL] [f/FUNCTION]...` (aliases `cm`, `addMember`)
* View members `viewMember` (alias `vm`)
* Edit member `editMember i/INDEX [n/NAME] [t/TELEGRAM] [e/EMAIL] [f/FUNCTION]...` (alias `em`)
* Find members `findMember q/KEYWORD [q/KEYWORD]... [n/NAME] [t/TELEGRAM] [e/EMAIL] [f/FUNCTION]...`
  (alias `fm`; at least one filter is required)
* Assign member `assignMember e/TAG i/INDEX [r/ROLE]` (alias `am`)
* Unassign member `unassignMember e/TAG i/INDEX` (alias `um`)
* Delete member `deleteMember i/INDEX [/f]` (alias `dm`)
* Add photo `addPhoto e/TAG [n/NAME] p/PATH` (alias `ap`)
* Add photo directory `addAllPhoto e/TAG p/PATH` (alias `aap`, `addPhotos`, `addAllPhotos`)
* List photos `listPhoto` or `listPhoto e/TAG` (alias `lp`)
* Open photos `openPhoto e/TAG` or `openPhoto e/TAG n/NAME` (alias `op`)
* Delete photos `deletePhoto e/TAG n/NAME` or `deletePhoto e/TAG all` (alias `dp`)
* Load data `load p/PATH`
* Exit `exit`
