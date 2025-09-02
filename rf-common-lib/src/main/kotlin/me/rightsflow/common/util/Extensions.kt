package me.rightsflow.common.util

import java.time.LocalDate
import io.hypersistence.utils.hibernate.type.range.Range

fun Range<LocalDate>.realUpper(): LocalDate? =
    if (upper() == null) null else if (!isUpperBoundClosed) upper().minusDays(1) else upper()

fun Range<LocalDate>.realLower(): LocalDate? =
    if (lower() == null) null else if (!isLowerBoundClosed) lower().plusDays(1) else lower()