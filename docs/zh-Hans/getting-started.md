# 入门

## 环境要求

- Java 17 或更高版本
- Kotlin 调用方使用 Kotlin 2.2.x
- Linux 或 macOS 上符合当前开发基线的 nono CLI

添加 Maven Central 依赖：

```kotlin
implementation("com.elonzh.nono:nono-kt:0.1.0")
```

## 创建客户端

`NonoClient.builder().build()` 会从 `PATH` 查找 `nono`，执行
`nono --version`，并拒绝不兼容的版本。需要时可以显式指定可执行文件和环境
变量：

```kotlin
val client = NonoClient.builder()
    .executable(Path.of("/opt/nono/bin/nono"))
    .environment("NONO_HOME", "/opt/nono")
    .build()
```

`verifyOnBuild(false)` 只会把相同的版本校验推迟到首次使用。请关闭客户端以释放
其工作线程；通过 `executors(io, scheduler)` 传入的执行器仍由调用方管理。

## 构建运行请求

子命令始终以参数列表表示：

```kotlin
val request = NonoRunRequest.builder("java", "-jar", "app.jar")
    .options(
        NonoRunOptions.builder()
            .profile("locked")
            .read(Path.of("input"))
            .write(Path.of("output"))
            .blockNetwork()
            .memory("512M")
            .maxProcesses(32)
            .build(),
    )
    .build()
```

nono-kt 会直接生成 `nono run ... -- java -jar app.jar`。不要把不可信参数拼接
成一个命令字符串。

## 读取结果

```kotlin
client.use {
    val result = it.run(request)
    println("exit=${result.exitCode}")
    print(result.stdoutText())
    System.err.print(result.stderrText())
}
```

子进程退出码 1 或 127 等属于正常的 `NonoExecutionResult`。可执行文件查找、
版本、启动和 JSON 协议错误使用 `NonoException` 异常体系。

下一步可阅读[运行与进程 API](run-api.md)或
[Profile 与诊断](profiles-and-diagnostics.md)。
