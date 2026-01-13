package me.rightsflow.common.config

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

class EmptyStringAsNullSerializer : JsonSerializer<String>() {

    override fun serialize(value: String?, gen: JsonGenerator, provider: SerializerProvider) {
        if (value.isNullOrEmpty()) {
            gen.writeNull()
        } else {
            gen.writeString(value)
        }
    }

}