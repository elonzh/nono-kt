# nono-kt

[English](README.md)

面向 Kotlin 和 Java 的 [`nono`](https://github.com/nolabs-ai/nono) CLI
客户端，直接执行参数数组，不经过 Shell 解析。

> 状态：早期预览。首个支持版本面向 nono 0.69.0+、Java 17+、Linux 和
> macOS。

## 安装

```kotlin
dependencies {
    implementation("com.elonzh.nono:nono-kt:0.1.0")
}
```

请单独安装 nono 0.69.0 或更高版本并加入 `PATH`；也可以通过
`NonoClient.builder().executable(path)` 指定路径。

## Kotlin

```kotlin
NonoClient.builder().build().use { client ->
    val result = client.run(
        nonoRun(listOf("printf", "hello\n")) {
            allowCurrentDirectory()
            blockNetwork()
        },
    )
    print(result.stdoutText())
}
```

## Java

```java
NonoRunOptions options =
        NonoRunOptions.builder().allowCurrentDirectory().blockNetwork().build();
NonoRunRequest request =
        NonoRunRequest.builder("printf", "hello\n").options(options).build();

try (NonoClient client = NonoClient.builder().build()) {
    NonoExecutionResult result = client.run(request);
    System.out.print(result.stdoutText());
}
```

`run` 将子进程退出码作为结果返回，包括非零退出码。启动、兼容性和协议错误会
抛出 `NonoException`。需要可取消的异步执行时使用 `start`。

沙箱策略由调用方负责。nono-kt 只把类型化配置映射为 `nono run` 参数并直接
执行 argv，不会自行扩大权限或弱化调用方指定的策略。

## 文档

- [文档网站](https://elonzh.github.io/nono-kt/)
- [入门](docs/zh-Hans/getting-started.md)
- [运行与进程 API](docs/zh-Hans/run-api.md)
- [Profile 与诊断](docs/zh-Hans/profiles-and-diagnostics.md)
- [兼容性](docs/zh-Hans/compatibility.md)
- [Kotlin 示例](examples/kotlin/src/main/kotlin/example/Main.kt)
- [Java 示例](examples/java/src/main/java/example/Main.java)

## 许可证

Apache License 2.0。
