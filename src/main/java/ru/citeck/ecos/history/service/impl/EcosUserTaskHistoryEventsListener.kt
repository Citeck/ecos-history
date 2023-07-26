package ru.citeck.ecos.history.service.impl

import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.history.domain.HistoryRecordEntity
import ru.citeck.ecos.history.dto.TaskRole
import ru.citeck.ecos.history.service.HistoryEventType
import ru.citeck.ecos.history.service.HistoryRecordService
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import java.time.Instant
import java.util.*

const val BPMN_EVENT_USER_TASK_CREATE = "bpmn-user-task-create"
const val BPMN_EVENT_USER_TASK_COMPLETE = "bpmn-user-task-complete"
const val BPMN_EVENT_USER_TASK_ASSIGN = "bpmn-user-task-assign"

/**
 * @author Roman Makarskiy
 */
@Component
class EcosUserTaskHistoryEventsListener(
    eventsService: EventsService,
    private val historyRecordService: HistoryRecordService
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    init {
        eventsService.addListener<UserTaskEvent> {
            withEventType(BPMN_EVENT_USER_TASK_CREATE)
            withDataClass(UserTaskEvent::class.java)
            withAction { event ->

                log.debug { "History Task Create Event: $event" }

                val record = getGeneralHistoryRecord(event, HistoryEventType.TASK_CREATED.value)
                record[HistoryRecordService.USERNAME] = event.user ?: ""

                log.debug { "History Task Create Event Record: $record" }

                historyRecordService.saveOrUpdateRecord(HistoryRecordEntity(), record)
            }
        }

        eventsService.addListener<UserTaskEvent> {
            withEventType(BPMN_EVENT_USER_TASK_COMPLETE)
            withDataClass(UserTaskEvent::class.java)
            withAction { event ->

                log.debug { "History Task Complete Event: $event" }

                val record = getGeneralHistoryRecord(event, HistoryEventType.TASK_COMPLETE.value)
                record[HistoryRecordService.USERNAME] = event.user ?: ""
                record[HistoryRecordService.TASK_OUTCOME] = event.outcome ?: ""
                record[HistoryRecordService.TASK_OUTCOME_NAME] = event.outcomeName.toString()
                record[HistoryRecordService.TASK_ROLE] = Json.mapper.toString(event.roles) ?: ""
                record[HistoryRecordService.COMMENTS] = event.comment ?: ""
                event.completedOnBehalfOf?.let {
                    record[HistoryRecordService.TASK_COMPLETED_ON_BEHALF_OF] = it
                }

                log.debug { "History Task Complete Event Record: $record" }

                historyRecordService.saveOrUpdateRecord(HistoryRecordEntity(), record)
            }
        }

        eventsService.addListener<UserTaskEvent> {
            withEventType(BPMN_EVENT_USER_TASK_ASSIGN)
            withDataClass(UserTaskEvent::class.java)
            withAction { event ->

                log.debug { "History Task Assign Event: $event" }

                val record = getGeneralHistoryRecord(event, HistoryEventType.TASK_ASSIGN.value)
                record[HistoryRecordService.USERNAME] = event.assignee ?: ""

                log.debug { "History Task Assign Event Record: $record" }

                historyRecordService.saveOrUpdateRecord(HistoryRecordEntity(), record)
            }
        }
    }

    private fun getGeneralHistoryRecord(event: UserTaskEvent, eventType: String): HashMap<String, String> {
        val record = hashMapOf<String, String>()

        record[HistoryRecordService.DOCUMENT_ID] = event.document.toString()
        record[HistoryRecordService.VERSION] = event.version ?: ""
        record[HistoryRecordService.EVENT_TYPE] = eventType
        record[HistoryRecordService.WORKFLOW_INSTANCE_ID] = event.procInstanceId.toString()
        record[HistoryRecordService.TASK_TITLE] = event.name.toString()
        record[HistoryRecordService.TASK_EVENT_INSTANCE_ID] = event.taskId.toString()
        record[HistoryRecordService.TASK_FORM_KEY] = event.form?.toString() ?: ""

        record[HistoryRecordService.DOC_TYPE] = event.documentTypeRef?.toString() ?: ""

        record[HistoryRecordService.USER_ID] = event.user ?: ""
        record[HistoryRecordService.CREATION_TIME] = event.time?.let { formatTime(it) } ?: ""

        return record
    }

    private fun formatTime(time: Instant): String {
        return HistoryRecordServiceImpl.dateFormat.format(Date.from(time))
    }
}

data class UserTaskEvent(
    var taskId: RecordRef,
    var assignee: String? = null,
    var completedOnBehalfOf: String? = null,

    var procInstanceId: RecordRef,
    var form: RecordRef? = null,

    var name: MLText,
    var comment: String? = null,
    var outcome: String? = null,
    var outcomeName: MLText? = null,

    @AttName("document.version")
    var version: String? = null,

    var document: RecordRef,

    @AttName("document._type?id")
    var documentTypeRef: RecordRef? = null,

    var roles: List<TaskRole> = emptyList(),

    @AttName("\$event.time")
    var time: Instant? = null,

    @AttName("\$event.user")
    val user: String? = null
)
