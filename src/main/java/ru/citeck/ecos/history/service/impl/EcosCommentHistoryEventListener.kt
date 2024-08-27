package ru.citeck.ecos.history.service.impl

import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Component
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.history.domain.HistoryRecordEntity
import ru.citeck.ecos.history.service.HistoryEventType
import ru.citeck.ecos.history.service.HistoryRecordService
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Instant
import java.util.*

@Component
class EcosCommentHistoryEventListener(
    eventsService: EventsService,
    private val historyRecordService: HistoryRecordService
) {

    companion object {
        const val COMMENT_CREATE_EVENT = "comment-create"
        const val COMMENT_DELETE_EVENT = "comment-delete"
        const val COMMENT_UPDATE_EVENT = "comment-update"
    }

    init {
        eventsService.addListener {
            withEventType(COMMENT_CREATE_EVENT)
            withDataClass(CommentEvent::class.java)
            withAction {
                val record = getGeneralHistoryRecord(it, HistoryEventType.COMMENT_CREATED.value)
                historyRecordService.saveOrUpdateRecord(HistoryRecordEntity(), record)
            }
        }

        eventsService.addListener {
            withEventType(COMMENT_DELETE_EVENT)
            withDataClass(CommentEvent::class.java)
            withAction {
                val record = getGeneralHistoryRecord(it, HistoryEventType.COMMENT_DELETED.value)
                historyRecordService.saveOrUpdateRecord(HistoryRecordEntity(), record)
            }
        }

        eventsService.addListener<CommentUpdateEvent> {
            withEventType(COMMENT_UPDATE_EVENT)
            withDataClass(CommentUpdateEvent::class.java)
            withAction { event ->
                val record = hashMapOf<String, String>()

                record[HistoryRecordService.DOCUMENT_ID] = event.record.toString()
                record[HistoryRecordService.EVENT_TYPE] = HistoryEventType.COMMENT_UPDATED.value
                record[HistoryRecordService.USER_ID] = event.user
                record[HistoryRecordService.USERNAME] = event.user
                record[HistoryRecordService.CREATION_TIME] = formatTime(event.time)

                var textBefore = event.textBefore ?: ""
                if (StringUtils.isNotBlank(textBefore) && textBefore.length > 3000) {
                    textBefore = textBefore.substring(0, 2998) + "~"
                }
                val textAfter = event.textAfter ?: ""
                record[HistoryRecordService.COMMENTS] = "$textBefore -> $textAfter"

                historyRecordService.saveOrUpdateRecord(HistoryRecordEntity(), record)
            }
        }
    }

    private fun getGeneralHistoryRecord(event: CommentEvent, eventType: String): HashMap<String, String> {
        val record = hashMapOf<String, String>()
        record[HistoryRecordService.DOCUMENT_ID] = event.record.toString()
        record[HistoryRecordService.EVENT_TYPE] = eventType
        record[HistoryRecordService.USER_ID] = event.user
        record[HistoryRecordService.USERNAME] = event.user
        record[HistoryRecordService.CREATION_TIME] = formatTime(event.time)
        record[HistoryRecordService.COMMENTS] = event.text ?: ""
        return record
    }

    private fun formatTime(time: Instant): String {
        return HistoryRecordServiceImpl.dateFormat.format(Date.from(time))
    }

    data class CommentEvent(
        @AttName("record?id")
        val record: EntityRef,
        @AttName("\$event.time")
        val time: Instant,
        @AttName("\$event.user")
        val user: String,
        val commentRecord: EntityRef,
        val text: String? = null,
    )

    data class CommentUpdateEvent(
        @AttName("record?id")
        val record: EntityRef,
        @AttName("\$event.time")
        val time: Instant,
        @AttName("\$event.user")
        val user: String,
        val commentRecord: EntityRef,
        val textBefore: String? = null,
        val textAfter: String? = null
    )
}
