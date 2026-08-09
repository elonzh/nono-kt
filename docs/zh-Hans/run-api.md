# 运行与进程 API

## 策略构建器

`NonoRunOptions.Builder` 对应 nono 0.72.0 的选项族：

- Profile/配置：`profile`、`extendProfile`、`config`
- 文件系统：`allow`、`read`、`write`、文件和 Unix Socket 变体、
  `allowCurrentDirectory`、`workdir`、`bypassProtection`、
  `suppressSavePrompt`
- 网络：`blockNetwork`、`allowNetwork`、Profile、域名、端口、代理和 CA
- 凭据与命令：凭据、端点、环境变量凭据、`allowCommand`、`blockCommand`
- 平台与资源：Launch Services、GPU、HTTP/2、Linux 沙箱策略、内存和进程限制
- 生命周期：Dry-run、分离、超时、回滚、静默模式、诊断、审计、会话、
  Capability Elevation 和详细日志

构建器会在启动 nono 之前拒绝已知冲突组合。策略仍由调用方负责，库不会自行
推断权限。

## 进程 I/O

两种请求都可以附加 `NonoExecutionOptions`：

```kotlin
val execution = NonoExecutionOptions.builder()
    .workingDirectory(Path.of("/work"))
    .environment("MODE", "test")
    .input(NonoInput.bytes("input".toByteArray()))
    .stdout(NonoOutput.capture(1_048_576))
    .stderr(NonoOutput.stream({ bytes -> consume(bytes) }, 1_048_576))
    .timeout(Duration.ofSeconds(30))
    .build()
```

输入可以关闭、继承或使用字节数据。输出可以捕获、在保留有界尾部的同时流式
处理、继承或丢弃。结果标记会说明超时、取消和输出截断。

## 异步与取消

```kotlin
val execution = client.start(request)
execution.result().thenAccept { result -> println(result.exitCode) }

// 稍后：
execution.cancel()
```

取消操作会终止已启动的进程树。`close()` 只关闭客户端自己创建的执行器。

## 通用命令

对于类型化运行 API 尚未建模的 nono 子命令，可使用
`NonoCommandRequest` 保持向前兼容：

```kotlin
val result = client.execute(
    NonoCommandRequest.builder("why", "--json", "--path", "/work").build(),
)
```

参数会被原样传递，绝不经过 Shell 解释。
