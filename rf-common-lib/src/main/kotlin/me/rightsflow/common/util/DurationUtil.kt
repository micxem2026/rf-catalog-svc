package me.rightsflow.common.util

import java.time.Duration

object DurationUtil {

    /**
     * Parses a string in the format HH:mm:ss and returns a [Duration] object.
     *
     * @param s The string to parse. If null or blank, returns null.
     * @return A [Duration] representing the parsed duration, or null if the string is null or blank.
     * @throws IllegalArgumentException if the string is not in the format HH:mm:ss.
     */
    fun fromStringHHmmss(s: String?): Duration? =
        s?.takeIf { it.isNotBlank() }?.let {
            val parts = it.split(":")
            require(parts.size == 3) { "Duration must be HH:mm:ss" }
            val h = parts[0].toLong()
            val m = parts[1].toLong()
            val sec = parts[2].toLong()
            Duration.ofHours(h).plusMinutes(m).plusSeconds(sec)
        }

    /**
     * Converts a [Duration] to a string in the format "HH:mm:ss".
     *
     * @param d the [Duration] to convert, or null to return null
     * @return the string representation of the [Duration], or null if `d` is null
     */
    fun toStringHHmmss(d: Duration?): String? =
        d?.let {
            val total = it.seconds
            val h = total / 3600
            val m = (total % 3600) / 60
            val s = total % 60
            "%02d:%02d:%02d".format(h, m, s)
        }
}