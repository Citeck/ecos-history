package ru.citeck.ecos.history.converter

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.json.Json
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
        result.creationTime = entity.creationTime?.toEpochMilli() ?: 0L
        result.taskTitle = entity.taskTitle
        result.taskRole = entity.taskRole
        result.taskOutcome = entity.taskOutcome
        result.taskOutcomeName = entity.taskOutcomeName
        result.taskDefinitionKey = entity.taskDefinitionKey
        result.taskType = entity.fullTaskType
        result.taskCompletedOnBehalfOf = entity.taskCompletedOnBehalfOf
        result.documentId = entity.documentId

        return result
    }

    fun toDto(sources: List<HistoryRecordEntity>): List<HistoryRecordDto> {
        return sources.map { toDto(it) }
    }

    fun toMap(dto: HistoryRecordDto): Map<String, String> {
        val mapType = Json.mapper.getMapType(String::class.java, String::class.java)
        val resultWithNull = Json.mapper.convert<Map<String, String?>>(dto, mapType)
            ?: error("Error while dto-to-map conversion. Dto: $dto")
        val result = HashMap<String, String>()
        for ((k, v) in resultWithNull) {
            if (v != null) {
                result[k] = v
            }
        }
        return result
    }
}
