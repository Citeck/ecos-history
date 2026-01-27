package ru.citeck.ecos.history.service

import ru.citeck.ecos.history.domain.HistoryRecordEntity
import ru.citeck.ecos.history.dto.HistoryRecordDto
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.webapp.api.entity.EntityRef

interface HistoryRecordService {

    companion object {
        const val HISTORY_EVENT_ID: String = "historyEventId"
        const val DOCUMENT_ID: String = "documentId"
        const val EVENT_TYPE: String = "eventType"
        const val COMMENTS: String = "comments"
        const val LAST_TASK_COMMENT: String = "lastTaskComment"
        const val VERSION: String = "version"
        const val CREATION_TIME: String = "creationTime"
        const val USERNAME: String = "username"
        const val USER_ID: String = "userId"
        const val TASK_TITLE: String = "taskTitle"
        const val TASK_ROLE: String = "taskRole"
        const val TASK_OUTCOME: String = "taskOutcome"
        const val TASK_OUTCOME_NAME: String = "taskOutcomeName"
        const val TASK_DEFINITION_KEY: String = "taskDefinitionKey"
        const val TASK_TYPE: String = "taskType"
        const val TASK_COMPLETED_ON_BEHALF_OF: String = "taskCompletedOnBehalfOf"
        const val FULL_TASK_TYPE: String = "fullTaskType"
        const val INITIATOR: String = "initiator"
        const val WORKFLOW_INSTANCE_ID: String = "workflowInstanceId"
        const val WORKFLOW_DESCRIPTION: String = "workflowDescription"
        const val TASK_EVENT_INSTANCE_ID: String = "taskEventInstanceId"
        const val DOCUMENT_VERSION: String = "documentVersion"
        const val PROPERTY_NAME: String = "propertyName"
        const val EXPECTED_PERFORM_TIME: String = "expectedPerformTime"
        const val TASK_DUE_DATE: String = "taskDueDate"
        const val TASK_ASSIGNEE_MANAGER: String = "assigneeManager"
        const val TASK_FORM_KEY: String = "taskFormKey"
        const val DOC_TYPE: String = "docType"
        const val DOC_STATUS_NAME: String = "docStatusName"
        const val DOC_STATUS_TITLE: String = "docStatusTitle"
        const val TASK_ACTORS: String = "taskActors"
        const val EMPTY_VALUE_KEY: String = "{empty}"
    }

    fun saveOrUpdateRecords(jsonRecords: String?): List<HistoryRecordEntity?>?

    fun saveOrUpdateRecord(
        historyRecord: HistoryRecordEntity?,
        requestParams: Map<String, String?>
    ): HistoryRecordEntity?

    fun saveOrUpdateRecord(historyRecordDto: HistoryRecordDto): HistoryRecordEntity?

    fun getHistoryRecordById(id: String?): HistoryRecordDto?

    fun getHistoryRecordByEventId(eventId: String?): HistoryRecordDto?

    fun getAll(
        maxItems: Int,
        skipCount: Int,
        predicate: Predicate,
        sort: List<SortBy>
    ): List<HistoryRecordDto>

    fun createHistoryDocumentMirror(documentMirrorRef: EntityRef, documentRef: EntityRef)

    fun getCount(): Long

    fun getCount(predicate: Predicate): Long
}
