package nono.elonzh.com

public data class NonoProfileSummary(
    public val name: String,
    public val source: String,
    public val pack: String?,
    public val description: String,
    public val extends: List<String>,
    public val error: String?,
    public val rawJson: String,
)

public data class NonoProfile(
    public val name: String,
    public val description: String,
    public val extends: List<String>,
    public val rawJson: String,
)

public data class NonoProfileValidation(
    public val file: String,
    public val valid: Boolean,
    public val errors: List<String>,
    public val warnings: List<String>,
    public val deprecatedKeys: Int,
    public val strictDeprecationFailure: Boolean,
    public val rawJson: String,
)
