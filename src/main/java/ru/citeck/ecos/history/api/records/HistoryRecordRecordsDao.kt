package ru.citeck.ecos.history.api.records

import lombok.extern.slf4j.Slf4j
import mu.KotlinLogging
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.history.dto.HistoryRecordDto
import ru.citeck.ecos.history.service.HistoryRecordService
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.AttributePredicate
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.ValuePredicate
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDtoDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import java.time.Instant

@Slf4j
@Component
class HistoryRecordRecordsDao(
    private val historyRecordService: HistoryRecordService
) : AbstractRecordsDao(),
    RecordAttsDao,
    RecordsQueryDao,
    RecordMutateDtoDao<HistoryRecordDto> {

    companion object {
        const val ID = "history-record"
        private val log = KotlinLogging.logger {}

        private val ATTS_MAPPING = mapOf(
            "_created" to "creationTime",
            "_modified" to "creationTime"
        )
    }

    override fun getRecToMutate(recordId: String): HistoryRecordDto {
        val historyRecord = historyRecordService.getHistoryRecordByEventId(recordId)
        return historyRecord?.let { HistoryRecordDto(it) } ?: HistoryRecordDto()
    }

    override fun saveMutatedRec(record: HistoryRecordDto): String {
        val entity = historyRecordService.saveOrUpdateRecord(record)
        return entity.id.toString()
    }

    override fun getRecordAtts(recordId: String): HistoryRecord? {
        return if (recordId.isEmpty()) {
            null
        } else {
            return historyRecordService.getHistoryRecordByEventId(recordId)
                ?.let { HistoryRecord(it) }
        }
    }

    override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<HistoryRecord>? {

        if (recsQuery.language != PredicateService.LANGUAGE_PREDICATE) {
            log.warn("Unsupported query language '{}'", recsQuery.language)
            return null
        }
        val sort = getSpringDataSort(recsQuery.sortBy)
        val (maxItems, skipCount) = recsQuery.page

        val maxItemsCount = if (maxItems <= 0) {
            10000
        } else {
            maxItems
        }
        var predicate = recsQuery.getQuery(Predicate::class.java)
        predicate = PredicateUtils.mapAttributePredicates(
            predicate,
            { preProcessAttPredicate(it) },
            onlyAnd = true,
            optimize = true
        ) ?: VoidPredicate.INSTANCE

        val historyRecordDtoList = historyRecordService.getAll(maxItemsCount, skipCount, predicate, sort)

        val result = RecsQueryRes<HistoryRecord>()
        result.setRecords(historyRecordDtoList.map { HistoryRecord(it) })
        result.setTotalCount(historyRecordService.getCount(predicate))
        return result
    }

    private fun preProcessAttPredicate(predicate: AttributePredicate): Predicate? {
        if (predicate !is ValuePredicate) {
            return predicate
        }

        val attribute = predicate.getAttribute()
        if (attribute == "document") {
            val value = preProcessTxtValuePredicate(predicate.getValue()) {
                it.replaceFirst("alfresco/@workspace://SpacesStore/", "")
            }
            return if (value.isNull()) {
                null
            } else {
                val copy = predicate.copy<ValuePredicate>()
                copy.setVal(value)
                copy
            }
        }

        return predicate
    }

    private fun preProcessTxtValuePredicate(value: DataValue, action: (String) -> String?): DataValue {
        if (value.isTextual()) {
            return action.invoke(value.asText())?.let { DataValue.createStr(it) } ?: DataValue.NULL
        }
        if (value.isArray()) {
            val res = DataValue.createArr()
            for (arrValue in value) {
                val elem = preProcessTxtValuePredicate(arrValue, action)
                if (!elem.isNull()) {
                    res.add(elem)
                }
            }
            if (res.size() == 0) {
                return DataValue.NULL
            }
            return res
        }
        return DataValue.NULL
    }

    private fun getSpringDataSort(sort: List<SortBy>): Sort? {

        val orders = sort.filter {
            it.attribute.isNotBlank()
        }.map {
            val mappedAtt = ATTS_MAPPING.getOrDefault(it.attribute, it.attribute)
            if (it.ascending) {
                Sort.Order.asc(mappedAtt)
            } else {
                Sort.Order.desc(mappedAtt)
            }
        }
        return if (orders.isNotEmpty()) {
            Sort.by(orders)
        } else {
            null
        }
    }

    override fun getId() = ID

    class HistoryRecord(
        @AttName("...")
        val dto: HistoryRecordDto
    ) {
        fun getId(): String {
            return dto.historyEventId
        }

        fun getEventType(): EventType {
            val type = dto.eventType ?: ""
            return EventType(type)
        }

        fun getCreationTime(): Instant? {
            return dto.creationTime?.let { Instant.ofEpochMilli(it) }
        }

        fun getDocument(): RecordRef {
            val docId = dto.documentId ?: return RecordRef.EMPTY
            var ref = RecordRef.valueOf(docId)
            if (ref.appName.isBlank()) {
                ref = ref.withAppName("alfresco")
                    .withId("workspace://SpacesStore/${ref.id}")
            }
            return ref
        }
    }

    data class EventType(val id: String) {

        fun getDisplayName(): String {
            return when (id) {
                "node.created" -> "Created"
                "node.updated" -> "Updated"
                "task.create" -> "Task created"
                "task.assign" -> "Task assigned"
                "task.complete" -> "Task completed"
                "workflow.start" -> "Workflow started"
                "workflow.end" -> "Workflow ended"
                "workflow.end.cancelled" -> "Workflow cancelled"
                "assoc.added" -> "Added"
                "assoc.removed" -> "Removed"
                "assoc.contains" -> "Document"
                "assoc.updated" -> "Updated"
                "status.changed" -> "Status changed"
                else -> id
            }
        }
    }
}
