**Warning** at `com.example.Collect:57`, calling `Screens.goTo(Home)`, ×3 in one run.

> stuck on Title | ~/x

| Fact | Value |
|---|---|
| Bot | com.example:bot |
| Version | v1.2.0 (abc1234) |
| OS | Linux 7.2 |
| Platform | Waydroid |
| botmaker-sdk | 2.0.0 |

<details><summary>The log around it</summary>

````text
10:15:31 debug [Screens] goTo(Home) → false  ```  3 ms
10:15:32 warn [Screens] stuck on Title | ~/x  (×3)
````

</details>

```botmaker-report
{
  "format" : 1,
  "writer" : "com.github.BotMakerDev:botmaker-runlog:0.1.0",
  "fingerprint" : "com.example.Collect:57|com.botmaker.sdk.api.screens.Screens.goTo|warn",
  "level" : "warn",
  "botClass" : "com.example.Collect",
  "line" : 57,
  "calledClass" : "com.botmaker.sdk.api.screens.Screens",
  "method" : "goTo",
  "argument" : "Home",
  "botVersion" : "v1.2.0",
  "commit" : "abc1234",
  "count" : 3,
  "details" : {
    "Platform" : "Waydroid"
  }
}
```

<!-- botmaker:fp=74f65ab843847fd6 -->
