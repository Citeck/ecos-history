package ru.citeck.ecos.history.domain

import lombok.Data
import java.io.Serializable
import java.util.*
import javax.persistence.*

@Data
@Entity(name = HistoryRecordEntity.ENTITY_NAME)
@Table(name = "HISTORY_RECORDS")
@SequenceGenerator(name = "entity_id_gen", sequenceName = "HISTORY_RECORDS_SEQ", allocationSize = 1)
class HistoryRecordEntity : Serializable {

    companion object {

        private const val serialVersionUID = -3354822461532742388L

        const val ENTITY_NAME = "HistoryRecord"

        const val HISTORY_EVENT_ID = "historyEventId"
        const val DOCUMENT_ID = "documentId"
        const val EVENT_TYPE = "eventType"
        const val COMMENTS = "comments"
        const val VERSION = "version"
        const val CREATION_TIME = "creationTime"
        const val USERNAME = "username"
        const val USER_ID = "userId"
        const val TASK_TITLE = "taskTitle"
        const val TASK_ROLE = "taskRole"
        const val TASK_OUTCOME = "taskOutcome"
        const val TASK_OUTCOME_NAME = "taskOutcomeName"
        const val TASK_DEFINITION_KEY = "taskDefinitionKey"
        const val TASK_TYPE = "taskType"
        const val FULL_TASK_TYPE = "fullTaskType"
        const val INITIATOR = "initiator"
        const val WORKFLOW_INSTANCE_ID = "workflowInstanceId"
        const val WORKFLOW_DESCRIPTION = "workflowDescription"
        const val TASK_EVENT_INSTANCE_ID = "taskEventInstanceId"
        const val DOCUMENT_VERSION = "documentVersion"
        const val PROPERTY_NAME = "propertyName"
        const val EXPECTED_PERFORM_TIME = "expectedPerformTime"
        const val TASK_FORM_KEY = "taskFormKey"
        const val DOC_TYPE = "docType"
        const val DOC_STATUS_NAME = "docStatusName"
        const val DOC_STATUS_TITLE = "docStatusTitle"
        const val LAT_TASK_COMMENT = "lastTaskComment"

        const val DOCUMENT = "document"

        private var attributeNames: List<String> = listOf(
            HISTORY_EVENT_ID, DOCUMENT_ID, EVENT_TYPE, COMMENTS, VERSION,
            CREATION_TIME, USERNAME, USER_ID, TASK_TITLE, TASK_ROLE, TASK_TITLE, TASK_OUTCOME,
            TASK_DEFINITION_KEY, TASK_TYPE, TASK_FORM_KEY, TASK_EVENT_INSTANCE_ID, TASK_OUTCOME_NAME, FULL_TASK_TYPE,
            INITIATOR, WORKFLOW_INSTANCE_ID, WORKFLOW_DESCRIPTION, DOC_STATUS_NAME, DOC_STATUS_TITLE,
            DOC_TYPE, DOCUMENT_VERSION, LAT_TASK_COMMENT, PROPERTY_NAME, EXPECTED_PERFORM_TIME,
            ENTITY_NAME
        )

        @JvmStatic
        fun isAttributeNameValid(attributeName: String): Boolean {
            return attributeNames.contains(attributeName)
        }

        @JvmStatic
        fun replaceNameValid(attributeName: String): String {
            if (DOCUMENT.equals(attributeName)) {
                return DOCUMENT_ID;
            }
            return attributeName
        }
    }

    @Id
    @GeneratedValue(generator = "entity_id_gen", strategy = GenerationType.SEQUENCE)
    var id: Long? = null

    /**
     * External ID for events
     */
    @Basic(optional = false)
    lateinit var historyEventId: String

    @Basic(optional = false)
    var documentId: String? = null

    @Basic(optional = false)
    var eventType: String? = null

    @Column(length = 6000)
    var comments: String? = null

    @Basic
    var version: String? = null

    @Basic(optional = false)
    var creationTime: Date? = null

    @Basic(optional = false)
    var username: String? = null

    @Basic(optional = false)
    var userId: String? = null
    var taskTitle: String? = null
    var taskRole: String? = null
    var taskOutcome: String? = null
    var taskOutcomeName: String? = null
    var taskDefinitionKey: String? = null
    var taskType: String? = null
    //var taskActors: String? = null
    var fullTaskType: String? = null
    var initiator: String? = null
    var workflowInstanceId: String? = null
    var workflowDescription: String? = null
    var taskEventInstanceId: String? = null
    var documentVersion: String? = null
    var propertyName: String? = null
    var expectedPerformTime: Int? = null
    var taskFormKey: String? = null
    var docType: String? = null
    var docStatusName: String? = null
    var docStatusTitle: String? = null
    var lastTaskComment: String? = null
}
