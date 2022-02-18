package ru.citeck.ecos.history.converter

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.history.domain.HistoryRecordEntity
import ru.citeck.ecos.history.dto.HistoryRecordDto

@Component
class HistoryRecordConverter {

    fun toDto(entity: HistoryRecordEntity): HistoryRecordDto {

        val result = HistoryRecordDto()

        result.uuid = entity.id?.toString() ?: "no_uuid"
        result.historyEventId = entity.historyEventId
        result.userId = entity.userId
        result.username = entity.username
        result.comments = entity.comments
        result.version = entity.version
        result.eventType = entity.eventType
        result.creationTime = entity.creationTime?.time ?: 0L
        result.taskTitle = entity.taskTitle
        result.taskRole = entity.taskRole
        result.taskOutcome = entity.taskOutcome
        result.taskOutcomeName = entity.taskOutcomeName
        result.taskDefinitionKey = entity.taskDefinitionKey
        result.taskType = entity.fullTaskType
        result.documentId = entity.documentId

        return result
    }

    fun toDto(sources: List<HistoryRecordEntity>): List<HistoryRecordDto> {
        return sources.map { toDto(it) }
    }

    fun toMap(dto: HistoryRecordDto): Map<String, String> {
        return DataValue.create(dto)
            .asMap(String::class.java, String::class.java)
    }
}
