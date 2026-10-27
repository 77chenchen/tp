# Developer Guide diagrams

Keep each PlantUML source beside its generated SVG.

| Folder | Contents |
| --- | --- |
| `architecture/` | Logical architecture and UI, logic, and model class views. |
| `common/` | Shared command infrastructure and validation. |
| `events/` | Event creation, editing, search, and completion views. |
| `clubs/` | Club switching. |
| `photos/` | Photo management. |
| `shared/` | Shared typography and opaque reference-frame styling. |

Using Java 25, regenerate SVGs from the repository root with your PlantUML JAR:

```shell
java -jar "<path-to-plantuml.jar>" -tsvg "docs/diagrams/**/*.puml"
```

New diagrams should include `../shared/UmlStyle.iuml`. In the Developer Guide,
set each image's width to its SVG width in pixels divided by 16, in `em`, so
diagram labels match the body text. Update image and source links when moving files.
