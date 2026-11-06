package pl.croolyy.timer.update

import android.content.Context
import android.content.Intent
import android.content.ClipData
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import pl.croolyy.timer.BuildConfig
import pl.croolyy.timer.data.sha256
import pl.croolyy.timer.network.GitHubHttp
import java.io.File

enum class UpdatePhase { IDLE, CHECKING, DOWNLOADING, VERIFYING }
data class UpdateState(
    val automatic: Boolean = true, val preview: Boolean = BuildConfig.DEBUG,
    val checkedAt: Long = 0, val release: AppRelease? = null, val phase: UpdatePhase = UpdatePhase.IDLE,
    val progress: Float = 0f, val ready: Boolean = false, val hidden: Boolean = false, val error: String? = null,
) { val busy: Boolean get() = phase != UpdatePhase.IDLE }

class UpdateManager(private val context: Context, private val scope: CoroutineScope) {
    private val preferences = context.getSharedPreferences("updates", Context.MODE_PRIVATE)
    private val directory = File(context.cacheDir, "updates").apply { mkdirs() }
    private val catalogFile = File(context.filesDir, "update-catalog.json")
    private val mutable = MutableStateFlow(UpdateState(
        automatic = preferences.getBoolean("automatic", true), preview = preferences.getBoolean("preview", BuildConfig.DEBUG),
        checkedAt = preferences.getLong("checked", 0),
    ))
    val state = mutable.asStateFlow()
    private var catalog: UpdateCatalog? = null
    private var checkJob: Job? = null
    private var downloadJob: Job? = null
    private val verifier = ArtifactVerifier({ installedIdentity() }, { archiveIdentity(it) }, Build.VERSION.SDK_INT)
    private val initial = scope.async {
        withContext(Dispatchers.IO) {
            if (catalogFile.isFile && catalogFile.length() <= 65_536) runCatching {
                catalog = UpdateCatalog.parse(catalogFile.readText()); select()
            }
        }
    }
    @Suppress("DEPRECATION")
    private fun identity(info: PackageInfo): ApkIdentity {
        val signatures = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
        val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        return ApkIdentity(info.packageName, code, info.applicationInfo?.minSdkVersion ?: 26,
            signatures.orEmpty().map { sha256(it.toByteArray()) }.toSet())
    }
    @Suppress("DEPRECATION")
    private fun flags() = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
    @Suppress("DEPRECATION")
    private fun installedIdentity() = identity(context.packageManager.getPackageInfo(context.packageName, flags()))
    @Suppress("DEPRECATION")
    private fun archiveIdentity(file: File) = identity(requireNotNull(context.packageManager.getPackageArchiveInfo(file.path, flags())) { "Nie można odczytać pliku aktualizacji." })
    private fun apk(release: AppRelease) = File(directory, "${release.versionCode}-${release.sha256.take(16)}.apk")
    private fun select() {
        val release = catalog?.newest(BuildConfig.VERSION_CODE.toLong(), Build.VERSION.SDK_INT, mutable.value.preview)
        val ready = release?.let { candidate -> runCatching { verifier.verify(apk(candidate), candidate); true }.getOrDefault(false) } ?: false
        mutable.update { it.copy(release = release, ready = ready, hidden = release?.tag == preferences.getString("hidden", null)) }
    }
    fun setAutomatic(enabled: Boolean) {
        preferences.edit().putBoolean("automatic", enabled).apply(); mutable.update { it.copy(automatic = enabled) }
        if (enabled) check(false)
    }
    fun setPreview(enabled: Boolean) {
        if (mutable.value.busy) return
        preferences.edit().putBoolean("preview", enabled).apply(); mutable.update { it.copy(preview = enabled, error = null) }
        scope.launch { initial.await(); withContext(Dispatchers.IO) { select() } }
    }
    fun hideBanner() {
        preferences.edit().putString("hidden", mutable.value.release?.tag).apply(); mutable.update { it.copy(hidden = true) }
    }
    fun check(manual: Boolean = true) {
        if (mutable.value.busy || checkJob?.isActive == true) return
        val now = System.currentTimeMillis()
        val previousAttempt = preferences.getLong("attempt", 0)
        if (!manual && (!mutable.value.automatic || (previousAttempt > 0 && now >= previousAttempt && now - previousAttempt < 6 * 60 * 60_000L))) return
        preferences.edit().putLong("attempt", now).apply()
        mutable.update { it.copy(phase = UpdatePhase.CHECKING, error = null) }
        checkJob = scope.launch {
            try {
                initial.await()
                withContext(Dispatchers.IO) {
                    val body = GitHubHttp.bytes(GitHubHttp.RAW + "updates.json?t=$now", 65_536).toString(Charsets.UTF_8)
                    val parsed = UpdateCatalog.parse(body)
                    val temp = File(context.filesDir, "update-catalog.tmp")
                    temp.writeText(body)
                    java.nio.file.Files.move(temp.toPath(), catalogFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                    catalog = parsed; select()
                }
                preferences.edit().putLong("checked", now).apply()
                mutable.update { it.copy(checkedAt = now) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (manual) mutable.update { it.copy(error = "Nie udało się sprawdzić aktualizacji. Sprawdź internet i spróbuj ponownie.") }
            } finally { mutable.update { it.copy(phase = UpdatePhase.IDLE) } }
        }
    }
    fun download() {
        val release = mutable.value.release ?: return
        if (mutable.value.busy || downloadJob?.isActive == true) return
        mutable.update { it.copy(phase = UpdatePhase.DOWNLOADING, progress = 0f, error = null, ready = false) }
        downloadJob = scope.launch {
            val partial = File(directory, "download.part")
            try {
                withContext(Dispatchers.IO) {
                    partial.outputStream().use { output ->
                        GitHubHttp.download(release.downloadUrl, output, release.size) { count ->
                            mutable.update { it.copy(progress = (count.toFloat() / release.size).coerceIn(0f, 1f)) }
                        }
                    }
                    mutable.update { it.copy(phase = UpdatePhase.VERIFYING) }
                    verifier.verify(partial, release)
                    java.nio.file.Files.move(partial.toPath(), apk(release).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                }
                mutable.update { it.copy(ready = true) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                mutable.update { it.copy(error = if (e is IllegalArgumentException) e.message else "Pobieranie przerwane. Sprawdź internet i spróbuj ponownie.") }
            } finally { withContext(NonCancellable + Dispatchers.IO) { partial.delete() }; mutable.update { it.copy(phase = UpdatePhase.IDLE) } }
        }
    }
    fun cancelDownload() { downloadJob?.cancel() }
    /** Revalidate immediately before handing a private, read-only URI to Android's installer. */
    suspend fun installIntent(): Intent = withContext(Dispatchers.IO) {
        initial.await()
        val release = requireNotNull(mutable.value.release) { "Najpierw sprawdź dostępność aktualizacji." }
        val file = apk(release)
        try { verifier.verify(file, release) }
        catch (e: Exception) { mutable.update { it.copy(ready = false, error = "Plik aktualizacji wymaga ponownego pobrania.") }; throw e }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).apply { clipData = ClipData.newRawUri("CrooLyyTimer", uri) }
    }
}
