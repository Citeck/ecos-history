package ru.citeck.ecos.history.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.history.domain.HistoryRecordEntity
import ru.citeck.ecos.history.service.HistoryEventType
import ru.citeck.ecos.history.service.HistoryRecordService
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Instant
import java.util.*
import kotlin.collections.HashMap

const val PROC_DEF_EVENT_CREATE = "proc-def-create"
const val PROC_DEF_EVENT_UPDATE = "proc-def-update"
const val PROC_DEF_EVENT_DEPLOYED = "proc-def-deployed"

const val PROC_DEF_DATA_STATE_RAW = "RAW"

val PROC_DEF_VERSION_DEPLOYED_MSG = MLText(
    I18nContext.RUSSIAN to "Версия опубликована",
    I18nContext.ENGLISH to "Version is deployed"
)

private val procDefVersionCreateMessage = fun(event: ProcDefEvent): MLText {
    with(event) {
        var rus = "Описание процесса создано"
        var eng = "Process definition is created"

        if (dataState == PROC_DEF_DATA_STATE_RAW) {
            rus += " (черновик)"
            eng += " (draft)"
        }

        return MLText(
            I18nContext.RUSSIAN to rus,
            I18nContext.ENGLISH to eng
        )
    }
}

private val procDefVersionUpdateMessage = fun(event: ProcDefEvent): MLText {
    with(event) {
        var rus = "Версия обновлена"
        var eng = "Version is updated"

        if (createdFromVersion > 0) {
            rus += " $createdFromVersion -> $version"
            eng += " $createdFromVersion -> $version"
        } else if (version > 0) {
            rus += " -> $version"
            eng += " -> $version"
        }

        if (dataState == PROC_DEF_DATA_STATE_RAW) {
            rus += " (черновик)"
            eng += " (draft)"
        }

        return MLText(
            I18nContext.RUSSIAN to rus,
            I18nContext.ENGLISH to eng
        )
    }
}

@Component
class ProcDefHistoryEventsListener(
    eventsService: EventsService,
    private val historyRecordService: HistoryRecordService
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    init {
        eventsService.addListener<ProcDefEvent> {
            withEventType(PROC_DEF_EVENT_CREATE)
            withDataClass(ProcDefEvent::class.java)
            withAction { event ->

                log.debug { "History Proc Def Create Event: $event" }

                val record = getGeneralProcDefRecord(event, HistoryEventType.NODE_CREATED)
                record[HistoryRecordService.COMMENTS] = procDefVersionCreateMessage(event).toString()

                log.debug { "History Proc Def Create Event Record: $record" }

                historyRecordService.saveOrUpdateRecord(HistoryRecordEntity(), record)
            }
        }

        eventsService.addListener<ProcDefEvent> {
            withEventType(PROC_DEF_EVENT_UPDATE)
            withDataClass(ProcDefEvent::class.java)
            withAction { event ->

                log.debug { "History Proc Def Update Event: $event" }

                val record = getGeneralProcDefRecord(event, HistoryEventType.NODE_UPDATED)
                record[HistoryRecordService.COMMENTS] = procDefVersionUpdateMessage(event).toString()

                log.debug { "History Proc Def Update Event Record: $record" }

                historyRecordService.saveOrUpdateRecord(HistoryRecordEntity(), record)
            }
        }

        eventsService.addListener<ProcDefEvent> {
            withEventType(PROC_DEF_EVENT_DEPLOYED)
            withDataClass(ProcDefEvent::class.java)
            withAction { event ->

                log.debug { "History Proc Def Deployed Event: $event" }

                val record = getGeneralProcDefRecord(event, HistoryEventType.NODE_UPDATED)
                record[HistoryRecordService.COMMENTS] = PROC_DEF_VERSION_DEPLOYED_MSG.toString()

                log.debug { "History Proc Def Deployed Event Record: $record" }

                historyRecordService.saveOrUpdateRecord(HistoryRecordEntity(), record)
            }
        }
    }

    private fun getGeneralProcDefRecord(event: ProcDefEvent, eventType: HistoryEventType): HashMap<String, String> {
        val record = hashMapOf<String, String>()
        record[HistoryRecordService.EVENT_TYPE] = eventType.value
        record[HistoryRecordService.DOCUMENT_ID] = event.procDefRef.toString()
        record[HistoryRecordService.VERSION] = event.version.toString()

        record[HistoryRecordService.USER_ID] = event.user ?: ""
        record[HistoryRecordService.USERNAME] = event.user ?: ""
        record[HistoryRecordService.CREATION_TIME] = event.time?.let { formatTime(it) } ?: ""

        return record
    }

    private fun formatTime(time: Instant): String {
        return HistoryRecordServiceImpl.dateFormat.format(Date.from(time))
    }
}

data class ProcDefEvent(
    val procDefRef: EntityRef,
    val version: Double,
    val createdFromVersion: Double = 0.0,
    val dataState: String? = null,

    @AttName("\$event.time")
    var time: Instant? = null,

    @AttName("\$event.user")
    val user: String? = null
)
