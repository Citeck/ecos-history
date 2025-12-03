package ru.citeck.ecos.history.api.records

import io.github.oshai.kotlinlogging.KotlinLogging
import lombok.extern.slf4j.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.history.common.HistorySystemArtifactPerms
import ru.citeck.ecos.history.dto.HistoryRecordDto
import ru.citeck.ecos.history.dto.TaskRole
import ru.citeck.ecos.history.service.HistoryRecordService
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
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.apps.EcosRemoteWebAppsApi
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.perms.RecordPerms
import java.time.Instant
import java.util.*

@Slf4j
@Component
class HistoryRecordRecordsDao(
    private val historyRecordService: HistoryRecordService,
    private val perms: HistorySystemArtifactPerms
) : AbstractRecordsDao(),
    RecordAttsDao,
    RecordsQueryDao,
    RecordMutateDtoDao<HistoryRecordDto> {

    companion object {
        const val ID = "history-record"
        private val log = KotlinLogging.logger {}

        const val USER_REF_ATT = "userRef"
        const val OWNER_ATT = "owner"
    }

    @Autowired
    private lateinit var remoteApps: EcosRemoteWebAppsApi

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
        val (maxItems, skipCount) = recsQuery.page

        val maxItemsCount = if (maxItems <= 0) {
            1000
        } else {
            maxItems
        }
        val basePredicate = recsQuery.getQuery(Predicate::class.java)

        val predicate = PredicateUtils.mapAttributePredicates(
            basePredicate,
            { preProcessAttPredicate(it) },
            onlyAnd = false,
            optimize = true
        ) ?: VoidPredicate.INSTANCE

        val historyRecordDtoList = historyRecordService.getAll(maxItemsCount, skipCount, predicate, recsQuery.sortBy)

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
        if (attribute == USER_REF_ATT || attribute == OWNER_ATT) {
            val value = preProcessTxtValuePredicate(predicate.getValue()) {
                EntityRef.valueOf(it).getLocalId()
            }
            return if (value.isNull()) {
                null
            } else {
                val copy = predicate.copy<ValuePredicate>()
                copy.setVal(value)
                val predicateType = predicate.getType()
                if (ValuePredicate.Type.CONTAINS == predicateType || ValuePredicate.Type.LIKE == predicateType) {
                    copy.setType(ValuePredicate.Type.EQ)
                }
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

    override fun getId() = ID

    inner class HistoryRecord(
        val recordsService: RecordsService,
        @AttName("...")
        val dto: HistoryRecordDto
    ) {
        fun getId(): String {
            return dto.historyEventId ?: ""
        }

        @AttName("?disp")
        fun getDispName(): String {
            return getId()
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

        fun getDocument(): EntityRef {
            val docId = dto.documentId ?: return EntityRef.EMPTY
            var ref = EntityRef.valueOf(docId)
            if (ref.getAppName().isBlank()) {
                ref = ref.withAppName("alfresco")
                    .withLocalId("workspace://SpacesStore/${ref.getLocalId()}")
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

        fun getUserRef(): EntityRef {
            val userId = dto.username?.lowercase()
            return EntityRef.create(AppName.EMODEL, "person", userId)
        }

        fun getOwner(): EntityRef {
            val userId = dto.taskCompletedOnBehalfOf?.lowercase() ?: ""
            if (userId.isEmpty()) {
                return EntityRef.EMPTY
            }
            return EntityRef.create(AppName.EMODEL, "person", userId)
        }

        fun getPermissions(): RecordPerms {
            return perms.getPerms(EntityRef.create(AppName.HISTORY, ID, getId()))
        }
    }

    data class EventType(val id: String) {

        companion object {
            const val BUNDLE_NAME = "i18n.document-history"
        }

        private val bundleMap = hashMapOf<Locale, ResourceBundle>()

        fun getBundle(locale: Locale): ResourceBundle {
            if (bundleMap.containsKey(locale)) {
                return bundleMap.get(locale)!!
            }
            val bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale)
            bundleMap[locale] = bundle
            return bundle
        }

        fun getDisplayName(): String {
            return try {
                val result = getBundle(I18nContext.getLocale()).getString(id)
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
        var document: EntityRef? = null,
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
