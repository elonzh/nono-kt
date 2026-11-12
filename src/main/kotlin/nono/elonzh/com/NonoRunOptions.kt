package nono.elonzh.com

import java.nio.file.Path
import java.time.Duration

public enum class LinuxSandboxPolicy(
    internal val cliValue: String,
) {
    AUTO("auto"),
    LANDLOCK("landlock"),
    EXTERNAL("external"),
}

/** Values accepted by nono's Linux namespace isolation flag. */
public enum class LinuxNamespace(
    internal val cliValue: String,
) {
    NET("net"),
    LOOPBACK("loopback"),
    IPC("ipc"),
    UTS("uts"),
}

public class NonoRunOptions private constructor(
    internal val arguments: List<String>,
) {
    public class Builder internal constructor() {
        private val arguments = mutableListOf<String>()
        private var configSelected = false
        private var configConflictingOptionSelected = false
        private var profileSelected = false
        private var extendsSelected = false
        private var allowNetworkSelected = false
        private var restrictedNetworkSelected = false
        private var blockNetworkSelected = false
        private var proxyModeSelected = false
        private var proxyCredentialSelected = false
        private var allowEndpointSelected = false
        private var proxyPortSelected = false
        private var upstreamProxySelected = false
        private var upstreamBypassSelected = false
        private var detachedSelected = false
        private var detachTimeoutSelected = false
        private var rollbackSelected = false
        private var noRollbackSelected = false
        private var rollbackAllSelected = false
        private var rollbackIncludeSelected = false
        private var rollbackDestinationSelected = false
        private var noAuditSelected = false
        private var noAuditIntegritySelected = false
        private var auditIntegritySelected = false
        private var auditSignKeySelected = false

        public fun profile(value: String): Builder =
            apply {
                profileSelected = true
                option("--profile", value, conflictsWithConfig = true)
            }

        public fun extendProfile(value: String): Builder =
            apply {
                extendsSelected = true
                option("--extends", value, conflictsWithConfig = true)
            }

        public fun config(value: Path): Builder =
            apply {
                configSelected = true
                pathOption("--config", value)
            }

        public fun allow(value: Path): Builder =
            apply {
                pathOption("--allow", value, conflictsWithConfig = true)
            }

        public fun read(value: Path): Builder =
            apply {
                pathOption("--read", value, conflictsWithConfig = true)
            }

        public fun write(value: Path): Builder =
            apply {
                pathOption("--write", value, conflictsWithConfig = true)
            }

        public fun allowFile(value: Path): Builder =
            apply {
                pathOption("--allow-file", value, conflictsWithConfig = true)
            }

        public fun readFile(value: Path): Builder =
            apply {
                pathOption("--read-file", value, conflictsWithConfig = true)
            }

        public fun writeFile(value: Path): Builder =
            apply {
                pathOption("--write-file", value, conflictsWithConfig = true)
            }

        public fun allowUnixSocket(value: Path): Builder =
            apply {
                pathOption("--allow-unix-socket", value, conflictsWithConfig = true)
            }

        public fun allowUnixSocketBind(value: Path): Builder =
            apply {
                pathOption("--allow-unix-socket-bind", value, conflictsWithConfig = true)
            }

        public fun allowUnixSocketDir(value: Path): Builder =
            apply {
                pathOption("--allow-unix-socket-dir", value, conflictsWithConfig = true)
            }

        public fun allowUnixSocketDirBind(value: Path): Builder =
            apply {
                pathOption("--allow-unix-socket-dir-bind", value, conflictsWithConfig = true)
            }

        public fun allowUnixSocketSubtree(value: Path): Builder =
            apply {
                pathOption("--allow-unix-socket-subtree", value, conflictsWithConfig = true)
            }

        public fun allowUnixSocketSubtreeBind(value: Path): Builder =
            apply {
                pathOption("--allow-unix-socket-subtree-bind", value, conflictsWithConfig = true)
            }

        public fun bypassProtection(value: Path): Builder =
            apply {
                pathOption("--bypass-protection", value, conflictsWithConfig = true)
            }

        public fun suppressSavePrompt(value: Path): Builder =
            apply {
                pathOption("--suppress-save-prompt", value, conflictsWithConfig = true)
            }

        public fun allowCurrentDirectory(): Builder =
            apply {
                flag("--allow-cwd", conflictsWithConfig = true)
            }

        public fun workdir(value: Path): Builder =
            apply {
                pathOption("--workdir", value)
            }

        public fun blockNetwork(): Builder =
            apply {
                blockNetworkSelected = true
                restrictedNetworkSelected = true
                flag("--block-net", conflictsWithConfig = true)
            }

        @Deprecated("Network is unrestricted by default in nono")
        public fun allowNetwork(): Builder =
            apply {
                allowNetworkSelected = true
                flag("--allow-net", conflictsWithConfig = true)
            }

        public fun networkProfile(value: String): Builder =
            apply {
                proxyModeSelected = true
                restrictedNetworkSelected = true
                option("--network-profile", value, conflictsWithConfig = true)
            }

        public fun allowDomain(value: String): Builder =
            apply {
                proxyModeSelected = true
                restrictedNetworkSelected = true
                option("--allow-domain", value, conflictsWithConfig = true)
            }

        public fun denyDomain(value: String): Builder =
            apply {
                restrictedNetworkSelected = true
                option("--deny-domain", value)
            }

        public fun listenPort(value: Int): Builder =
            apply {
                portOption("--listen-port", value, conflictsWithConfig = true)
            }

        public fun openPort(value: Int): Builder =
            apply {
                portOption("--open-port", value, conflictsWithConfig = true)
            }

        public fun allowConnectPort(value: Int): Builder =
            apply {
                portOption("--allow-connect-port", value, conflictsWithConfig = true)
            }

        public fun upstreamProxy(value: String): Builder =
            apply {
                proxyModeSelected = true
                upstreamProxySelected = true
                restrictedNetworkSelected = true
                option("--upstream-proxy", value, conflictsWithConfig = true)
            }

        public fun upstreamBypass(value: String): Builder =
            apply {
                upstreamBypassSelected = true
                restrictedNetworkSelected = true
                option("--upstream-bypass", value)
            }

        public fun proxyPort(value: Int): Builder =
            apply {
                proxyPortSelected = true
                restrictedNetworkSelected = true
                portOption("--proxy-port", value, conflictsWithConfig = true)
            }

        public fun trustProxyCa(): Builder =
            apply {
                flag("--trust-proxy-ca")
            }

        public fun proxyCaValidityDays(value: Int): Builder =
            apply {
                require(value in 1..365) { "proxy CA validity must be between 1 and 365 days" }
                option("--proxy-ca-validity", value.toString())
            }

        public fun credential(value: String): Builder =
            apply {
                proxyModeSelected = true
                proxyCredentialSelected = true
                restrictedNetworkSelected = true
                option("--credential", value, conflictsWithConfig = true)
            }

        public fun allowEndpoint(value: String): Builder =
            apply {
                allowEndpointSelected = true
                option("--allow-endpoint", value, conflictsWithConfig = true)
            }

        public fun environmentCredential(value: String): Builder =
            apply {
                option("--env-credential", value, conflictsWithConfig = true)
            }

        public fun environmentCredentialMap(
            reference: String,
            environmentVariable: String,
        ): Builder =
            apply {
                option(
                    "--env-credential-map",
                    reference,
                    environmentVariable,
                    conflictsWithConfig = true,
                )
            }

        @Deprecated("nono command allowlists are startup-only and deprecated")
        public fun allowCommand(value: String): Builder =
            apply {
                option("--allow-command", value, conflictsWithConfig = true)
            }

        @Deprecated("nono command denylists are startup-only and deprecated")
        public fun blockCommand(value: String): Builder =
            apply {
                option("--block-command", value, conflictsWithConfig = true)
            }

        public fun allowLaunchServices(): Builder =
            apply {
                flag("--allow-launch-services", conflictsWithConfig = true)
            }

        public fun allowGpu(): Builder =
            apply {
                flag("--allow-gpu", conflictsWithConfig = true)
            }

        /** Reject sandbox policies that let the child write to directories on the host PATH. */
        public fun strictBrokerPath(): Builder =
            apply {
                flag("--strict-broker-path")
            }

        public fun allowHttp2(): Builder =
            apply {
                flag("--allow-http2", conflictsWithConfig = true)
            }

        public fun linuxSandboxPolicy(value: LinuxSandboxPolicy): Builder =
            apply {
                option("--sandbox-policy", value.cliValue)
            }

        /**
         * Add Linux namespaces to the selected profile's isolation settings.
         * [LinuxNamespace.LOOPBACK] requires network isolation in the resulting policy.
         */
        public fun isolateLinuxNamespaces(vararg namespaces: LinuxNamespace): Builder =
            apply {
                require(namespaces.isNotEmpty()) { "at least one Linux namespace is required" }
                option("--isolate", namespaces.joinToString(",") { it.cliValue })
            }

        public fun memory(value: String): Builder =
            apply {
                option("--memory", value, conflictsWithConfig = true)
            }

        public fun maxProcesses(value: Long): Builder =
            apply {
                require(value >= 0) { "maximum processes must not be negative" }
                option("--max-processes", value.toString(), conflictsWithConfig = true)
            }

        public fun dryRun(): Builder =
            apply {
                flag("--dry-run")
            }

        public fun detached(): Builder =
            apply {
                detachedSelected = true
                flag("--detached")
            }

        public fun detachTimeout(value: Duration): Builder =
            apply {
                detachTimeoutSelected = true
                durationOption("--detach-timeout", value)
            }

        public fun rollback(): Builder =
            apply {
                rollbackSelected = true
                flag("--rollback")
            }

        public fun noRollbackPrompt(): Builder =
            apply {
                flag("--no-rollback-prompt")
            }

        public fun noRollback(): Builder =
            apply {
                noRollbackSelected = true
                flag("--no-rollback")
            }

        public fun rollbackExclude(value: String): Builder =
            apply {
                option("--rollback-exclude", value)
            }

        public fun rollbackInclude(value: String): Builder =
            apply {
                rollbackIncludeSelected = true
                option("--rollback-include", value)
            }

        public fun rollbackAll(): Builder =
            apply {
                rollbackAllSelected = true
                flag("--rollback-all")
            }

        public fun skipDirectory(value: String): Builder =
            apply {
                option("--skip-dir", value)
            }

        public fun rollbackDestination(value: Path): Builder =
            apply {
                rollbackDestinationSelected = true
                pathOption("--rollback-dest", value)
            }

        public fun silent(): Builder =
            apply {
                flag("--silent")
            }

        public fun noDiagnostics(): Builder =
            apply {
                flag("--no-diagnostics")
            }

        public fun diagnosticsJson(): Builder =
            apply {
                flag("--diagnostics-json")
            }

        public fun startupTimeout(value: Duration): Builder =
            apply {
                durationOption("--startup-timeout", value)
            }

        public fun noAudit(): Builder =
            apply {
                noAuditSelected = true
                flag("--no-audit")
            }

        public fun noAuditIntegrity(): Builder =
            apply {
                noAuditIntegritySelected = true
                flag("--no-audit-integrity")
            }

        public fun auditIntegrity(): Builder =
            apply {
                auditIntegritySelected = true
                flag("--audit-integrity")
            }

        public fun auditSignKey(value: String): Builder =
            apply {
                auditSignKeySelected = true
                option("--audit-sign-key", value)
            }

        public fun trustOverride(): Builder =
            apply {
                flag("--trust-override")
            }

        public fun sessionName(value: String): Builder =
            apply {
                option("--name", value)
            }

        public fun capabilityElevation(): Builder =
            apply {
                flag("--capability-elevation")
            }

        public fun verbose(level: Int): Builder =
            apply {
                require(level in 0..UByte.MAX_VALUE.toInt()) { "verbose level must fit in an unsigned byte" }
                repeat(level) {
                    flag("-v")
                }
            }

        public fun build(): NonoRunOptions {
            require(!configSelected || !configConflictingOptionSelected) {
                "config cannot be combined with sandbox configuration flags"
            }
            require(!extendsSelected || profileSelected) { "extends requires a profile" }
            require(!allowNetworkSelected || !restrictedNetworkSelected) {
                "allow-network cannot be combined with network restrictions or proxy options"
            }
            require(!blockNetworkSelected || !proxyModeSelected) {
                "block-network cannot be combined with proxy-mode options"
            }
            require(!allowEndpointSelected || proxyCredentialSelected || profileSelected) {
                "allow-endpoint requires a credential or profile"
            }
            require(!proxyPortSelected || proxyModeSelected || profileSelected) {
                "proxy port requires a proxy-mode option or profile"
            }
            require(!upstreamBypassSelected || upstreamProxySelected || profileSelected) {
                "upstream bypass requires an upstream proxy or profile"
            }
            require(!detachTimeoutSelected || detachedSelected) { "detach timeout requires detached mode" }
            require(!rollbackSelected || !noRollbackSelected) {
                "rollback and no-rollback are mutually exclusive"
            }
            require(!rollbackAllSelected || !rollbackIncludeSelected) {
                "rollback-all and rollback-include are mutually exclusive"
            }
            require(!rollbackDestinationSelected || rollbackSelected) {
                "rollback destination requires rollback"
            }
            require(!noAuditSelected || !(noAuditIntegritySelected || auditIntegritySelected || auditSignKeySelected || rollbackSelected)) {
                "no-audit conflicts with audit integrity, signing, and rollback"
            }
            require(!noAuditIntegritySelected || !(auditIntegritySelected || auditSignKeySelected || rollbackSelected)) {
                "no-audit-integrity conflicts with audit integrity, signing, and rollback"
            }
            return NonoRunOptions(arguments.toList())
        }

        private fun flag(
            flag: String,
            conflictsWithConfig: Boolean = false,
        ) {
            if (conflictsWithConfig) {
                configConflictingOptionSelected = true
            }
            arguments += flag
        }

        private fun option(
            flag: String,
            vararg values: String,
            conflictsWithConfig: Boolean = false,
        ) {
            if (conflictsWithConfig) {
                configConflictingOptionSelected = true
            }
            require(values.isNotEmpty()) { "option values must not be empty" }
            values.forEach(::validateOptionValue)
            arguments += flag
            arguments += values
        }

        private fun pathOption(
            flag: String,
            value: Path,
            conflictsWithConfig: Boolean = false,
        ) {
            val rendered = value.toString()
            require(rendered.isNotEmpty()) { "$flag path must not be empty" }
            option(flag, rendered, conflictsWithConfig = conflictsWithConfig)
        }

        private fun portOption(
            flag: String,
            value: Int,
            conflictsWithConfig: Boolean = false,
        ) {
            require(value in 0..65535) { "$flag port must be between 0 and 65535" }
            option(flag, value.toString(), conflictsWithConfig = conflictsWithConfig)
        }

        private fun durationOption(
            flag: String,
            value: Duration,
        ) {
            require(!value.isNegative) { "$flag duration must not be negative" }
            require(value.nano == 0) { "$flag duration must use whole seconds" }
            option(flag, value.seconds.toString())
        }

        private fun validateOptionValue(value: String) {
            require(value.isNotBlank()) { "option value must not be blank" }
            require('\u0000' !in value) { "option value must not contain NUL" }
        }
    }

    public companion object {
        @JvmStatic
        public fun builder(): Builder = Builder()
    }
}
