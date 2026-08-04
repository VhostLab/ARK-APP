package com.arkapp.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.awt.Desktop
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.time.Duration

data class UpdateInfo(val version: String, val msiUrl: String)

/**
 * Checks GitHub Releases for a newer version. All failures are swallowed —
 * the app must work identically without network access.
 */
object UpdateChecker {

    const val REPO = "VhostLab/ARK-APP"

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class GithubRelease(
        @SerialName("tag_name") val tagName: String = "",
        val assets: List<GithubAsset> = emptyList(),
    )

    @Serializable
    private data class GithubAsset(
        val name: String = "",
        @SerialName("browser_download_url") val browserDownloadUrl: String = "",
    )

    fun currentVersion(): String = System.getProperty("app.version") ?: "0.0.0"

    fun check(): UpdateInfo? = runCatching {
        val client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build()
        val request = HttpRequest.newBuilder(URI("https://api.github.com/repos/$REPO/releases/latest"))
            .timeout(Duration.ofSeconds(10))
            .header("Accept", "application/vnd.github+json")
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) return null
        val release = json.decodeFromString(GithubRelease.serializer(), response.body())
        val latest = release.tagName.removePrefix("v")
        if (!isNewer(latest, currentVersion())) return null
        val msi = release.assets.firstOrNull { it.name.endsWith(".msi", ignoreCase = true) } ?: return null
        UpdateInfo(latest, msi.browserDownloadUrl)
    }.getOrNull()

    fun isNewer(candidate: String, current: String): Boolean {
        fun parse(v: String) = v.split(".").map { part -> part.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val a = parse(candidate)
        val b = parse(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /**
     * Downloads the MSI and hands off to a windowless script (wscript) that runs
     * the installer (passive UI), waits for it to finish and relaunches the app.
     * On success the caller must exit so msiexec can replace the files.
     */
    fun downloadAndLaunchInstaller(info: UpdateInfo): Boolean = runCatching {
        val client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build()
        val dir = Files.createTempDirectory("prodigiosos-update")
        val target = dir.resolve("Prodigiosos App-${info.version}.msi")
        val request = HttpRequest.newBuilder(URI(info.msiUrl)).build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofFile(target))
        if (response.statusCode() != 200) return false
        // Relaunch only when running as the installed exe (not from a dev JVM).
        val exe = ProcessHandle.current().info().command().orElse("")
        val relaunch =
            if (exe.endsWith("Prodigiosos App.exe", ignoreCase = true)) {
                "sh.Run \"\"\"$exe\"\"\", 1, False\r\n"
            } else ""
        val script = dir.resolve("update.vbs")
        Files.writeString(
            script,
            "Set sh = CreateObject(\"WScript.Shell\")\r\n" +
                "sh.Run \"msiexec /i \"\"$target\"\" /passive\", 1, True\r\n" +
                relaunch,
        )
        ProcessBuilder("wscript", script.toString()).start()
        true
    }.getOrDefault(false)

    /** Fallback when the direct download fails: open the releases page. */
    fun openReleasesPage() {
        runCatching { Desktop.getDesktop().browse(URI("https://github.com/$REPO/releases/latest")) }
    }
}
