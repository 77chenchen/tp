# Wang Qichen's Project Portfolio Page

### Project: ClubFlow

ClubFlow is a command-line application written in Java that helps student club organisers manage events, members, and related information. Users interact with it by entering commands, and their club data is saved locally for use across sessions.

Given below are my contributions to the project.

- **New Feature: Added member management commands.** ([PR #25](https://github.com/AY2627S1-CS2113-W10-2/tp/pull/25), [PR #52](https://github.com/AY2627S1-CS2113-W10-2/tp/pull/52))
  - What it does: Allows users to create, view, edit, and delete members, including their names, Telegram handles, email addresses, and multiple club functions.
  - Justification: Club organisers need an up-to-date roster before they can coordinate people and events in the same application.
  - Highlights: Implemented validation for optional and repeatable fields, one-based member indices, aliases, confirmation before deletion, and atomic editing so invalid input does not partially modify a member. Editing a member also preserves existing event assignments.

- **New Feature: Added ranked and typo-tolerant member search.** ([PR #52](https://github.com/AY2627S1-CS2113-W10-2/tp/pull/52))
  - What it does: Searches across names, Telegram handles, email addresses, and club functions using combinable general or field-specific filters. The `/u` filter can also find members who are not assigned to any event.
  - Justification: A growing club roster is difficult to scan manually, while small spelling mistakes should not prevent an organiser from finding a member. Finding unassigned members helps managers distribute work more evenly.
  - Highlights: Ranked exact, prefix, and substring matches; implemented bounded Levenshtein-distance suggestions; and separated possible matches from direct matches. The `/u` predicate is evaluated from the current assignment graph, so results remain correct after assignment changes, event deletion, edits, and club switches. Search results always report current member indices.

- **New Feature: Added member-event assignments and responsibility views.** ([PR #52](https://github.com/AY2627S1-CS2113-W10-2/tp/pull/52))
  - What it does: Assigns and unassigns members from events with optional roles, displays an event's roster, and lets a member view all events assigned to them using `viewMemberEvents` (`vme`). Assignments are saved across sessions.
  - Justification: Event records are more useful when managers can allocate work and each member can check their own responsibilities from one place.
  - Highlights: Modelled assignments using object references so index changes cannot silently redirect an assignment. The responsibility view combines member, event, and assignment data; displays tag, date, role, and responsibility status; summarizes upcoming, overdue, completed, and undated work; sorts dated events chronologically with undated events last; and remains read-only. Editing preserves relationships, while deletion removes related assignments. The extended save format remains compatible with older files.

- **Code contributed**: [tP Code Dashboard](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/?search=77chenchen&sort=groupTitle&sortWithin=title&timeframe=commit&mergegroup=&groupSelect=groupByRepos&breakdown=true&checkedFileTypes=docs~functional-code~test-code~other)

- **Enhancements to existing features**:
  - Extended the command parser to support repeatable arguments such as multiple member functions and search filters.
  - Integrated member assignments with event viewing, editing, and deletion while maintaining data consistency.
  - Extended autosave and the save-file format to persist member-event assignments while retaining compatibility with older files.
  - Added command-level tests for member CRUD, combined and fuzzy search, unassigned-member filtering, assignment changes, and responsibility-view ordering, status summaries, and placeholders.
  - Added persistence and end-to-end tests covering save/load round trips, autosave across sessions, index changes, deletion cleanup, invalid records, assignment preservation after edits, and the two assignment-query workflows.

- **Documentation**:
  - **User Guide**:
    - Documented the `editMember`, `findMember`, `assignMember`, `unassignMember`, and `viewMemberEvents` commands, including formats, examples, aliases, validation rules, chronological ordering, responsibility statuses, the `/u` filter, ranking behavior, and typo-tolerance limits.
    - Updated the member, event-viewing, deletion, autosave, data-file compatibility, and command-summary sections to describe assignment behavior accurately.
  - **Developer Guide** ([PR #60](https://github.com/AY2627S1-CS2113-W10-2/tp/pull/60)):
    - Documented the Logic component and command-processing flow and added and updated the [Logic class diagram](../diagrams/architecture/LogicClass.svg) and its [PlantUML source](../diagrams/architecture/LogicClass.puml).
    - Explained the member-search design, scoring rules, bounded edit-distance approach, current-index behavior, dynamic unassigned-member filtering, and design trade-offs.
    - Documented the responsibility-view implementation and added `ViewMemberEventsSequence` and `FindUnassignedMembersSequence` sequence diagrams with their PlantUML sources.
    - Added and refined product scope, user stories, use cases, and manual testing instructions, including the complete member and event-assignment workflow and the two new assignment-query workflows.
    - Reorganised the guide to follow the recommended project-documentation structure and aligned it with the implemented application behavior.

- **Team-based contributions**:
  - Integrated member features with the team's shared command parser, command aliases and help syntax, event display, autosave, save/load format, and multi-club model.
  - Updated documentation after team features were merged so that command correction, invitations, member workflows, and diagrams consistently described the final implementation.
