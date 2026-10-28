# Project Portfolio Page: Camille (`camimaigrot`)

## Overview

ClubFlow is a Java command-line application designed to help student clubs organise events, members, event assignments, and photos. It supports multiple independent clubs, allowing users to manage each club's information separately through a common interface.

## Summary of Contributions

**Code contributed:** [My contributions on GitHub](https://github.com/AY2627S1-CS2113-W10-2/tp/pulls?q=is%3Apr+author%3Acamimaigrot) | [Team repository](https://github.com/AY2627S1-CS2113-W10-2/tp)

### Enhancements implemented

* **Multiple-club management:** Implemented support for managing separate clubs in a single application. Introduced `ClubManager` and the `createClub`, `switchClub`, `listClubs`, and `currentClub` commands. Each club has its own saved data, including members, events, and photos. Added club-ID validation, duplicate and missing-club handling, and compatibility with existing single-club saves. A key design decision was to retain a shared `Club` object when switching clubs, so existing command objects continue to reference the active club without needing to be reconstructed. Added automated tests for club operations, switching, persistence, and integration with existing workflows. [PR #54](https://github.com/AY2627S1-CS2113-W10-2/tp/pull/54)
* **Event and member models:** Contributed the initial `Event` and `Member` domain classes and their JUnit tests, establishing data structures used by later event and member workflows. [PR #21](https://github.com/AY2627S1-CS2113-W10-2/tp/pull/21) | [PR #22](https://github.com/AY2627S1-CS2113-W10-2/tp/pull/22)
* **Terminal UI:** Worked on the application's welcome-message presentation. [PR #28](https://github.com/AY2627S1-CS2113-W10-2/tp/pull/28)

### Documentation

* **User Guide:** Documented the multi-club workflow, including club creation, switching, listing, current-club selection, restrictions on IDs, and expected behaviour. [PR #58](https://github.com/AY2627S1-CS2113-W10-2/tp/pull/58) | [User Guide](../UserGuide.md)
* **Developer Guide:** Documented the terminal UI and the design and implementation of switching clubs without recreating commands. Explained class responsibilities, data replacement, persistence and failure handling, and relevant design trade-offs. Added the UI class diagram and successful club-switch sequence diagram, including their PlantUML sources. Also reorganised the requirements and manual-testing appendices. [PR #58](https://github.com/AY2627S1-CS2113-W10-2/tp/pull/58) | [Developer Guide](../DeveloperGuide.md) | [UI diagram source](../diagrams/UiClassDiagram.puml) | [Club-switch diagram source](../diagrams/SwitchClubSequence.puml)

### Team-based contributions

* Integrated the multi-club enhancement with the team's existing member, event, command, and storage components, addressing merge conflicts and coding-standard issues during integration. [PR #54](https://github.com/AY2627S1-CS2113-W10-2/tp/pull/54)
* As a repository administrator, was responsible for reviewing a significant portion of the team's pull requests before they were merged. This involved checking code quality, adherence to coding standards, consistency with the existing codebase, and potential integration issues. Also provided feedback to teammates and helped coordinate the integration of their contributions to maintain the overall quality and stability of the project.
