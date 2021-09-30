package ru.citeck.ecos.history.service.impl

import org.apache.commons.lang3.time.FastDateFormat
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.events2.type.RecordChangedEvent
import ru.citeck.ecos.events2.type.RecordCreatedEvent
import ru.citeck.ecos.events2.type.RecordStatusChangedEvent
import ru.citeck.ecos.history.domain.HistoryRecordEntity
import ru.citeck.ecos.history.service.HistoryRecordService
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import java.time.Instant
import java.time.ZoneId
import java.util.*
import javax.annotation.PostConstruct

@Component
class EcosEventsListener(
    private val eventsService: EventsService,
    private val historyRecordService: HistoryRecordService
) {

    companion object {
        private val RU_LOCALE = Locale("ru")

        private val DATE_FORMAT = FastDateFormat.getInstance(
            "dd.MM.yyyy",
            TimeZone.getTimeZone(ZoneId.of("UTC"))
        )
    }

    @PostConstruct
    fun init() {

        val buildStatusMsg = { status: StatusValue ->
            status.name.getClosest(RU_LOCALE).ifBlank { "—" }
        }

        eventsService.addListener<StatusChanged> {
            withEventType(RecordStatusChangedEvent.TYPE)
            withDataClass(StatusChanged::class.java)
            withAction { event ->

                val record = hashMapOf<String, String>()

                record[HistoryRecordService.DOCUMENT_ID] = event.record.toString()
                record[HistoryRecordService.DOC_STATUS_NAME] = event.after.id
                record[HistoryRecordService.DOC_STATUS_TITLE] = event.after.name.getClosest(RU_LOCALE)
                record[HistoryRecordService.EVENT_TYPE] = "status.changed"
                record[HistoryRecordService.USER_ID] = event.user
                record[HistoryRecordService.USERNAME] = event.user
                record[HistoryRecordService.CREATION_TIME] = formatTime(event.time)

                record[HistoryRecordService.COMMENTS] =
                    "${buildStatusMsg(event.before)} -> ${buildStatusMsg(event.after)}"

                historyRecordService.saveOrUpdateRecord(HistoryRecordEntity(), record)
            }
        }

        eventsService.addListener<RecordCreated> {
            withEventType(RecordCreatedEvent.TYPE)
            withDataClass(RecordCreated::class.java)
            withAction { event ->

                val record = hashMapOf<String, String>()

                record[HistoryRecordService.DOCUMENT_ID] = event.record.toString()
                record[HistoryRecordService.EVENT_TYPE] = "node.created"
                record[HistoryRecordService.USER_ID] = event.user
                record[HistoryRecordService.USERNAME] = event.user
                record[HistoryRecordService.CREATION_TIME] = formatTime(event.time)

                historyRecordService.saveOrUpdateRecord(HistoryRecordEntity(), record)
            }
        }

        eventsService.addListener<RecordUpdated> {
            withEventType(RecordChangedEvent.TYPE)
            withDataClass(RecordUpdated::class.java)
            withAction { event ->

                val record = hashMapOf<String, String>()

                val valueToStr: (List<String>?, AttributeType) -> String = { value, type ->

                    val notEmptyValues: List<String> = if (value == null || value.isEmpty()) {
                        emptyList()
                    } else {
                        value.filter { it.isNotBlank() }
                    }

                    if (notEmptyValues.isEmpty()) {
                        "—"
                    } else {
                        notEmptyValues.joinToString(", ") {
                            when (type) {
                                AttributeType.DATETIME -> formatTime(Instant.parse(it))
                                AttributeType.DATE -> DATE_FORMAT.format(Instant.parse(it))
                                else -> it
                            }
                        }
                    }
                }

                for (changed in event.changed) {

                    record[HistoryRecordService.DOCUMENT_ID] = event.record.toString()
                    record[HistoryRecordService.EVENT_TYPE] = "node.updated"
                    record[HistoryRecordService.USER_ID] = event.user
                    record[HistoryRecordService.USERNAME] = event.user
                    record[HistoryRecordService.CREATION_TIME] = formatTime(event.time)

                    record[HistoryRecordService.COMMENTS] =
                        "${changed.attName.getClosest(RU_LOCALE)}: " +
                            valueToStr(changed.before, changed.attType) +
                            " -> " +
                            valueToStr(changed.after, changed.attType)

                    historyRecordService.saveOrUpdateRecord(HistoryRecordEntity(), record)
                }
            }
        }
    }

    private fun formatTime(time: Instant): String {
        return HistoryRecordServiceImpl.dateFormat.format(Date.from(time))
    }

    data class RecordUpdated(
        @AttName("record?id")
        val record: RecordRef,
        @AttName("\$event.time")
        val time: Instant,
        @AttName("\$event.user")
        val user: String,
        @AttName("diff.list[]")
        val changed: List<ChangedValue>
    )

    data class ChangedValue(
        @AttName("before[]?disp")
        val before: List<String>?,
        @AttName("after[]?disp")
        val after: List<String>?,
        @AttName("def.id")
        val attId: String,
        @AttName("def.name")
        val attName: MLText,
        @AttName("def.type")
        val attType: AttributeType
    )

    data class RecordCreated(
        @AttName("record?id")
        val record: RecordRef,
        @AttName("\$event.time")
        val time: Instant,
        @AttName("\$event.user")
        val user: String
    )

    data class StatusChanged(
        @AttName("record?id")
        val record: RecordRef,
        val before: StatusValue,
        val after: StatusValue,
        @AttName("\$event.time")
        val time: Instant,
        @AttName("\$event.user")
        val user: String
    )

    data class StatusValue(
        val id: String,
        val name: MLText
    )
}
