package nono.elonzh.com

import nono.elonzh.com.internal.RunArgumentRenderer
import java.nio.file.Path
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RunArgumentRendererTest {
    @Test
    fun `renders silent mode`() {
        val request =
            NonoRunRequest
                .builder("echo", "hello")
                .options(NonoRunOptions.builder().silent().build())
                .build()

        assertEquals(
            listOf("nono", "run", "--silent", "--", "echo", "hello"),
            RunArgumentRenderer.render(Path.of("nono"), request),
        )
    }

    @Test
    fun `renders Linux namespaces together as one argument`() {
        val options =
            NonoRunOptions
                .builder()
                .blockNetwork()
                .isolateLinuxNamespaces(LinuxNamespace.NET, LinuxNamespace.LOOPBACK, LinuxNamespace.IPC, LinuxNamespace.UTS)
                .build()

        assertEquals(listOf("--block-net", "--isolate", "net,loopback,ipc,uts"), options.arguments)
    }

    @Test
    fun `renders typed nono run flags without a shell`() {
        val options =
            NonoRunOptions
                .builder()
                .profile("locked")
                .extendProfile("ci")
                .allow(Path.of("/allow"))
                .read(Path.of("/read"))
                .write(Path.of("/write"))
                .allowFile(Path.of("/allow-file"))
                .readFile(Path.of("/read-file"))
                .writeFile(Path.of("/write-file"))
                .allowUnixSocket(Path.of("/socket"))
                .allowUnixSocketBind(Path.of("/socket-bind"))
                .allowUnixSocketDir(Path.of("/socket-dir"))
                .allowUnixSocketDirBind(Path.of("/socket-dir-bind"))
                .allowUnixSocketSubtree(Path.of("/socket-tree"))
                .allowUnixSocketSubtreeBind(Path.of("/socket-tree-bind"))
                .bypassProtection(Path.of("/protected"))
                .suppressSavePrompt(Path.of("/ignored"))
                .allowCurrentDirectory()
                .workdir(Path.of("/work"))
                .networkProfile("package-downloads")
                .allowDomain("repo.maven.apache.org")
                .denyDomain("*.invalid.example")
                .listenPort(8080)
                .openPort(8081)
                .allowConnectPort(443)
                .upstreamProxy("proxy.example:8443")
                .upstreamBypass("internal.example")
                .proxyPort(19090)
                .trustProxyCa()
                .proxyCaValidityDays(7)
                .credential("github")
                .allowEndpoint("github:GET:/repos/**")
                .environmentCredential("openai")
                .environmentCredentialMap("keyring://token", "TOKEN")
                .allowLaunchServices()
                .allowGpu()
                .strictBrokerPath()
                .allowHttp2()
                .linuxSandboxPolicy(LinuxSandboxPolicy.LANDLOCK)
                .memory("512M")
                .maxProcesses(32)
                .dryRun()
                .detached()
                .detachTimeout(Duration.ofSeconds(5))
                .rollback()
                .noRollbackPrompt()
                .rollbackExclude("node_modules")
                .rollbackAll()
                .skipDirectory("target")
                .rollbackDestination(Path.of("/allow/rollback"))
                .silent()
                .noDiagnostics()
                .diagnosticsJson()
                .startupTimeout(Duration.ofSeconds(10))
                .auditIntegrity()
                .auditSignKey("keyring://audit")
                .trustOverride()
                .sessionName("build")
                .capabilityElevation()
                .verbose(2)
                .build()
        val request =
            NonoRunRequest
                .builder("java", "-jar", "app.jar")
                .options(options)
                .build()

        assertEquals(
            listOf(
                "/opt/bin/nono",
                "run",
                "--profile",
                "locked",
                "--extends",
                "ci",
                "--allow",
                "/allow",
                "--read",
                "/read",
                "--write",
                "/write",
                "--allow-file",
                "/allow-file",
                "--read-file",
                "/read-file",
                "--write-file",
                "/write-file",
                "--allow-unix-socket",
                "/socket",
                "--allow-unix-socket-bind",
                "/socket-bind",
                "--allow-unix-socket-dir",
                "/socket-dir",
                "--allow-unix-socket-dir-bind",
                "/socket-dir-bind",
                "--allow-unix-socket-subtree",
                "/socket-tree",
                "--allow-unix-socket-subtree-bind",
                "/socket-tree-bind",
                "--bypass-protection",
                "/protected",
                "--suppress-save-prompt",
                "/ignored",
                "--allow-cwd",
                "--workdir",
                "/work",
                "--network-profile",
                "package-downloads",
                "--allow-domain",
                "repo.maven.apache.org",
                "--deny-domain",
                "*.invalid.example",
                "--listen-port",
                "8080",
                "--open-port",
                "8081",
                "--allow-connect-port",
                "443",
                "--upstream-proxy",
                "proxy.example:8443",
                "--upstream-bypass",
                "internal.example",
                "--proxy-port",
                "19090",
                "--trust-proxy-ca",
                "--proxy-ca-validity",
                "7",
                "--credential",
                "github",
                "--allow-endpoint",
                "github:GET:/repos/**",
                "--env-credential",
                "openai",
                "--env-credential-map",
                "keyring://token",
                "TOKEN",
                "--allow-launch-services",
                "--allow-gpu",
                "--strict-broker-path",
                "--allow-http2",
                "--sandbox-policy",
                "landlock",
                "--memory",
                "512M",
                "--max-processes",
                "32",
                "--dry-run",
                "--detached",
                "--detach-timeout",
                "5",
                "--rollback",
                "--no-rollback-prompt",
                "--rollback-exclude",
                "node_modules",
                "--rollback-all",
                "--skip-dir",
                "target",
                "--rollback-dest",
                "/allow/rollback",
                "--silent",
                "--no-diagnostics",
                "--diagnostics-json",
                "--startup-timeout",
                "10",
                "--audit-integrity",
                "--audit-sign-key",
                "keyring://audit",
                "--trust-override",
                "--name",
                "build",
                "--capability-elevation",
                "-v",
                "-v",
                "--",
                "java",
                "-jar",
                "app.jar",
            ),
            RunArgumentRenderer.render(Path.of("/opt/bin/nono"), request),
        )
    }

    @Test
    fun `generic command preserves argument boundaries`() {
        val request =
            NonoCommandRequest
                .builder("why", "--json", "--path", "a path with spaces", "")
                .build()

        assertEquals(
            listOf("/opt/bin/nono", "why", "--json", "--path", "a path with spaces", ""),
            RunArgumentRenderer.render(Path.of("/opt/bin/nono"), request),
        )
    }

    @Test
    fun `rejects config combined with sandbox flags`() {
        assertFailsWith<IllegalArgumentException> {
            NonoRunOptions
                .builder()
                .config(Path.of("/manifest.json"))
                .allow(Path.of("/work"))
                .build()
        }
    }

    @Test
    fun `rejects extends without profile`() {
        assertFailsWith<IllegalArgumentException> {
            NonoRunOptions.builder().extendProfile("base").build()
        }
    }

    @Test
    fun `rejects detach timeout without detached mode`() {
        assertFailsWith<IllegalArgumentException> {
            NonoRunOptions.builder().detachTimeout(Duration.ofSeconds(2)).build()
        }
    }

    @Test
    fun `rejects rollback conflicts`() {
        assertFailsWith<IllegalArgumentException> {
            NonoRunOptions
                .builder()
                .rollback()
                .noRollback()
                .build()
        }
        assertFailsWith<IllegalArgumentException> {
            NonoRunOptions
                .builder()
                .rollbackAll()
                .rollbackInclude("cache")
                .build()
        }
        assertFailsWith<IllegalArgumentException> {
            NonoRunOptions.builder().rollbackDestination(Path.of("/tmp/snapshots")).build()
        }
    }

    @Test
    fun `rejects audit conflicts`() {
        assertFailsWith<IllegalArgumentException> {
            NonoRunOptions
                .builder()
                .noAudit()
                .auditIntegrity()
                .build()
        }
        assertFailsWith<IllegalArgumentException> {
            NonoRunOptions
                .builder()
                .noAuditIntegrity()
                .auditSignKey("keyring://audit")
                .build()
        }
    }

    @Test
    fun `rejects explicit block network and proxy mode conflicts`() {
        listOf<(NonoRunOptions.Builder) -> Unit>(
            { it.networkProfile("strict") },
            { it.allowDomain("example.com") },
            { it.upstreamProxy("proxy.example:8080") },
            { it.credential("github") },
        ).forEach { configureProxy ->
            assertFailsWith<IllegalArgumentException> {
                NonoRunOptions
                    .builder()
                    .blockNetwork()
                    .also(configureProxy)
                    .build()
            }
        }
    }

    @Test
    fun `rejects incomplete proxy options without a profile fallback`() {
        assertFailsWith<IllegalArgumentException> {
            NonoRunOptions.builder().allowEndpoint("github:GET:/repos/**").build()
        }
        assertFailsWith<IllegalArgumentException> {
            NonoRunOptions.builder().proxyPort(8080).build()
        }
        assertFailsWith<IllegalArgumentException> {
            NonoRunOptions.builder().upstreamBypass("internal.example").build()
        }
    }

    @Test
    fun `rejects empty child command and NUL values`() {
        assertFailsWith<IllegalArgumentException> {
            NonoRunRequest.builder(emptyList()).build()
        }
        assertFailsWith<IllegalArgumentException> {
            NonoCommandRequest.builder("why", "bad\u0000value").build()
        }
    }
}
