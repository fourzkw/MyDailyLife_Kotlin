package com.mydailylife.schedule.ui.screens.courses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mydailylife.schedule.data.SampleUiData
import com.mydailylife.schedule.ui.components.PrimaryPillButton
import com.mydailylife.schedule.ui.components.SectionHeader
import com.mydailylife.schedule.ui.components.horizontalSwipe
import com.mydailylife.schedule.ui.theme.Body
import com.mydailylife.schedule.ui.theme.CardShape
import com.mydailylife.schedule.ui.theme.Hairline
import com.mydailylife.schedule.ui.theme.Ink
import com.mydailylife.schedule.ui.theme.Muted
import com.mydailylife.schedule.ui.theme.OnSoftPrimary
import com.mydailylife.schedule.ui.theme.Rausch
import com.mydailylife.schedule.ui.theme.RauschSoft
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@Composable
fun CoursesScreen() {
    var weekOffset by remember { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val slotHeight = 52.dp
    val today = LocalDate.now()
    val thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekStart = thisMonday.plusWeeks(weekOffset.toLong())
    val weekDates = (0..6).map { weekStart.plusDays(it.toLong()) }
    val weekEnd = weekDates.last()
    val rangeFormatter = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)
    val weekRange = "${weekStart.format(rangeFormatter)} – ${weekEnd.format(rangeFormatter)}"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .horizontalSwipe(
                onSwipeLeft = { weekOffset += 1 },
                onSwipeRight = { weekOffset -= 1 },
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(
                title = "课表",
                action = if (weekOffset == 0) "本周" else weekRange,
                onAction = { weekOffset = 0 },
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = weekRange,
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.width(40.dp))
                weekDates.forEach { date ->
                    val isToday = date == today
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = SampleUiData.weekdays[date.dayOfWeek.value - 1],
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isToday) OnSoftPrimary else Muted,
                            fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                        )
                        Text(
                            text = "${date.monthValue}/${date.dayOfMonth}",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isToday) OnSoftPrimary else Ink,
                            fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                Box {
                    Column {
                        SampleUiData.timeSlots.forEach { time ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(slotHeight),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text(
                                    text = time,
                                    modifier = Modifier.width(40.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Muted,
                                )
                                repeat(7) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .border(0.5.dp, Hairline),
                                    )
                                }
                            }
                        }
                    }

                    SampleUiData.courseBlocks.forEach { course ->
                        val top = slotHeight * (course.startSlot - 1)
                        val height = slotHeight * (course.endSlot - course.startSlot + 1)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 40.dp)
                                .padding(top = top),
                        ) {
                            repeat(course.weekday - 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(height)
                                    .padding(2.dp)
                                    .clip(CardShape)
                                    .background(RauschSoft)
                                    .border(1.dp, Rausch.copy(alpha = 0.28f), CardShape)
                                    .padding(4.dp),
                            ) {
                                Column {
                                    Text(
                                        text = course.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Ink,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = course.location,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Body,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            repeat(7 - course.weekday) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            PrimaryPillButton(
                text = "导入课表",
                onClick = {
                    scope.launch {
                        snackbar.showSnackbar("ICS 订阅将在下一阶段接入")
                    }
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp),
        )
    }
}
