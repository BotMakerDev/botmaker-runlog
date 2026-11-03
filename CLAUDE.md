# CLAUDE.md

Guidance for working in **botmaker-runlog**, the run log's format. Read the umbrella `../CLAUDE.md` first; the
design is `../docs/superpowers/specs/2026-11-03-run-reports-design.md` (§2 the log, §6 this module).

## What it is

A run's `run.jsonl`: the writer (`RunLog`), every format's reader (`RunLogs.read`, dispatching by `format` to a
package-private `FormatN`), the run's facts (`RunFacts`), and
`com.botmaker.runlog.api`, the fixed shape every reader of reports uses: `LogLine` in, `Report` (`Dimension`s,
`Occurrence`s) out, `Counts` over it. shared writes through it; the CLI and Studio read through it.

## Rules

- **Jackson only.** No BotMaker dependency, no JavaFX, nothing that self-activates.
- **Every reader stays, nothing is loaded.** A new format adds `FormatN` and `src/test/resources/formats/N/run.jsonl`;
  neither is ever deleted (`RunLogsTest.everyFormatsFixtureReads`). A newer format than the jar knows is
  `Unreadable` with *Update Studio to read this log* (`RunLogsTest.aNewerFormatAsksForAnUpdate`).
- **`format` is bumped only when a field is renamed or changes meaning.** Adding a field is free: every reader
  ignores fields it does not know and reads a missing one as `unknown` (`RunLogTest.aNewerFieldStillReads`).
- **An issue is read like a log.** `RunLogs.readIssue` dispatches on the `botmaker-report` block's `format`;
  `formats/N/issue.md` and `issue-comment.md` stay with the log fixture (`IssueTextTest.everyFormatsIssueFixtureReads`).
- **Nothing leaves the machine unscrubbed** (`Scrubber`): screen text, other windows' titles, the home folder and
  the user's name. The run log on disk keeps them.
- **No plugin vocabulary.** A plugin's facts are `RunFacts.details`, its own labels; nothing here names a flow,
  a screen, a platform or a game.

## Building

```bash
mvn test        # RunLogTest, RunLogsTest, CountsTest, ProblemsTest, ScrubberTest, IssueTextTest
```

Published through JitPack. Releases are cut from the umbrella (`botmaker release --runlog <version>`).
