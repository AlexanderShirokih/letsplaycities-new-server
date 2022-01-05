package ru.quandastudio.lpsserver.data.entities.chat

import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter(autoApply = true)
class ChatTypeConverter : AttributeConverter<ChatType, String> {
    override fun convertToDatabaseColumn(attribute: ChatType?): String? {
        return attribute?.value
    }

    override fun convertToEntityAttribute(dbData: String?): ChatType {
        return ChatType.values().single { it.value == dbData }
    }
}
