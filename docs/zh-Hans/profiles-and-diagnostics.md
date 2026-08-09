# Profile 与诊断

## Profile

客户端映射了 nono Profile 命令的 JSON 形式：

```kotlin
val profiles: List<NonoProfileSummary> = client.listProfiles()
val profile: NonoProfile = client.showProfile("locked")
val validation: NonoProfileValidation =
    client.validateProfile("/work/profile.json", strict = true)
```

Profile 模型暴露稳定字段并保留 `rawJson`，因此在 nono-kt 增加类型化访问器之前，
调用方仍能读取 nono 新增字段。校验退出码 2 通过
`strictDeprecationFailure` 表示；其他意外命令失败会抛出
`NonoCommandFailedException`。

## 诊断

可以在运行请求中启用诊断：

```kotlin
val request = NonoRunRequest.builder("curl", "https://example.com")
    .options(
        NonoRunOptions.builder()
            .blockNetwork()
            .silent()
            .noDiagnostics()
            .diagnosticsJson()
            .build(),
    )
    .build()

val result = client.run(request)
result.diagnostics?.diagnostics?.forEach { diagnostic ->
    println("${diagnostic.code}: ${diagnostic.message}")
}
```

启用 `diagnosticsJson()` 后，即使 CLI 在报告后继续追加说明文本，nono-kt 也会从
stderr 中提取 nono 报告。`stderr` 保留移除报告后的其余内容，`rawStderr` 保留原始捕获字节。未知 JSON 字段保留
在报告的 `rawJson` 中。候选报告格式错误时会设置
`diagnosticsProtocolError`；stderr 捕获被截断时也会设置该字段，并保留捕获到的
stderr 原文。

诊断不会改变子进程退出码。
