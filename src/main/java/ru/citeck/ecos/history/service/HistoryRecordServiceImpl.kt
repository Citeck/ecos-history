package ru.citeck.ecos.history.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import lombok.extern.slf4j.Slf4j
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.time.FastDateFormat
import org.jetbrains.annotations.Nullable
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.auth.AuthRole
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.data.sql.records.refs.DbRecordRefService
import ru.citeck.ecos.history.api.records.HistoryRecordRecordsDao
import ru.citeck.ecos.history.converter.HistoryRecordConverter
import ru.citeck.ecos.history.domain.HistoryDocumentMirrorEntity
import ru.citeck.ecos.history.domain.HistoryRecordEntity
import ru.citeck.ecos.history.dto.HistoryRecordDto
import ru.citeck.ecos.history.repository.HistoryDocumentMirrorRepo
import ru.citeck.ecos.history.repository.HistoryRecordRepository
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.predicate.model.ValuePredicate
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaEntityFieldType
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaSearchConverter
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaSearchConverterFactory
import java.time.ZoneId
import java.util.ArrayList
import java.util.HashMap
import java.util.ResourceBundle
import java.util.TimeZone
import java.util.UUID

@Slf4j
@Service("historyRecordService")
class HistoryRecordServiceImpl(
    private val historyRecordRepository: HistoryRecordRepository,
    private val historyDocumentMirrorRepo: HistoryDocumentMirrorRepo,
    private val jpaSearchConverterFactory: JpaSearchConverterFactory,
    private val dbRecordRefService: DbRecordRefService,
    private val historyRecordConverter: HistoryRecordConverter
) : HistoryRecordService {

    companion object {
        const val WORKSPACE_SPACES_STORE = "workspace://SpacesStore/"
        private val DOCUMENT_ATT_VARIANTS = setOf("document", "documentId")

        private val objectMapper = ObjectMapper()

        val dateFormat: FastDateFormat = FastDateFormat.getInstance(
            "dd.MM.yyyy HH:mm:ss",
            TimeZone.getTimeZone(ZoneId.of("UTC"))
        )

        private val log = KotlinLogging.logger {}

        @JvmStatic
        fun normalizeDocRef(refStr: String?): EntityRef {
            if (refStr.isNullOrBlank()) {
                return EntityRef.EMPTY
            }
            var ref = EntityRef.valueOf(refStr)
            if (ref.getAppName().isEmpty()) {
                if (!ref.getLocalId().startsWith(WORKSPACE_SPACES_STORE)) {
                    ref = ref.withLocalId(WORKSPACE_SPACES_STORE + ref.getLocalId())
                }
                ref = ref.withAppName(AppName.ALFRESCO)
            }
            return ref
        }
    }

    private lateinit var searchConv: JpaSearchConverter<HistoryRecordEntity>

    @PostConstruct
    fun init() {

        searchConv = jpaSearchConverterFactory.createConverter(HistoryRecordEntity::class.java)
            .withAttMapping(HistoryRecordRecordsDao.Companion.USER_REF_ATT, HistoryRecordEntity.Companion.USERNAME)
            .withAttMapping(HistoryRecordRecordsDao.Companion.OWNER_ATT, HistoryRecordEntity.Companion.TASK_COMPLETED_ON_BEHALF_OF)
            .withAttMapping("document", "documentRefId")
            .withAttMapping("documentId", "documentRefId")
            .withFieldType(HistoryRecordEntity.Companion.TASK_ROLE, JpaEntityFieldType.MLTEXT)
            .withFieldType(HistoryRecordEntity.Companion.TASK_TITLE, JpaEntityFieldType.MLTEXT)
            .withFieldType(HistoryRecordEntity.Companion.TASK_OUTCOME_NAME, JpaEntityFieldType.MLTEXT)
            .withFieldVariants(HistoryRecordEntity.Companion.EVENT_TYPE, this::loadEventTypeVariants)
            .build()
    }

    private fun loadEventTypeVariants(): Map<String, MLText> {
        val variants: MutableMap<String, MLText> = HashMap()
        val bundleEn: ResourceBundle = ResourceBundle.getBundle(
            HistoryRecordRecordsDao.EventType.BUNDLE_NAME,
            I18nContext.ENGLISH
        )
        val bundleRu: ResourceBundle = ResourceBundle.getBundle(
            HistoryRecordRecordsDao.EventType.BUNDLE_NAME,
            I18nContext.RUSSIAN
        )

        val keys: Set<String> = bundleEn.keySet()
        for (key in keys) {
            val localizedVariant: MLText = MLText()
                .withValue(I18nContext.ENGLISH, bundleEn.getString(key))
                .withValue(I18nContext.RUSSIAN, bundleRu.getString(key))

            variants[key] = localizedVariant
        }
        return variants
    }

    override fun getAll(
        maxItems: Int,
        skipCount: Int,
        predicate: Predicate,
        sort: List<SortBy>
    ): List<HistoryRecordDto> {

        return searchConv.findAll(
            repo = historyRecordRepository,
            predicate = preparePredicateToDbQuery(predicate),
            max = maxItems,
            skip = skipCount,
            sort = sort
        ).map(historyRecordConverter::toDto)
    }

    @Transactional
    @Secured(AuthRole.SYSTEM, AuthRole.ADMIN)
    override fun saveOrUpdateRecords(jsonRecords: String?): List<HistoryRecordEntity?>? {

        if (jsonRecords == null) {
            return null
        }

        val result: MutableList<HistoryRecordEntity?> = ArrayList()
        val recordsList: List<String> = objectMapper.readValue(
            jsonRecords,
            object : TypeReference<ArrayList<String>>() {}
        )

        for (record in recordsList) {
            // TODO: arrays processing is break reading
            val mapType = objectMapper.typeFactory.constructMapType(
                HashMap::class.java,
                String::class.java,
                String::class.java
            )
            val resultMap: Map<String, String?> = objectMapper.readValue(record, mapType)

            val eventId: String? = resultMap[HistoryRecordEntity.Companion.HISTORY_EVENT_ID]
            var recordEntity: HistoryRecordEntity? = historyRecordRepository.getHistoryRecordByHistoryEventId(eventId)
            if (recordEntity == null) {
                recordEntity = HistoryRecordEntity()
            }
            result.add(saveOrUpdateRecord(recordEntity, resultMap))
        }
        return result
    }

    @Transactional
    @Secured(AuthRole.SYSTEM, AuthRole.ADMIN)
    override fun saveOrUpdateRecord(
        historyRecord: HistoryRecordEntity?,
        requestParams: Map<String, String?>
    ): HistoryRecordEntity? {

        var result: HistoryRecordEntity = historyRecord ?: HistoryRecordEntity()

        log.debug { "Request parameters: $requestParams" }

        if (requestParams.containsKey(HistoryRecordService.HISTORY_EVENT_ID)) {
            val historyEventId: String? = requestParams[HistoryRecordService.HISTORY_EVENT_ID]
            var oldRecord: HistoryRecordEntity? = null
            if (historyEventId != null) {
                oldRecord = historyRecordRepository.getHistoryRecordByHistoryEventId(historyEventId)
            }
            if (oldRecord != null) {
                result = oldRecord
            }
            if (historyEventId != null) {
                result.historyEventId = historyEventId
            }
        } else {
            result.historyEventId = UUID.randomUUID().toString()
        }

        if (requestParams.containsKey(HistoryRecordService.DOCUMENT_ID)) {
            val docRef = normalizeDocRef(requestParams[HistoryRecordService.DOCUMENT_ID])
            if (docRef.isEmpty()) {
                return null
            }
            result.documentRefId = dbRecordRefService.getOrCreateIdByEntityRef(docRef)
        } else {
            return null
        }

        if (requestParams.containsKey(HistoryRecordService.EVENT_TYPE)) {
            result.eventType = requestParams[HistoryRecordService.EVENT_TYPE]
        }

        if (requestParams.containsKey(HistoryRecordService.COMMENTS)) {
            var comment: String? = requestParams[HistoryRecordService.COMMENTS]
            if (!comment.isNullOrBlank() && comment.length > 6000) {
                log.warn {
                    "Event comment is too long (${comment?.length}" +
                        ") and will be trimmed. Comment: $comment"
                }
                comment = comment.take(5998) + "~"
            }
            result.comments = comment
        }

        if (requestParams.containsKey(HistoryRecordService.VERSION)) {
            result.version = requestParams[HistoryRecordService.VERSION]
        }

        if (requestParams.containsKey(HistoryRecordService.CREATION_TIME)) {
            val dateString: String? = requestParams[HistoryRecordService.CREATION_TIME]
            result.creationTime = dateFormat.parse(dateString).toInstant()
        }

        fun getIfReqContainsKey(key: String, action: (String?) -> Unit) {
            if (requestParams.containsKey(key)) {
                action(requestParams[key])
            }
        }

        getIfReqContainsKey(HistoryRecordService.USERNAME) { result.username = it }
        getIfReqContainsKey(HistoryRecordService.USER_ID) { result.userId = it }
        getIfReqContainsKey(HistoryRecordService.TASK_TITLE) { result.taskTitle = it }
        getIfReqContainsKey(HistoryRecordService.TASK_ROLE) { result.taskRole = it }
        getIfReqContainsKey(HistoryRecordService.TASK_OUTCOME) { result.taskOutcome = it }
        getIfReqContainsKey(HistoryRecordService.TASK_OUTCOME_NAME) { result.taskOutcomeName = it }
        getIfReqContainsKey(HistoryRecordService.TASK_DEFINITION_KEY) { result.taskDefinitionKey = it }
        getIfReqContainsKey(HistoryRecordService.TASK_TYPE) { result.taskType = it }
        getIfReqContainsKey(HistoryRecordService.FULL_TASK_TYPE) { result.fullTaskType = it }
        getIfReqContainsKey(HistoryRecordService.INITIATOR) { result.initiator = it }
        getIfReqContainsKey(HistoryRecordService.WORKFLOW_INSTANCE_ID) { result.workflowInstanceId = it }
        getIfReqContainsKey(HistoryRecordService.WORKFLOW_DESCRIPTION) { result.workflowDescription = it }
        getIfReqContainsKey(HistoryRecordService.TASK_EVENT_INSTANCE_ID) { result.taskEventInstanceId = it }
        getIfReqContainsKey(HistoryRecordService.DOCUMENT_VERSION) { result.documentVersion = it }
        getIfReqContainsKey(HistoryRecordService.PROPERTY_NAME) { result.propertyName = it }

        if (requestParams.containsKey(HistoryRecordService.EXPECTED_PERFORM_TIME) &&
            StringUtils.isNotEmpty(requestParams[HistoryRecordService.EXPECTED_PERFORM_TIME])
        ) {
            result.expectedPerformTime = Integer.valueOf(requestParams[HistoryRecordService.EXPECTED_PERFORM_TIME])
        }

        if (requestParams.containsKey(HistoryRecordService.TASK_FORM_KEY)) {
            val taskFormKey: String? = requestParams[HistoryRecordService.TASK_FORM_KEY]
            if (StringUtils.isNotBlank(taskFormKey)) {
                result.taskFormKey = taskFormKey
            }
        }

        if (requestParams.containsKey(HistoryRecordService.TASK_COMPLETED_ON_BEHALF_OF)) {
            val completedOnBehalfOf: String? = requestParams[HistoryRecordService.TASK_COMPLETED_ON_BEHALF_OF]
            if (StringUtils.isNotBlank(completedOnBehalfOf)) {
                result.taskCompletedOnBehalfOf = completedOnBehalfOf
            }
        }

        result.lastTaskComment = getValueOrEmpty(requestParams, HistoryRecordService.LAST_TASK_COMMENT)
        result.docType = getValueOrEmpty(requestParams, HistoryRecordService.DOC_TYPE)
        result.docStatusName = getValueOrEmpty(requestParams, HistoryRecordService.DOC_STATUS_NAME)
        result.docStatusTitle = getValueOrEmpty(requestParams, HistoryRecordService.DOC_STATUS_TITLE)

        if (log.isInfoEnabled()) {
            log.info { Json.mapper.toString(historyRecordConverter.toDto(result)) }
        }

        result = historyRecordRepository.save(result)

        return result
    }

    override fun getHistoryRecordById(id: String?): HistoryRecordDto? {
        if (id.isNullOrBlank()) {
            return null
        }
        try {
            val idValue = id.toLong()
            return historyRecordRepository.findById(idValue)
                .map(historyRecordConverter::toDto)
                .orElse(null)
        } catch (e: NumberFormatException) {
            log.error(e) { "Failed to get history record by ID = $id" }
            return null
        }
    }

    @Nullable
    override fun getHistoryRecordByEventId(eventId: String?): HistoryRecordDto? {
        if (StringUtils.isBlank(eventId)) {
            return null
        }
        val entity = historyRecordRepository.getHistoryRecordByHistoryEventId(eventId) ?: return null
        return historyRecordConverter.toDto(entity)
    }

    @Transactional
    @Secured(AuthRole.SYSTEM, AuthRole.ADMIN)
    override fun createHistoryDocumentMirror(documentMirrorRef: EntityRef, documentRef: EntityRef) {

        val documentMirrorRefId = dbRecordRefService.getOrCreateIdByEntityRef(documentMirrorRef)
        val documentRefId = dbRecordRefService.getOrCreateIdByEntityRef(documentRef)

        var entity = historyDocumentMirrorRepo.findByDocumentMirrorRefAndDocumentRef(
            documentMirrorRefId,
            documentRefId
        )
        if (entity != null) {
            return
        }

        entity = HistoryDocumentMirrorEntity()
        entity.documentMirrorRef = documentMirrorRefId
        entity.documentRef = documentRefId

        historyDocumentMirrorRepo.save(entity)
    }

    @Transactional
    @Secured(AuthRole.SYSTEM, AuthRole.ADMIN)
    override fun saveOrUpdateRecord(historyRecordDto: HistoryRecordDto): HistoryRecordEntity {

        val propertyMap: MutableMap<String, String> = historyRecordConverter.toMap(historyRecordDto)
        val timeValue = propertyMap[HistoryRecordEntity.Companion.CREATION_TIME]
        if (timeValue != null) {
            propertyMap[HistoryRecordEntity.Companion.CREATION_TIME] = dateFormat.format(timeValue.toLong())
        }
        return saveOrUpdateRecord(HistoryRecordEntity(), propertyMap)!!
    }

    private fun getValueOrEmpty(requestParams: Map<String, String?>, valueKey: String): String? {
        if (!requestParams.containsKey(valueKey)) {
            return null
        }
        val value = requestParams[valueKey]
        return if (value.isNullOrBlank()) "" else value
    }

    override fun getCount(): Long {
        return historyRecordRepository.count()
    }

    override fun getCount(predicate: Predicate): Long {
        return searchConv.getCount(
            historyRecordRepository,
            preparePredicateToDbQuery(predicate)
        )
    }

    private fun preparePredicateToDbQuery(predicate: Predicate): Predicate {
        return PredicateUtils.mapValuePredicates(predicate) { pred ->
            if (DOCUMENT_ATT_VARIANTS.contains(pred.getAttribute())) {
                prepareDocRefsToSearch(pred)
            } else {
                pred
            }
        } ?: Predicates.alwaysTrue()
    }

    private fun prepareDocRefsToSearch(predicate: ValuePredicate): Predicate {

        val docRef = normalizeDocRef(predicate.getValue().asText())
        if (docRef.isEmpty()) {
            return predicate
        }
        val docRefId = dbRecordRefService.getIdByEntityRefs(listOf(docRef)).first()
        if (docRefId == -1L) {
            return Predicates.alwaysFalse()
        }
        val mirrors = historyDocumentMirrorRepo.findAllByDocumentMirrorRef(docRefId)

        val documentVariants = ArrayList<Long>()
        documentVariants.add(docRefId)
        mirrors.forEach { documentVariants.add(it.documentRef) }

        return ValuePredicate(
            attribute = HistoryRecordEntity.Companion.DOCUMENT_REF_ID,
            type = ValuePredicate.Type.IN,
            value = documentVariants
        )
    }
}
