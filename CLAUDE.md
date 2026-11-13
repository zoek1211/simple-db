# simple-db

Java · Gradle (Kotlin DSL) JDBC utility (`SimpleDb`, `Sql`, `Article`) for a Codemit assignment. Goal: pass the provided `SimpleDbTest` (t001–t019) without changing the test or the dependencies in `build.gradle.kts`.

- Build with JDK 25 (`JAVA_HOME=$(brew --prefix openjdk@25)/libexec/openjdk.jdk/Contents/Home`); JDK 27 crashes Lombok 1.18.42.
- Tests need MySQL on `localhost:3306`, root / `lldj123414`, DB `simpleDb__test` (Docker container `mysql-1`, `mysql:8.4`).

## Agent skills

### Issue tracker

Issues live in GitHub Issues for zoek1211/simple-db (via `gh`). See `docs/agents/issue-tracker.md`.

### Triage labels

Default five-role vocabulary (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: root `GLOSSARY.md` + `docs/adr/`. See `docs/agents/domain.md`.
