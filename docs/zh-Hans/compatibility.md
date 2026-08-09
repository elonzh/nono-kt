# 兼容性

| nono-kt | nono CLI | Java | Kotlin | 操作系统 |
| --- | --- | --- | --- | --- |
| 当前开发版本 | CI 选择的最新版本 | 17+ | 2.2.x | Linux、macOS |

nono 发布 1.0 之前，nono-kt 不保证跨 nono 版本兼容。开发以当前 CI 流水线
安装的 nono 版本为基准。CI 每次运行都解析官方最新版本，以尽早发现上游
兼容性问题。

默认情况下，nono-kt 会在创建客户端时校验 CLI。可以用 `minimumVersion` 提高
最低版本；不支持降低源码中的开发基线。通过该校验不代表保证兼容其他 nono
版本。

类型化 `NonoRunOptions` API 以当前开发基线为准。通用
`NonoCommandRequest` 会原样传递 argv，可调用其他子命令，但不会自动将其
输出转换为类型化模型。

语义化版本仅适用于 nono-kt API：

- 补丁版本修复兼容性问题；
- 次版本可以增加类型化 nono 选项和模型；
- 主版本可以改变公开源码或二进制兼容性。

类型化解码会忽略未知 JSON 字段，并在 `rawJson` 中保留原始内容。这种容错不
改变上述 pre-1.0 兼容性策略。
