package pl.croolyy.timer.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.croolyy.timer.data.*
import pl.croolyy.timer.domain.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

private fun dayLabel(date: LocalDate) = date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Polish))

@Composable fun BookStatistics(book: Book, sessions: List<ReadingSession>, onCalendar: () -> Unit) {
    val analytics = remember(sessions) { ReadingAnalytics(sessions) }
    val today = LocalDate.now()
    var range by rememberSaveable(book.id) { mutableIntStateOf(7) }
    var selected by rememberSaveable(book.id) { mutableStateOf(today.toString()) }
    val days = remember(analytics, range, today) { analytics.recentDays(today, range) }
    val chosen = analytics.day(LocalDate.parse(selected))
    SoftCard(Modifier.fillMaxWidth().testTag("book-statistics")) {
        SectionTitle("Twój rytm czytania")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(7, 30).forEach { count -> FilterChip(range == count, { range = count; selected = today.toString() }, label = { Text("$count dni") }) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Metric(shortTime(days.sumOf { it.durationMs }), "w tym okresie", Modifier.weight(1f))
            Metric(days.count { it.sessions.isNotEmpty() }.toString(), "dni z czytaniem", Modifier.weight(1f))
        }
        DailyReadingChart(days, LocalDate.parse(selected)) { selected = it.toString() }
        Text("${dayLabel(chosen.date)} · ${shortTime(chosen.durationMs)} · ${sessionCount(chosen.sessions.size)}",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Metric(analytics.longestMs?.let(::shortTime) ?: "—", "najdłuższa sesja", Modifier.weight(1f))
            Metric(analytics.daysWithSessions.toString(), "dni z sesją łącznie", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Metric(analytics.longestStreak().toString(), "najdłuższa seria dni", Modifier.weight(1f))
            Metric(analytics.pagesPerHour?.let { "%.1f".format(Polish, it) } ?: "—", "stron na godzinę", Modifier.weight(1f))
        }
        analytics.estimatedRemainingMs(book)?.takeIf { !book.finished && it > 0 }?.let { remaining ->
            Text("Do końca około ${shortTime(remaining)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text("Na podstawie Twojego dotychczasowego tempa.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onCalendar, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.CalendarMonth, null); Spacer(Modifier.width(8.dp)); Text("Zobacz kalendarz tej książki") }
    }
}

@Composable fun DailyReadingChart(days: List<ReadingDay>, selected: LocalDate, onSelect: (LocalDate) -> Unit) {
    val maxTime = days.maxOfOrNull { it.durationMs }?.coerceAtLeast(1) ?: 1
    val primary = MaterialTheme.colorScheme.primary
    Row(Modifier.fillMaxWidth().height(152.dp).testTag("reading-chart"), horizontalArrangement = Arrangement.spacedBy(if (days.size > 7) 3.dp else 8.dp),
        verticalAlignment = Alignment.Bottom) {
        days.forEachIndexed { index, day ->
            Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(6.dp)).clickable { onSelect(day.date) }
                .semantics(mergeDescendants = true) { contentDescription = "${dayLabel(day.date)}, ${duration(day.durationMs)}" },
                verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().height(118.dp), contentAlignment = Alignment.BottomCenter) {
                    Box(Modifier.fillMaxWidth().height(if (day.durationMs == 0L) 3.dp else (8 + 108 * day.durationMs.toFloat() / maxTime).dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(if (day.date == selected) primary else if (day.durationMs > 0) primary.copy(alpha = .55f) else MaterialTheme.colorScheme.outlineVariant))
                }
                Spacer(Modifier.height(8.dp))
                Text(if (days.size <= 7) day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Polish)
                    else if (index % 5 == 0 || index == days.lastIndex) day.date.dayOfMonth.toString() else "",
                    fontSize = if (days.size > 7) 8.sp else 11.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable fun CalendarScreen(state: LibraryState, coverFile: (String) -> java.io.File?, initialBookId: String? = null, onBook: (String) -> Unit) {
    val today = LocalDate.now()
    var monthText by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    var selectedText by rememberSaveable { mutableStateOf(today.toString()) }
    var bookId by rememberSaveable(initialBookId) { mutableStateOf(initialBookId) }
    val month = YearMonth.parse(monthText)
    val selected = LocalDate.parse(selectedText)
    val sessions = remember(state.sessions, bookId) { state.sessions.filter { bookId == null || it.bookId == bookId } }
    val analytics = remember(sessions) { ReadingAnalytics(sessions) }
    val monthDays = remember(analytics, month) { analytics.month(month) }
    val chosen = analytics.day(selected)
    fun moveMonth(delta: Long) {
        val next = month.plusMonths(delta)
        monthText = next.toString()
        selectedText = next.atDay(minOf(selected.dayOfMonth, next.lengthOfMonth())).toString()
    }
    LazyColumn(Modifier.fillMaxSize().testTag("calendar-list"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { SectionTitle("Kalendarz czytania", "Twoje historie, dzień po dniu.") }
        item { LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(bookId == null, { bookId = null }, label = { Text("Wszystkie książki") }) }
            items(state.books, key = { it.id }) { book -> FilterChip(bookId == book.id, { bookId = book.id },
                label = { Text(book.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 190.dp)) }) }
        } }
        item { SoftCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton({ moveMonth(-1) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Poprzedni miesiąc") }
                Text(month.month.getDisplayName(TextStyle.FULL_STANDALONE, Polish).replaceFirstChar { it.uppercase() } + " ${month.year}",
                    Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge)
                IconButton({ moveMonth(1) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Następny miesiąc") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Metric(shortTime(monthDays.sumOf { it.durationMs }), "w tym miesiącu", Modifier.weight(1f))
                Metric(monthDays.count { it.sessions.isNotEmpty() }.toString(), "dni z sesją", Modifier.weight(1f))
            }
            MonthHeatmap(month, monthDays, selected, today) { selectedText = it.toString() }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Mniej", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                (0..4).forEach { level -> Box(Modifier.size(13.dp).clip(RoundedCornerShape(3.dp)).background(heatColor(level))) }
                Text("Więcej", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                TextButton({ monthText = YearMonth.from(today).toString(); selectedText = today.toString() }) { Text("Dzisiaj") }
            }
            Text("Sesje według daty rozpoczęcia.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } }
        item { SectionTitle(dayLabel(selected), if (chosen.sessions.isEmpty()) "Dzień bez zapisanej sesji" else "${shortTime(chosen.durationMs)} · ${sessionCount(chosen.sessions.size)} · ${bookCount(chosen.bookCount)}") }
        if (chosen.sessions.isEmpty()) item { EmptyState(Icons.Outlined.CalendarToday, "Jeszcze nie zapisano tu czytania", "Wybierz inny dzień lub rozpocznij kolejną sesję.") }
        items(chosen.sessions, key = { it.id }) { session ->
            val book = state.books.find { it.id == session.bookId }
            if (book != null) Surface(onClick = { onBook(book.id) }, shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth().testTag("calendar-session-${session.id}")) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Cover(book, book.cover?.let(coverFile), Modifier.width(64.dp).height(96.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(state.timers.find { it.id == session.timerId }?.name ?: "Czytanie", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(duration(session.durationMs), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Text("${java.time.Instant.ofEpochMilli(session.startedAt).atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))} · str. ${session.startPage ?: "—"} → ${session.endPage ?: "—"}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable private fun heatColor(level: Int): Color = if (level == 0) MaterialTheme.colorScheme.surfaceVariant else
    MaterialTheme.colorScheme.primary.copy(alpha = listOf(0f, .22f, .42f, .68f, 1f)[level.coerceIn(0, 4)])

@Composable fun MonthHeatmap(month: YearMonth, days: List<ReadingDay>, selected: LocalDate, today: LocalDate, onSelect: (LocalDate) -> Unit) {
    val offset = month.atDay(1).dayOfWeek.value - 1
    val cells = ((offset + month.lengthOfMonth() + 6) / 7) * 7
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("Pn", "Wt", "Śr", "Cz", "Pt", "So", "Nd").forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        (0 until cells / 7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                (0..6).forEach { weekday ->
                    val number = week * 7 + weekday - offset + 1
                    if (number !in 1..month.lengthOfMonth()) Spacer(Modifier.weight(1f).aspectRatio(1f))
                    else {
                        val day = days[number - 1]
                        val border = if (day.date == selected) MaterialTheme.colorScheme.primary else if (day.date == today)
                            MaterialTheme.colorScheme.outline else Color.Transparent
                        Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(10.dp)).background(heatColor(day.intensity))
                            .border(if (day.date == selected) 2.dp else 1.dp, border, RoundedCornerShape(10.dp))
                            .clickable { onSelect(day.date) }.testTag("calendar-day-${day.date}")
                            .semantics { contentDescription = "${dayLabel(day.date)}, ${duration(day.durationMs)}, ${sessionCount(day.sessions.size)}" },
                            contentAlignment = Alignment.Center) {
                            Text(number.toString(), fontSize = 13.sp, fontWeight = if (day.date == selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (day.intensity >= 4) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}
