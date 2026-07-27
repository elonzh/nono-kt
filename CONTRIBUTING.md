# Contributing

Use Java 17+, Kotlin 2.2.x, and [Go Task](https://taskfile.dev/).

```bash
task format
task check
task docs
task integration
```

Keep public packages under `nono.elonzh.com`, execute argv directly, and add
tests before behavior changes. Update English and Simplified Chinese
documentation together.

Commits use `<type>[optional scope]: <description>` and include a body describing
the implementation and validation.
