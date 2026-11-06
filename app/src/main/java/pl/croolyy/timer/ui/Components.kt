package pl.croolyy.timer.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.croolyy.timer.data.Book
import java.io.File
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

val Polish: Locale = Locale.forLanguageTag("pl-PL")
fun duration(ms: Long): String { val s = ms.coerceAtLeast(0) / 1000; return "%02d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60) }
fun shortTime(ms: Long): String { val m = ms.coerceAtLeast(0) / 60000; return if (m >= 60) "${m / 60} godz. ${m % 60} min" else "$m min" }
fun dateTime(time: Long): String = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Polish))

@Composable fun Brand(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.AutoStories, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(23.dp))
        }
        Column {
            Text("CrooLyyTimer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("TWÓJ CZAS MIĘDZY STRONAMI", fontSize = 8.sp, letterSpacing = 1.4.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable fun Cover(book: Book, file: File?, modifier: Modifier = Modifier) {
    val bitmap by produceState<ImageBitmap?>(null, file?.absolutePath) {
        value = if (file?.isFile == true) withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, bounds)
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 600) sample *= 2
                BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
            }.getOrNull()
        } else null
    }
    val palettes = listOf(Color(0xFF49634D), Color(0xFF765748), Color(0xFF4F6073), Color(0xFF766449), Color(0xFF5E546A))
    val base = palettes[(book.id.hashCode() and Int.MAX_VALUE) % palettes.size]
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(base)) {
        if (bitmap != null) Image(bitmap!!, "Okładka: ${book.title}", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Brush.linearGradient(listOf(Color.White.copy(alpha = .12f), Color.Transparent, Color.Black.copy(alpha = .25f))))
                drawRect(Color.Black.copy(alpha = .13f), size = androidx.compose.ui.geometry.Size(size.width * .08f, size.height))
                drawCircle(Color(0xFFE3D6AD).copy(alpha = .16f), size.width * .65f,
                    center = androidx.compose.ui.geometry.Offset(size.width * .9f, size.height * .78f))
                drawLine(Color(0xFFE3D6AD).copy(alpha = .5f), androidx.compose.ui.geometry.Offset(size.width * .2f, size.height * .2f),
                    androidx.compose.ui.geometry.Offset(size.width * .8f, size.height * .2f), 2f)
            }
            Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 18.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Text(book.title, color = Color(0xFFF9F1DD), fontFamily = FontFamily.Serif, fontSize = 15.sp,
                    lineHeight = 18.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Outlined.LocalFlorist, null, tint = Color(0xFFD8C99E), modifier = Modifier.align(Alignment.CenterHorizontally).size(24.dp))
                Text(book.author.ifBlank { "CROOLYY LIBRARY" }, color = Color(0xFFE8DECB), fontSize = 8.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable fun SectionTitle(title: String, subtitle: String? = null, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            if (subtitle != null) Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        action?.invoke()
    }
}
@Composable fun SoftCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f))) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
@Composable fun Metric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable fun Progress(book: Book) {
    book.totalPages?.let { total ->
        val p = (book.currentPage.toFloat() / total).coerceIn(0f, 1f)
        val animated by animateFloatAsState(p, label = "Postęp książki")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Strona ${book.currentPage} z $total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${(p * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        LinearProgressIndicator(progress = { animated }, Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
            trackColor = MaterialTheme.colorScheme.surfaceVariant, drawStopIndicator = {})
    } ?: Text("Aktualna strona: ${book.currentPage}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
}
@Composable fun EmptyState(icon: ImageVector, title: String, text: String, action: (@Composable () -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        action?.invoke()
    }
}
@Composable fun ConfirmDialog(title: String, text: String, confirm: String = "Usuń", onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(text) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } })
}
