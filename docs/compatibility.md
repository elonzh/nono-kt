# Compatibility

| nono-kt | nono CLI | Java | Kotlin | OS |
| --- | --- | --- | --- | --- |
| Current development | Latest release selected by CI | 17+ | 2.2.x | Linux, macOS |

Until nono reaches 1.0, nono-kt does not guarantee compatibility across nono
releases. Development follows the nono release installed by the current CI
pipeline. CI resolves the latest official release on every run so upstream
incompatibilities surface early.

nono-kt verifies the CLI at client creation by default. The minimum version can
be raised with `minimumVersion`; lowering the source baseline is unsupported.
Passing this check is not a compatibility promise for another nono release.

The typed `NonoRunOptions` API follows the current development baseline.
Generic `NonoCommandRequest` passes exact argv and can access other subcommands,
but their output is not automatically typed.

Semantic versioning applies to the nono-kt API:

- patch releases fix compatible behavior;
- minor releases may add typed nono options and models;
- major releases may change public source or binary compatibility.

Unknown JSON fields are ignored by typed decoding and preserved in `rawJson`.
This tolerance does not change the pre-1.0 compatibility policy above.
