package com.example.calendario.database

import com.example.calendario.DailyNote
import com.example.calendario.Festivo
import java.time.LocalDate
import java.time.LocalTime

fun EventEntity.toFestivo(): Festivo {
    return Festivo(
        id = this.googleId,
        title = this.title,
        description = this.description,
        date = LocalDate.parse(this.date),
        startTime = this.startTime?.let { LocalTime.parse(it) },
        endTime = this.endTime?.let { LocalTime.parse(it) },
        isAllDay = this.isAllDay,
        calendarId = this.calendarId,
        isFromHolidaySource = this.isFromHolidaySource,
        rrule = this.rrule,
        age = this.age,
        isBirthday = this.isBirthday,
        originalBirthDate = this.originalBirthDate?.let { LocalDate.parse(it) },
        isLongPeriod = this.isLongPeriod,
        lane = this.lane,
        totalDays = this.totalDays,
        currentDay = this.currentDay,
        customColor = this.customColor,
        fullStartMillis = this.fullStartMillis,
        fullEndMillis = this.fullEndMillis,
        repeatCount = this.repeatCount,
        repeatIndex = this.repeatIndex,
        adn = this.adn,
        lastModified = this.lastModified,
        isDeleted = this.isDeleted,
        isGhost = this.isGhost
    )
}

fun Festivo.toEntity(): EventEntity {
    return EventEntity(
        googleId = this.id,
        title = this.title,
        description = this.description,
        date = this.date.toString(),
        startTime = this.startTime?.toString(),
        endTime = this.endTime?.toString(),
        isAllDay = this.isAllDay,
        calendarId = this.calendarId,
        isFromHolidaySource = this.isFromHolidaySource,
        rrule = this.rrule,
        age = this.age,
        isBirthday = this.isBirthday,
        originalBirthDate = this.originalBirthDate?.toString(),
        isLongPeriod = this.isLongPeriod,
        lane = this.lane,
        totalDays = this.totalDays,
        currentDay = this.currentDay,
        customColor = this.customColor,
        fullStartMillis = this.fullStartMillis,
        fullEndMillis = this.fullEndMillis,
        repeatCount = this.repeatCount,
        repeatIndex = this.repeatIndex,
        adn = this.adn,
        lastModified = this.lastModified,
        isDeleted = this.isDeleted,
        isGhost = this.isGhost
    )
}

fun NoteEntity.toDailyNote(): DailyNote {
    return DailyNote(
        dateStr = this.dateStr,
        content = this.content,
        lastModified = this.lastModified,
        isDeleted = this.isDeleted
    )
}

fun DailyNote.toEntity(): NoteEntity {
    return NoteEntity(
        dateStr = this.dateStr,
        content = this.content,
        lastModified = this.lastModified,
        isDeleted = this.isDeleted
    )
}
