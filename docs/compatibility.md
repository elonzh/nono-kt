# Compatibility

[简体中文](compatibility.zh-Hans.md) · [Home](../README.md)

| nono-kt | nono CLI | Java | Kotlin | OS |
| --- | --- | --- | --- | --- |
| 0.1.x | 0.69.0+ | 17+ | 2.2.x | Linux, macOS |

nono-kt verifies the CLI at client creation by default. The minimum version can
be raised with `minimumVersion`; lowering it is unsupported because typed
arguments and JSON parsing target the table above.

The typed `NonoRunOptions` API follows nono 0.69.0. Generic
`NonoCommandRequest` passes exact argv and can access later subcommands without
a library release, but their output is not automatically typed.

The library uses semantic versioning:

- patch releases fix compatible behavior;
- minor releases may add typed nono options and models;
- major releases may change public source or binary compatibility.

Unknown JSON fields are ignored by typed decoding and preserved in `rawJson`.
This improves forward compatibility but does not guarantee compatibility with a
future nono breaking change.
