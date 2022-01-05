package ru.quandastudio.lpsserver.data.entities.chat

import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter(autoApply = true)
class PermissionLevelConverter : AttributeConverter<PermissionLevel, String> {
    override fun convertToDatabaseColumn(attribute: PermissionLevel?): String? {
        return attribute?.name?.lowercase()
    }

    override fun convertToEntityAttribute(dbData: String?): PermissionLevel {
        return PermissionLevel.values().single { it.name.lowercase() == dbData }
    }
}
