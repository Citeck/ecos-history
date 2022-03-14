package ru.citeck.ecos.history.service.impl

import org.apache.commons.lang3.time.FastDateFormat
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
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

        private val EMPTY_VALUE_STR = "—"
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

                record[HistoryRecordService.DOCUMENT_ID] = event.record.toString()
                record[HistoryRecordService.EVENT_TYPE] = "node.updated"
                record[HistoryRecordService.USER_ID] = event.user
                record[HistoryRecordService.USERNAME] = event.user
                record[HistoryRecordService.CREATION_TIME] = formatTime(event.time)

                for (changed in event.changed) {
                    val comments = getComments(changed)
                    for (comment in comments) {
                        record[HistoryRecordService.COMMENTS] = comment
                        historyRecordService.saveOrUpdateRecord(HistoryRecordEntity(), record)
                    }
                }
            }
        }
    }

    private fun getComments(changed: ChangedValue): List<String> {

        val fieldName = changed.attName.getClosest(RU_LOCALE).ifBlank { changed.attId }

        if (changed.attType == AttributeType.JSON) {

            val isEmptyBefore = isEmpty(changed.before)
            val isEmptyAfter = isEmpty(changed.after)

            if (isEmptyBefore && isEmptyBefore != isEmptyAfter) {
                val msg = "$fieldName: " + if (isEmptyBefore) {
                    "Добавлен"
                } else {
                    "Удален"
                }
                return listOf(msg)
            } else if (isEmptyBefore && isEmptyAfter) {
                return emptyList()
            }

            val beforeStrArr = changed.before ?: emptyList()
            val afterStrArr = changed.after ?: emptyList()

            for (i in 0 until beforeStrArr.size.coerceAtLeast(afterStrArr.size)) {

                val beforeStrValue: String = beforeStrArr.getOrNull(i) ?: "{}"
                val afterStrValue: String = afterStrArr.getOrNull(i) ?: "{}"

                val valueBefore = convertArraysToObjects(DataValue.create(beforeStrValue))
                val valueAfter = convertArraysToObjects(DataValue.create(afterStrValue))

                val comments = arrayListOf<String>()
                collectJsonChangedEvents(fieldName, valueBefore, valueAfter, comments)
                return comments
            }
        }

        val valueToStr: (List<String>?, AttributeType) -> String = { value, type ->

            val notEmptyValues: List<String> = if (value == null || value.isEmpty()) {
                emptyList()
            } else {
                value.filter { it.isNotBlank() }
            }

            if (notEmptyValues.isEmpty()) {
                EMPTY_VALUE_STR
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

        return listOf(
            "$fieldName: " + valueToStr(changed.before, changed.attType) +
                " -> " +
                valueToStr(changed.after, changed.attType)
        )
    }

    private fun isEmpty(value: Any?): Boolean {
        return value == null
            || value is Collection<*> && (value.isEmpty() || value.all { isEmpty(it) })
            || value is Map<*, *> && value.isEmpty()
            || value is String && value.isEmpty()
            || value is DataValue && (
                value.isNull()
                || ((value.isArray() || value.isObject()) && value.size() == 0)
                || value.isTextual() && value.asText().isEmpty()
            )
    }

    private fun convertArraysToObjects(value: DataValue): DataValue {
        return if (value.isArray()) {
            val data = DataValue.createObj()
            for (element in value) {
                val id = element.get("id").asText().ifEmpty {
                    element.get("key").asText()
                }
                if (id.isNotBlank()) {
                    data.set(id, convertArraysToObjects(element))
                }
            }
            if (data.size() == 0) {
                value
            } else {
                data
            }
        } else if (value.isObject()) {
            val data = DataValue.createObj()
            value.forEach { k, v ->
                data.set(k, convertArraysToObjects(v))
            }
            data
        } else {
            value
        }
    }

    private fun collectJsonChangedEvents(
        path: String,
        before: DataValue,
        after: DataValue,
        events: MutableList<String>
    ) {
        if (after.isObject()) {
            val keys = before.fieldNamesList().toMutableSet()
            keys.addAll(after.fieldNamesList())
            val added = mutableListOf<DataValue>()
            val removed = mutableListOf<String>()
            for (key in keys) {
                if (!before.has(key)) {
                    added.add(after.get(key))
                    continue
                } else if (!after.has(key)) {
                    removed.add(key)
                    continue
                }
                collectJsonChangedEvents("$path.$key", before.get(key), after.get(key), events)
            }
            if (removed.isNotEmpty()) {
                events.add("$path удалено: $removed")
            }
            if (added.isNotEmpty()) {
                events.add("$path добавлено: $added")
            }
        } else if (before != after) {
            events.add("$path: $before -> $after")
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
