package pl.croolyy.timer.update

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pl.croolyy.timer.network.GitHubHttp
import pl.croolyy.timer.data.sha256File
import java.io.File

const val MAX_APK_BYTES = 128L * 1024 * 1024
@Serializable data class AppRelease(
    val versionCode: Long, val versionName: String, val tag: String,
    val apkName: String, val sha256: String, val size: Long, val minSdk: Int,
    val signerSha256: Set<String>, val notes: String = "",
) {
    val downloadUrl: String get() = "${GitHubHttp.REPO}/releases/download/$tag/$apkName"
    val releaseUrl: String get() = "${GitHubHttp.REPO}/releases/tag/$tag"
    fun validate() {
        require(versionCode > 0 && versionName.matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+(?:-[A-Za-z0-9.-]+)?")))
        require(tag == "v$versionName")
        require(apkName.matches(Regex("CrooLyyTimer-${Regex.escape(versionName)}-(debug|release)\\.apk")))
        require(sha256.matches(Regex("[a-f0-9]{64}")) && size in 1..MAX_APK_BYTES && minSdk in 26..1000)
        require(signerSha256.isNotEmpty() && signerSha256.size <= 4 && signerSha256.all { it.matches(Regex("[a-f0-9]{64}")) })
        require(notes.length <= 4000)
    }
}
@Serializable data class UpdateCatalog(val schemaVersion: Int = 1, val stable: AppRelease? = null, val preview: AppRelease? = null) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun parse(body: String): UpdateCatalog = json.decodeFromString<UpdateCatalog>(body).also {
            require(it.schemaVersion == 1); it.stable?.validate(); it.preview?.validate()
        }
    }
    fun newest(current: Long, sdk: Int, includePreview: Boolean): AppRelease? =
        listOfNotNull(stable, preview.takeIf { includePreview }).filter { it.versionCode > current && it.minSdk <= sdk }.maxByOrNull { it.versionCode }
}
data class ApkIdentity(val packageName: String, val versionCode: Long, val minSdk: Int, val signers: Set<String>)

/** Certificate equality is checked here and the Android installer verifies APK signatures on installation. */
class ArtifactVerifier(private val installed: () -> ApkIdentity, private val archive: (File) -> ApkIdentity, private val sdk: Int) {
    fun verify(file: File, release: AppRelease) {
        release.validate()
        require(file.isFile && file.length() == release.size && sha256File(file) == release.sha256) { "Plik aktualizacji jest niekompletny lub uszkodzony. Pobierz go ponownie." }
        val old = installed(); val next = archive(file)
        require(old.packageName == "pl.croolyy.timer" && next.packageName == old.packageName) { "To nie jest aktualizacja CrooLyyTimer." }
        require(next.versionCode == release.versionCode && next.versionCode > old.versionCode) { "Plik zawiera nieprawidłową wersję aplikacji." }
        require(next.minSdk == release.minSdk && next.minSdk <= sdk) { "Ta aktualizacja wymaga nowszego Androida." }
        require(old.signers.isNotEmpty() && old.signers == next.signers && old.signers == release.signerSha256) { "Podpis aktualizacji nie pasuje do zainstalowanej aplikacji." }
    }
}
