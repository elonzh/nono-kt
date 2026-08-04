# 兼容性

[English](../compatibility.md) · [首页](index.md)

| nono-kt | nono CLI | Java | Kotlin | 操作系统 |
| --- | --- | --- | --- | --- |
| 0.1.x | 0.69.0+ | 17+ | 2.2.x | Linux、macOS |

默认情况下，nono-kt 会在创建客户端时校验 CLI。可以用 `minimumVersion` 提高
最低版本；不支持降低版本，因为类型化参数和 JSON 解析面向上表所列版本。

类型化 `NonoRunOptions` API 对应 nono 0.69.0。通用
`NonoCommandRequest` 会原样传递 argv，因此无需等待库发版即可调用后续子命令，
但不会自动将其输出转换为类型化模型。

本库遵循语义化版本：

- 补丁版本修复兼容性问题；
- 次版本可以增加类型化 nono 选项和模型；
- 主版本可以改变公开源码或二进制兼容性。

类型化解码会忽略未知 JSON 字段，并在 `rawJson` 中保留原始内容。这有助于向前
兼容，但不保证兼容未来 nono 的破坏性变更。
