package org.sensors.demo.component

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.util.Arrays

@Converter
class IntListConverter : AttributeConverter<List<Int>?, Array<Int>?> {
    override fun convertToDatabaseColumn(attribute: List<Int>?): Array<Int>? {
        if (attribute == null) {
            return null
        }
        return attribute.toTypedArray<Int>()
    }

    override fun convertToEntityAttribute(dbData: Array<Int>?): List<Int>? {
        if (dbData == null) {
            return null
        }
        return Arrays.stream(dbData).toList()
    }
}