package ru.citeck.ecos.history.api.records

import lombok.extern.slf4j.Slf4j
import mu.KotlinLogging
import org.apache.commons.lang.StringUtils
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
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDtoDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.records3.record.request.RequestContext
import java.time.Instant
import java.util.*

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
                ?.let { HistoryRecord(recordsService, it) }
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
        result.setRecords(historyRecordDtoList.map { HistoryRecord(recordsService, it) })
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
        val recordsService: RecordsService,
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

        fun getTaskOutcomeName(): String? {
            val i18nPrefix = "alfresco/i18n-value@"
            val outcome = dto.taskOutcome
            if (StringUtils.isBlank(outcome)){
                return null
            }
            val taskDefinitionKey = dto.taskDefinitionKey
            if (StringUtils.isNotBlank(taskDefinitionKey)) {
                val key = "flowable.form.button.$taskDefinitionKey.$outcome.label"
                val ref = RecordRef.valueOf(i18nPrefix + key)
                val title: String = recordsService.getAtt(ref, "?disp").asText()
                if (StringUtils.isNotBlank(title)) {
                    return title
                }
            }
            val taskType = dto.taskType
            if (StringUtils.isNotBlank(taskType)) {
                //todo: dynamic replacement of task type prefix
                val correctType: String = taskType!!.replace("\\{.*\\}".toRegex(), "ctrwf_")
                val key = "workflowtask.$correctType.outcome.$outcome"
                val ref = RecordRef.valueOf(i18nPrefix + key)
                val title: String = recordsService.getAtt(ref, "?disp").asText()
                if (StringUtils.isNotBlank(title)) {
                    return title
                }
            }
            val ref = RecordRef.valueOf(i18nPrefix + "workflowtask.outcome.$outcome")
            val title: String = recordsService.getAtt(ref, "?disp").asText()
            return if (StringUtils.isNotBlank(title)) title else outcome!!
        }

        fun getUserRef(): RecordRef {
            val userId = dto.username
            return RecordRef.create("emodel", "person", userId)
        }
    }

    data class EventType(val id: String) {

        private val BUNDLE_NAME = "i18n.document-history"
        private val bundleMap = hashMapOf<Locale, ResourceBundle>()

        fun getBundle(locale: Locale): ResourceBundle {
            if (bundleMap.containsKey(locale)) {
                return bundleMap.get(locale)!!
            }
            val bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale)
            bundleMap.put(locale, bundle)
            return bundle
        }

        fun getDisplayName(): String {
            try {
                val result = getBundle(RequestContext.getLocale()).getString(id)
                return result ?: id
            } catch (e: Exception) {
                return id
            }
        }
    }
}
