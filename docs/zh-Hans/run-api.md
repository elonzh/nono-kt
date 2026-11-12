# 运行与进程 API

## 策略构建器

`NonoRunOptions.Builder` 对应当前开发基线的选项族：

- Profile/配置：`profile`、`extendProfile`、`config`
- 文件系统：`allow`、`read`、`write`、文件和 Unix Socket 变体、
  `allowCurrentDirectory`、`workdir`、`bypassProtection`、
  `suppressSavePrompt`、`strictBrokerPath`
- 网络：`blockNetwork`、`allowNetwork`、Profile、域名、端口、代理和 CA
- 凭据与命令：凭据、端点、环境变量凭据、`allowCommand`、`blockCommand`
- 平台与资源：Launch Services、GPU、HTTP/2、Linux 沙箱策略与命名空间隔离、
  内存和进程限制（Linux cgroup v2）
- 生命周期：Dry-run、分离、超时、回滚、静默模式、诊断、审计、会话、
  Capability Elevation 和详细日志

构建器会在启动 nono 之前拒绝已知冲突组合。策略仍由调用方负责，库不会自行
推断权限。

启用 `strictBrokerPath()` 后，如果沙箱可以写入宿主机 `PATH` 中的目录，
nono 会拒绝启动。

在 Linux 上，`isolateLinuxNamespaces(LinuxNamespace.NET, LinuxNamespace.IPC)`
会在 Profile 的隔离策略上追加命名空间。网络隔离需要 `blockNetwork()` 或代理
网络策略。`LinuxNamespace.LOOPBACK` 需要网络隔离，并允许在隔离环境自己的回环
接口上使用 TCP。nono 在启动时检查合并后的策略及宿主机是否支持所选命名空间。

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
