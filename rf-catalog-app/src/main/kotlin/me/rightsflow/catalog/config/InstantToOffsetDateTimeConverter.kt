package me.rightsflow.catalog.config

import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

@Component
class InstantToOffsetDateTimeConverter : Converter<Instant, OffsetDateTime> {
    override fun convert(source: Instant): OffsetDateTime =
        OffsetDateTime.ofInstant(source, ZoneId.systemDefault())
}