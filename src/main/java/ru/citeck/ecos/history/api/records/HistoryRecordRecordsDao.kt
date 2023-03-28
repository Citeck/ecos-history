package ru.citeck.ecos.history.api.records

import com.netflix.discovery.EurekaClient
import lombok.extern.slf4j.Slf4j
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.history.dto.HistoryRecordDto
import ru.citeck.ecos.history.dto.TaskRole
import ru.citeck.ecos.history.service.HistoryRecordService
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.AttributePredicate
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.ValuePredicate
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
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
import kotlin.collections.ArrayList

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

        private const val APP_NAME_ALFRESCO = "alfresco"
        private const val I18N_ALF_SOURCE_ID = "$APP_NAME_ALFRESCO/i18n-value"
        private const val HISTORY_ALF_SOURCE_ID = "$APP_NAME_ALFRESCO/history"

        private const val ATT_CREATION_TIME = "creationTime"

        private val ATTS_MAPPING = mapOf(
            "_created" to "creationTime",
            "_modified" to "creationTime"
        )

        private val ALF_REF_PREFIXES = setOf(
            "alfresco/@workspace://SpacesStore/",
            "workspace://SpacesStore/"
        )
    }

    @Autowired(required = false)
    private var eurekaClient: EurekaClient? = null

    override fun getRecToMutate(recordId: String): HistoryRecordDto {
        val historyRecord = historyRecordService.getHistoryRecordByEventId(recordId)
        return historyRecord?.let { HistoryRecordDto(it) } ?: HistoryRecordDto()
    }

    override fun saveMutatedRec(record: HistoryRecordDto): String {
        val entity = historyRecordService.saveOrUpdateRecord(record)
        return entity.historyEventId
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

        val maxItemsCount = if (maxItems < 0) {
            1000
        } else {
            maxItems
        }
        val basePredicate = recsQuery.getQuery(Predicate::class.java)
        val predicate = PredicateUtils.mapAttributePredicates(
            basePredicate,
            { preProcessAttPredicate(it) },
            onlyAnd = true,
            optimize = true
        ) ?: VoidPredicate.INSTANCE

        var historyRecordDtoPage = historyRecordService.getAll(maxItemsCount, skipCount, predicate, sort)

        var historyRecordDtoList = historyRecordDtoPage?.historyRecordDtos
        if (historyRecordDtoList == null) {
            historyRecordDtoList = emptyList()
        }

        fillTaskOutcomeNames(historyRecordDtoList)

        val alfEvents = try {
            getEventsFromAlfresco(basePredicate)
        } catch (e: Exception) {
            log.error(e) { "Error while loading events from alfresco. Predicate: $predicate" }
            emptyList()
        }
        if (alfEvents.isNotEmpty()) {

            historyRecordDtoList = ArrayList(historyRecordDtoList)
            historyRecordDtoList.addAll(alfEvents)

            val creationOrder = sort.getOrderFor(ATT_CREATION_TIME)
            if (creationOrder != null) {
                if (creationOrder.isAscending) {
                    historyRecordDtoList.sortBy { it.creationTime ?: 0L }
                } else {
                    historyRecordDtoList.sortByDescending { it.creationTime ?: 0L }
                }
            }
        }

        val result = RecsQueryRes<HistoryRecord>()
        result.setRecords(historyRecordDtoList.map { HistoryRecord(recordsService, it) })
        var totalCount = historyRecordDtoPage?.totalElementsCount
        if (totalCount == null) {
            totalCount = 0L
        }
        result.setTotalCount(totalCount)
        return result
    }

    private fun fillTaskOutcomeNames(historyRecords: List<HistoryRecordDto>) {

        if (eurekaClient?.getApplication(APP_NAME_ALFRESCO) == null) {
            return
        }

        val outcomesToRequest = HashMap<OutcomeRequestData, MutableList<HistoryRecordDto>>()

        for (dto in historyRecords) {
            val outcome = dto.taskOutcome
            if (!outcome.isNullOrBlank() && (dto.taskOutcomeName.isNullOrBlank() || dto.taskOutcomeName == "{}")) {
                outcomesToRequest.computeIfAbsent(
                    OutcomeRequestData(
                        dto.taskType,
                        dto.taskDefinitionKey,
                        outcome
                    )
                ) { ArrayList() }.add(dto)
            }
            if (outcomesToRequest.isNotEmpty()) {
                val outcomesToReqList = outcomesToRequest.keys.toList()
                val outcomeLabels = try {
                    recordsService.query(
                        RecordsQuery.create {
                            withSourceId(I18N_ALF_SOURCE_ID)
                            withLanguage(TaskOutcomesLabelQuery.LANG)
                            withQuery(TaskOutcomesLabelQuery(outcomesToReqList))
                        },
                        listOf(ScalarType.DISP.schema)
                    ).getRecords().map { it.getAtt(ScalarType.DISP.schema).asText() }
                } catch (e: Exception) {
                    log.error(e) { "Exception while task outcome labels request: $outcomesToReqList" }
                    return
                }
                for ((idx, label) in outcomeLabels.withIndex()) {
                    if (label.isNotBlank()) {
                        outcomesToRequest[outcomesToReqList[idx]]?.forEach {
                            it.taskOutcomeName = label
                        }
                    }
                }
            }
        }
    }

    private fun getEventsFromAlfresco(predicate: Predicate): List<HistoryRecordDto> {

        if (eurekaClient?.getApplication(APP_NAME_ALFRESCO) == null) {
            return emptyList()
        }

        val predicateDto = PredicateUtils.convertToDto(predicate, PredicateDto::class.java, true)
        val document = predicateDto.document

        if (document == null || RecordRef.isEmpty(document) || ALF_REF_PREFIXES.none { document.id.startsWith(it) }) {
            return emptyList()
        }

        val records = recordsService.query(
            RecordsQuery.create {
                withSourceId(HISTORY_ALF_SOURCE_ID)
                withQuery(AlfHistoryQuery(true, document.id))
                withLanguage(AlfHistoryQuery.LANG)
            },
            AlfHistoryRecordAtts::class.java
        ).getRecords()

        val result: MutableList<HistoryRecordDto> = ArrayList()
        val allowedTypes = predicateDto.eventType ?: emptySet()
        for (record in records) {
            if (allowedTypes.isNotEmpty() && !allowedTypes.contains(record.eventType)) {
                continue
            }
            val historyRecDto = HistoryRecordDto()
            historyRecDto.comments = record.comments
            historyRecDto.version = record.version
            historyRecDto.username = record.userName
            historyRecDto.eventType = record.eventType
            historyRecDto.creationTime = record.creationTime?.toEpochMilli()
            historyRecDto.taskTitle = record.taskTitle
            historyRecDto.taskRole = record.taskRole
            historyRecDto.taskOutcomeName = record.taskOutcomeName
            historyRecDto.documentId = document.id
            result.add(historyRecDto)
        }

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

    private fun getSpringDataSort(sort: List<SortBy>): Sort {

        val orders = sort.filter {
            it.attribute.isNotBlank()
        }.map {
            val mappedAtt = ATTS_MAPPING.getOrDefault(it.attribute, it.attribute)
            if (it.ascending) {
                Sort.Order.asc(mappedAtt)
            } else {
                Sort.Order.desc(mappedAtt)
            }
        }.ifEmpty {
            listOf(Sort.Order.desc(ATT_CREATION_TIME))
        }
        return Sort.by(orders)
    }

    override fun getId() = ID

    class HistoryRecord(
        val recordsService: RecordsService,
        @AttName("...")
        val dto: HistoryRecordDto
    ) {
        fun getId(): String {
            return dto.historyEventId ?: ""
        }

        fun getEventType(): EventType {
            val type = dto.eventType ?: ""
            return EventType(type)
        }

        fun getComments(): MLText {
            return Json.mapper.read(dto.comments, MLText::class.java) ?: MLText()
        }

        fun getTaskTitle(): MLText {
            return Json.mapper.read(dto.taskTitle, MLText::class.java) ?: MLText()
        }

        fun getTaskRole(): List<MLText> {
            return dto.taskRole?.let { roleData ->
                if (roleData.startsWith("[")) {
                    return Json.mapper.readList(dto.taskRole, TaskRole::class.java).map {
                        it.name
                    }
                } else {
                    return listOf(MLText(roleData))
                }
            } ?: emptyList()
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

        fun getTaskOutcomeName(): Any? {
            val name = dto.taskOutcomeName
            if (name.isNullOrBlank()) {
                return dto.taskOutcome
            }
            if (name.startsWith("{")) {
                return Json.mapper.read(name, MLText::class.java) ?: dto.taskOutcome
            }
            return name
        }

        fun getUserRef(): RecordRef {
            val userId = dto.username?.lowercase()
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
            return try {
                val result = getBundle(RequestContext.getLocale()).getString(id)
                result ?: id
            } catch (e: Exception) {
                id
            }
        }
    }

    data class OutcomeRequestData(
        var taskType: String? = null,
        val taskDefinitionKey: String? = null,
        val outcome: String
    )

    data class TaskOutcomesLabelQuery(
        val outcomes: List<OutcomeRequestData>
    ) {
        companion object {
            const val LANG = "task-outcome-labels"
        }
    }

    data class PredicateDto(
        var document: RecordRef? = null,
        var eventType: Set<String>? = null
    )

    data class AlfHistoryQuery(
        val local: Boolean,
        val nodeRef: String
    ) {
        companion object {
            const val LANG = "document"
        }
    }

    data class AlfHistoryRecordAtts(
        @AttName("event:taskComment?disp")
        val comments: String? = null,
        @AttName("event:documentVersion?disp")
        val version: String? = null,
        @AttName("event:initiator.cm:userName")
        val userName: String? = null,
        @AttName("event:name?disp")
        val eventType: String? = null,
        @AttName("event:date?disp")
        val creationTime: Instant? = null,
        @AttName("event:taskTitle?disp")
        val taskTitle: String? = null,
        @AttName("event:taskRole?disp")
        val taskRole: String? = null,
        @AttName("event:taskOutcomeTitle?disp")
        val taskOutcomeName: String? = null
    )
}
