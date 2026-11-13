package com.jaguar.gearbox.ui.components

import androidx.compose.runtime.saveable.Saver
import java.time.LocalDate

/** Save an explicit, Bundle-compatible value rather than a platform object. */
val LocalDateSaver = Saver<LocalDate, Long>(
    save = { it.toEpochDay() },
    restore = { LocalDate.ofEpochDay(it) },
)
