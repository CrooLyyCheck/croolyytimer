package pl.croolyy.timer.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
fun sha256File(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input -> val buffer = ByteArray(8192)
        while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

class CoverStore(private val context: Context) {
    val directory = File(context.filesDir, "covers").apply { mkdirs() }
    fun file(name: String): File {
        require(name.matches(Regex("[a-f0-9]{64}\\.jpg"))) { "Nieprawidłowa nazwa okładki." }
        return File(directory, name)
    }
    suspend fun import(uri: Uri): String = withContext(Dispatchers.IO) {
        val sourceFile = File.createTempFile("cover-", ".image", context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri)?.use { input -> sourceFile.outputStream().use { out ->
                val buffer = ByteArray(8192); var total = 0L
                while (true) {
                    val n = input.read(buffer); if (n < 0) break
                    total += n; require(total <= 30L * 1024 * 1024) { "Zdjęcie może mieć najwyżej 30 MB." }
                    out.write(buffer, 0, n)
                }
            } } ?: error("Nie można odczytać zdjęcia.")
            val bitmap = if (android.os.Build.VERSION.SDK_INT >= 28) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(sourceFile)) { decoder, info, _ ->
                    val scale = (maxOf(info.size.width, info.size.height) / 1400f).coerceAtLeast(1f)
                    decoder.setTargetSize((info.size.width / scale).toInt().coerceAtLeast(1),
                        (info.size.height / scale).toInt().coerceAtLeast(1))
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(sourceFile.path, bounds)
                require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Nieprawidłowy plik zdjęcia." }
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1400) sample *= 2
                requireNotNull(BitmapFactory.decodeFile(sourceFile.path, BitmapFactory.Options().apply { inSampleSize = sample }))
            }
            val bytes = java.io.ByteArrayOutputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out); out.toByteArray() }
            bitmap.recycle()
            val name = sha256(bytes) + ".jpg"
            install(name, bytes)
            name
        } finally { sourceFile.delete() }
    }
    fun install(name: String, bytes: ByteArray) {
        require(bytes.size <= 10 * 1024 * 1024 && sha256(bytes) + ".jpg" == name) { "Uszkodzona okładka." }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth in 1..4096 && bounds.outHeight in 1..4096) { "Nieprawidłowy format lub rozmiar okładki." }
        val target = file(name)
        if (!target.exists()) {
            val temp = File.createTempFile("install-", ".tmp", directory)
            try { temp.writeBytes(bytes); check(temp.renameTo(target)) { "Nie można zapisać okładki." } }
            finally { temp.delete() }
        }
    }
}
