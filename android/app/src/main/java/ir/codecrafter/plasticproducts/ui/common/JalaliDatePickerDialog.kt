package ir.codecrafter.plasticproducts.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.util.PersianDateFormatter
import java.time.LocalDate

/** Duplicated from PersianDateFormatter's private array — per-file duplication over a shared util, matching this project's habit (see statusLabel()/roleLabel() precedent). */
private val PERSIAN_MONTH_NAMES = arrayOf(
    "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
    "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
)

private val PERSIAN_WEEKDAY_LABELS = arrayOf("ش", "ی", "د", "س", "چ", "پ", "ج")

private val PERSIAN_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

private fun toPersianDigits(value: Int): String =
    value.toString().map { ch -> if (ch in '0'..'9') PERSIAN_DIGITS[ch - '0'] else ch }.joinToString("")

/**
 * Which column (شنبه=0 .. جمعه=6) the 1st of (jy, jm) falls under. Derived
 * from jalaliToGregorian + java.time's ISO day-of-week (MONDAY=1..SUNDAY=7):
 * (isoValue + 1) % 7 rotates SATURDAY(6)->0, SUNDAY(7)->1, MONDAY(1)->2, ...,
 * FRIDAY(5)->6 — i.e. the Persian week order starting on Saturday.
 */
private fun firstDayColumn(jy: Int, jm: Int): Int {
    val (gy, gm, gd) = PersianDateFormatter.jalaliToGregorian(jy, jm, 1)
    val isoValue = LocalDate.of(gy, gm, gd).dayOfWeek.value
    return (isoValue + 1) % 7
}

/**
 * A Jalali (Persian) calendar month-grid date picker. [initialJy]/[initialJm]
 * seed which month is shown first (the month of an already-selected date, if
 * any); both default to today's Jalali month/year when null. Selecting a day
 * calls [onDateSelected] with the raw (year, month, day) — conversion to/from
 * Gregorian for storage or API calls is the caller's responsibility, not this
 * dialog's.
 */
@Composable
fun JalaliDatePickerDialog(
    onDateSelected: (jy: Int, jm: Int, jd: Int) -> Unit,
    onDismiss: () -> Unit,
    initialJy: Int? = null,
    initialJm: Int? = null,
) {
    val today = remember {
        val now = LocalDate.now()
        PersianDateFormatter.gregorianToJalali(now.year, now.monthValue, now.dayOfMonth)
    }
    var displayedJy by remember { mutableIntStateOf(initialJy ?: today.first) }
    var displayedJm by remember { mutableIntStateOf(initialJm ?: today.second) }

    val daysInMonth = PersianDateFormatter.daysInJalaliMonth(displayedJy, displayedJm)
    val leadingBlanks = firstDayColumn(displayedJy, displayedJm)
    val rowCount = (leadingBlanks + daysInMonth + 6) / 7

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = {
                            if (displayedJm == 1) {
                                displayedJm = 12
                                displayedJy -= 1
                            } else {
                                displayedJm -= 1
                            }
                        },
                    ) {
                        Text(stringResource(R.string.btn_previous_month))
                    }
                    Text(
                        text = "${PERSIAN_MONTH_NAMES[displayedJm - 1]} ${toPersianDigits(displayedJy)}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    TextButton(
                        onClick = {
                            if (displayedJm == 12) {
                                displayedJm = 1
                                displayedJy += 1
                            } else {
                                displayedJm += 1
                            }
                        },
                    ) {
                        Text(stringResource(R.string.btn_next_month))
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    PERSIAN_WEEKDAY_LABELS.forEach { label ->
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                for (row in 0 until rowCount) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (col in 0 until 7) {
                            val day = row * 7 + col - leadingBlanks + 1
                            val isRealDay = day in 1..daysInMonth
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .let {
                                        if (isRealDay) {
                                            it.clickable {
                                                onDateSelected(displayedJy, displayedJm, day)
                                                onDismiss()
                                            }
                                        } else {
                                            it
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isRealDay) {
                                    Text(text = toPersianDigits(day), textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }
        },
    )
}
