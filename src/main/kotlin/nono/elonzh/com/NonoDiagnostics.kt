package nono.elonzh.com

public data class NonoDiagnostic(
    public val code: String,
    public val severity: String,
    public val message: String,
    public val rawJson: String,
)

public data class NonoDiagnosticReport(
    public val exitCode: Int,
    public val diagnostics: List<NonoDiagnostic>,
    public val proxyDiagnostics: List<NonoDiagnostic>,
    public val denialCount: Int,
    public val ipcDenialCount: Int,
    public val violationCount: Int,
    public val rawJson: String,
)
