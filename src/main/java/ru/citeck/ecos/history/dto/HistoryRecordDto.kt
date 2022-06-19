package ru.citeck.ecos.history.dto

import lombok.Data
import java.io.Serializable

@Data
class HistoryRecordDto : Serializable {

    companion object {
        private const val serialVersionUID = -8215328451338606217L
    }

    var uuid: String? = null
    var historyEventId: String? = null
    var comments: String? = null
    var version: String? = null
    var username: String? = null
    var userId: String? = null
    var eventType: String? = null
    var creationTime: Long? = null
    var taskTitle: String? = null
    var taskRole: String? = null
    var taskOutcome: String? = null
    var taskOutcomeName: String? = null
    var taskDefinitionKey: String? = null
    var taskType: String? = null
    var documentId: String? = null

    constructor() {}

    constructor(other: HistoryRecordDto) {
        uuid = other.uuid
        historyEventId = other.historyEventId
        comments = other.comments
        version = other.version
        username = other.username
        userId = other.userId
        eventType = other.eventType
        creationTime = other.creationTime
        taskTitle = other.taskTitle
        taskRole = other.taskRole
        taskOutcome = other.taskOutcome
        taskOutcomeName = other.taskOutcomeName
        taskDefinitionKey = other.taskDefinitionKey
        taskType = other.taskType
        documentId = other.documentId
    }
}
