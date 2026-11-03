# botmaker-runlog

The run log's format for [BotMaker](https://github.com/BotMakerDev): a run's `run.jsonl`, its writer and reader,
and the fixed shape reports are counted from.

```xml
<dependency>
    <groupId>com.github.BotMakerDev</groupId>
    <artifactId>botmaker-runlog</artifactId>
    <version>v0.1.0</version>
</dependency>
```

from `https://jitpack.io`. Each line of `run.jsonl` is one JSON object; the first is the run's facts, headed by
the `format` and `writer` that wrote it. `jq`, `lnav` and any log tool read it as it is.
