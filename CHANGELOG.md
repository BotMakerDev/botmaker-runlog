# Changelog

What each released version of `botmaker-runlog` changes, in a few bullets. It is what the release publishes as
the GitHub Release body, and the release refuses to cut a version with no section here.

Sections are `## [x.y.z] — YYYY-MM-DD`, newest first.

## [Unreleased]

### Added

- **Problems and issue texts** (spec `2026-11-03-run-reports-design.md` §3, §6). `Problems.of` groups a log's
  warnings and errors by fingerprint (`botClass:line|calledClass.method|level`): at most 10 per run, the rest
  counted in the last one's `unreported`, each with the 20 lines around its first occurrence and the argument of
  its own call. `IssueText` writes an issue's title, body (facts, the why, the log, a fenced `botmaker-report`
  block and a hidden fingerprint marker) and a repeat's comment; `RunLogs.readIssue` reads an issue and its
  comments back into a `Report`, by format, as it reads a log. Fixtures `formats/1/issue.md` and
  `issue-comment.md` are kept for good.
- **`Scrubber`**: what leaves the machine has screen text (`ScreenText`'s marked spans) as `…`, the given hidden
  texts (other windows' titles) as `…`, the home folder as `~` and the user's name as `<user>`. The run log
  itself keeps everything.
- The run log, moved from `botmaker-shared`: `RunLog` writes a run's `run.jsonl` (its facts first, one line per
  `LogLine`, flushed per line, 5 runs kept, capped at 5 MB), `RunLogs.read` reads it back (`Readable` or
  `Unreadable`) and skips a torn last line, `RunFacts` holds what any host knows plus a plugin's own `details`.
- Every format's reader stays in this jar for good (`Format1`, its fixture under `src/test/resources/formats/1/`);
  nothing is loaded at run time. A log in a newer format reads as *Update Studio to read this log*.
- Every log's first line names its format and writer: `{"format":1,"writer":"com.github.BotMakerDev:botmaker-runlog:<version>","facts":{…}}`.
  A reader ignores fields it does not know and reads missing ones as `unknown`.
- `com.botmaker.runlog.api`: `LogLine`, `Level`, and the shape reports are counted from: `Report`, `Dimension`,
  `Occurrence`, `Counts.by` and `Counts.overTime`. A read log's warnings and errors are its `Report`, grouped by
  bot line, called method, level, bot version and each of the plugin's `details`.
