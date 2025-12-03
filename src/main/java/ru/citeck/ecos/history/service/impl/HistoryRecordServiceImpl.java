package ru.citeck.ecos.history.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.context.lib.i18n.I18nContext;
import ru.citeck.ecos.data.sql.context.DbDataSourceContext;
import ru.citeck.ecos.data.sql.context.DbSchemaContext;
import ru.citeck.ecos.data.sql.domain.DbDomainFactory;
import ru.citeck.ecos.data.sql.records.refs.DbRecordRefService;
import ru.citeck.ecos.history.api.records.HistoryRecordRecordsDao;
import ru.citeck.ecos.history.converter.HistoryRecordConverter;
import ru.citeck.ecos.history.domain.HistoryDocumentMirrorEntity;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.dto.HistoryRecordDto;
import ru.citeck.ecos.history.repository.HistoryDocumentMirrorRepo;
import ru.citeck.ecos.history.repository.HistoryRecordRepository;
import ru.citeck.ecos.history.service.HistoryRecordService;
import ru.citeck.ecos.records2.predicate.PredicateUtils;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.predicate.model.ValuePredicate;
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy;
import ru.citeck.ecos.webapp.api.constants.AppName;
import ru.citeck.ecos.webapp.api.entity.EntityRef;
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaEntityFieldType;
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaSearchConverter;
import ru.citeck.ecos.webapp.lib.spring.hibernate.context.predicate.JpaSearchConverterFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service("historyRecordService")
public class HistoryRecordServiceImpl implements HistoryRecordService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final FastDateFormat dateFormat;

    static {
        dateFormat = FastDateFormat.getInstance("dd.MM.yyyy HH:mm:ss", TimeZone.getTimeZone(ZoneId.of("UTC")));
    }

    private final HistoryRecordRepository historyRecordRepository;
    private final HistoryDocumentMirrorRepo historyDocumentMirrorRepo;
    private final DbDomainFactory dbDomainFactory;
    private final JpaSearchConverterFactory jpaSearchConverterFactory;
    private DbRecordRefService dbRecordRefService;

    private final HistoryRecordConverter historyRecordConverter;
    private JpaSearchConverter<HistoryRecordEntity> searchConv;

    @SneakyThrows
    @PostConstruct
    void init() {

        // temp solution based on reflection to access DbRecordRefService.
        // In future versions this service will be accessible with public methods
        Field dataSourceContextField = DbDomainFactory.class.getDeclaredField("dataSourceContext");
        dataSourceContextField.setAccessible(true);
        DbDataSourceContext dataSourceCtx = (DbDataSourceContext) dataSourceContextField.get(dbDomainFactory);

        DbSchemaContext publicSchemaCtx = dataSourceCtx.getSchemaContext("public");
        dbRecordRefService = publicSchemaCtx.getRecordRefService();

        searchConv = jpaSearchConverterFactory.createConverter(HistoryRecordEntity.class)
            .withAttMapping(HistoryRecordRecordsDao.USER_REF_ATT, HistoryRecordEntity.USERNAME)
            .withAttMapping(HistoryRecordRecordsDao.OWNER_ATT, HistoryRecordEntity.TASK_COMPLETED_ON_BEHALF_OF)
            .withAttMapping(HistoryRecordEntity.DOCUMENT, HistoryRecordEntity.DOCUMENT_ID)
            .withFieldType(HistoryRecordEntity.TASK_ROLE, JpaEntityFieldType.MLTEXT)
            .withFieldType(HistoryRecordEntity.TASK_TITLE, JpaEntityFieldType.MLTEXT)
            .withFieldType(HistoryRecordEntity.TASK_OUTCOME_NAME, JpaEntityFieldType.MLTEXT)
            .withFieldVariants(HistoryRecordEntity.EVENT_TYPE, this::loadEventTypeVariants)
            .build();
    }

    private Map<String, MLText> loadEventTypeVariants() {
        Map<String, MLText> variants = new HashMap<>();
        ResourceBundle bundleEn = ResourceBundle.getBundle(HistoryRecordRecordsDao.EventType.BUNDLE_NAME, I18nContext.ENGLISH);
        ResourceBundle bundleRu = ResourceBundle.getBundle(HistoryRecordRecordsDao.EventType.BUNDLE_NAME, I18nContext.RUSSIAN);

        Set<String> keys = bundleEn.keySet();
        for (String key : keys) {
            MLText localizedVariant = new MLText()
                .withValue(I18nContext.ENGLISH, bundleEn.getString(key))
                .withValue(I18nContext.RUSSIAN, bundleRu.getString(key));

            variants.put(key, localizedVariant);
        }
        return variants;
    }

    @Transactional
    @Override
    public List<HistoryRecordEntity> saveOrUpdateRecords(String jsonRecords) throws IOException, ParseException {

        if (jsonRecords == null) {
            return null;
        }

        List<HistoryRecordEntity> result = new ArrayList<>();
        List<String> recordsList = OBJECT_MAPPER.readValue(jsonRecords, new TypeReference<ArrayList<String>>() {
        });

        for (String record : recordsList) {
            // TODO: arrays processing is break reading
            Map<String, String> resultMap = OBJECT_MAPPER.readValue(record, HashMap.class);

            String eventId = resultMap.get(HISTORY_EVENT_ID);
            HistoryRecordEntity recordEntity = historyRecordRepository.getHistoryRecordByHistoryEventId(eventId);
            if (recordEntity == null) {
                recordEntity = new HistoryRecordEntity();
            }
            result.add(saveOrUpdateRecord(recordEntity, resultMap));
        }
        return result;
    }

    @Transactional
    @Override
    public HistoryRecordEntity saveOrUpdateRecord(HistoryRecordEntity historyRecord,
                                                  Map<String, String> requestParams) throws ParseException {
        HistoryRecordEntity result = historyRecord != null ? historyRecord : new HistoryRecordEntity();

        log.debug("Request parameters:\n{}", requestParams);

        if (requestParams.containsKey(HISTORY_EVENT_ID)) {
            String historyEventId = requestParams.get(HISTORY_EVENT_ID);
            HistoryRecordEntity oldRecord = historyRecordRepository.getHistoryRecordByHistoryEventId(historyEventId);
            if (oldRecord != null) {
                result = oldRecord;
            }
            result.setHistoryEventId(requestParams.get(HISTORY_EVENT_ID));
        } else {
            result.setHistoryEventId(UUID.randomUUID().toString());
        }

        if (requestParams.containsKey(DOCUMENT_ID)) {
            result.setDocumentId(requestParams.get(DOCUMENT_ID));
        } else {
            return null;
        }

        if (requestParams.containsKey(EVENT_TYPE)) {
            result.setEventType(requestParams.get(EVENT_TYPE));
        }

        if (requestParams.containsKey(COMMENTS)) {
            String comment = requestParams.get(COMMENTS);
            if (StringUtils.isNotBlank(comment) && comment.length() > 6000) {
                log.warn("Event comment is too long ({}" +
                    ") and will be trimmed. Comment: {}", comment.length(), comment);
                comment = comment.substring(0, 5998) + "~";
            }
            result.setComments(comment);
        }

        if (requestParams.containsKey(VERSION)) {
            result.setVersion(requestParams.get(VERSION));
        }

        if (requestParams.containsKey(CREATION_TIME)) {
            String dateString = requestParams.get(CREATION_TIME);
            result.setCreationTime(dateFormat.parse(dateString).toInstant());
        }

        if (requestParams.containsKey(USERNAME)) {
            result.setUsername(requestParams.get(USERNAME));
        }

        if (requestParams.containsKey(USER_ID)) {
            result.setUserId(requestParams.get(USER_ID));
        }

        if (requestParams.containsKey(TASK_TITLE)) {
            result.setTaskTitle(requestParams.get(TASK_TITLE));
        }

        if (requestParams.containsKey(TASK_ROLE)) {
            result.setTaskRole(requestParams.get(TASK_ROLE));
        }

        if (requestParams.containsKey(TASK_OUTCOME)) {
            result.setTaskOutcome(requestParams.get(TASK_OUTCOME));
        }

        if (requestParams.containsKey(TASK_OUTCOME_NAME)) {
            result.setTaskOutcomeName(requestParams.get(TASK_OUTCOME_NAME));
        }

        if (requestParams.containsKey(TASK_DEFINITION_KEY)) {
            result.setTaskDefinitionKey(requestParams.get(TASK_DEFINITION_KEY));
        }

        if (requestParams.containsKey(TASK_TYPE)) {
            result.setTaskType(requestParams.get(TASK_TYPE));
        }

        if (requestParams.containsKey(FULL_TASK_TYPE)) {
            result.setFullTaskType(requestParams.get(FULL_TASK_TYPE));
        }

        if (requestParams.containsKey(INITIATOR)) {
            result.setInitiator(requestParams.get(INITIATOR));
        }

        if (requestParams.containsKey(WORKFLOW_INSTANCE_ID)) {
            result.setWorkflowInstanceId(requestParams.get(WORKFLOW_INSTANCE_ID));
        }

        if (requestParams.containsKey(WORKFLOW_DESCRIPTION)) {
            result.setWorkflowDescription(requestParams.get(WORKFLOW_DESCRIPTION));
        }

        if (requestParams.containsKey(TASK_EVENT_INSTANCE_ID)) {
            result.setTaskEventInstanceId(requestParams.get(TASK_EVENT_INSTANCE_ID));
        }

        if (requestParams.containsKey(DOCUMENT_VERSION)) {
            result.setDocumentVersion(requestParams.get(DOCUMENT_VERSION));
        }

        if (requestParams.containsKey(PROPERTY_NAME)) {
            result.setPropertyName(requestParams.get(PROPERTY_NAME));
        }

        if (requestParams.containsKey(EXPECTED_PERFORM_TIME)
            && StringUtils.isNotEmpty(requestParams.get(EXPECTED_PERFORM_TIME))) {
            result.setExpectedPerformTime(Integer.valueOf(requestParams.get(EXPECTED_PERFORM_TIME)));
        }

        if (requestParams.containsKey(TASK_FORM_KEY)) {
            String taskFormKey = requestParams.get(TASK_FORM_KEY);
            if (StringUtils.isNotBlank(taskFormKey)) {
                result.setTaskFormKey(taskFormKey);
            }
        }

        if (requestParams.containsKey(TASK_COMPLETED_ON_BEHALF_OF)) {
            String completedOnBehalfOf = requestParams.get(TASK_COMPLETED_ON_BEHALF_OF);
            if (StringUtils.isNotBlank(completedOnBehalfOf)) {
                result.setTaskCompletedOnBehalfOf(completedOnBehalfOf);
            }
        }

        result.setLastTaskComment(getValueOrEmpty(requestParams, LAST_TASK_COMMENT));
        result.setDocType(getValueOrEmpty(requestParams, DOC_TYPE));
        result.setDocStatusName(getValueOrEmpty(requestParams, DOC_STATUS_NAME));
        result.setDocStatusTitle(getValueOrEmpty(requestParams, DOC_STATUS_TITLE));

        if (log.isInfoEnabled()) {
            log.info(Json.getMapper().toString(historyRecordConverter.toDto(result)));
        }

        result = historyRecordRepository.save(result);

        return result;
    }

    @Nullable
    @Override
    public HistoryRecordDto getHistoryRecordById(String id) {
        if (StringUtils.isBlank(id)) {
            return null;
        }
        try {
            Long idValue = Long.valueOf(id);
            return historyRecordRepository.findById(idValue)
                .map(historyRecordConverter::toDto)
                .orElse(null);
        } catch (NumberFormatException e) {
            log.error("Failed to get history record by ID = {}", id, e);
            return null;
        }
    }

    @Nullable
    @Override
    public HistoryRecordDto getHistoryRecordByEventId(String eventId) {
        if (StringUtils.isBlank(eventId)) {
            return null;
        }
        HistoryRecordEntity entity = historyRecordRepository.getHistoryRecordByHistoryEventId(eventId);
        if (entity == null) {
            return null;
        }
        return historyRecordConverter.toDto(entity);
    }

    @Override
    public List<HistoryRecordDto> getAll(int maxItems, int skipCount, Predicate predicate, List<SortBy> sort) {
        Predicate convertedPredicate = PredicateUtils.mapValuePredicates(predicate, (pred) -> {
            String validName = HistoryRecordEntity.replaceNameValid(pred.getAttribute());
            if (HistoryRecordEntity.DOCUMENT_ID.equals(validName)) {
                Predicate newPred = expandDocumentMirrors(pred);
                if (!newPred.equals(pred)) {
                    return newPred;
                }
            }
            return pred;
        });

        return searchConv.findAll(historyRecordRepository, convertedPredicate, maxItems, skipCount, sort)
            .stream()
            .map(historyRecordConverter::toDto)
            .toList();
    }

    @Transactional
    @Override
    public void createHistoryDocumentMirror(EntityRef documentMirrorRef, EntityRef documentRef) {

        long documentMirrorRefId = dbRecordRefService.getOrCreateIdByEntityRef(documentMirrorRef);
        long documentRefId = dbRecordRefService.getOrCreateIdByEntityRef(documentRef);

        HistoryDocumentMirrorEntity entity = historyDocumentMirrorRepo.findByDocumentMirrorRefAndDocumentRef(
            documentMirrorRefId,
            documentRefId
        );
        if (entity != null) {
            return;
        }

        entity = new HistoryDocumentMirrorEntity();
        entity.setDocumentMirrorRef(documentMirrorRefId);
        entity.setDocumentRef(documentRefId);

        historyDocumentMirrorRepo.save(entity);
    }

    private Predicate expandDocumentMirrors(ValuePredicate predicate) {

        EntityRef docRef = EntityRef.valueOf(predicate.getValue().asText());
        if (docRef.isEmpty()) {
            return predicate;
        }
        docRef = docRef.withDefaultAppName(AppName.ALFRESCO);
        long docRefId = dbRecordRefService.getIdByEntityRefs(Collections.singletonList(docRef)).get(0);
        if (docRefId == -1) {
            return predicate;
        }
        List<HistoryDocumentMirrorEntity> mirrors = historyDocumentMirrorRepo.findAllByDocumentMirrorRef(docRefId);
        if (mirrors.isEmpty()) {
            return predicate;
        }
        List<Long> mirrorsIds = new ArrayList<>();
        for (HistoryDocumentMirrorEntity mirror : mirrors) {
            mirrorsIds.add(mirror.getDocumentRef());
        }
        List<EntityRef> entityRefsByIds = dbRecordRefService.getEntityRefsByIds(mirrorsIds);

        List<EntityRef> documentVariants = new ArrayList<>();
        documentVariants.add(docRef);
        documentVariants.addAll(entityRefsByIds);

        return new ValuePredicate(
            predicate.getAttribute(),
            ValuePredicate.Type.IN,
            documentVariants.stream().map(ref -> {
                if (AppName.ALFRESCO.equals(ref.getAppName())) {
                    return ref.getLocalId().replace("workspace://SpacesStore/", "");
                } else {
                    return ref.toString();
                }
            }).collect(Collectors.toList())
        );
    }

    @Transactional
    @Override
    public HistoryRecordEntity saveOrUpdateRecord(HistoryRecordDto historyRecordDto) throws ParseException {
        Map<String, String> propertyMap = historyRecordConverter.toMap(historyRecordDto);
        String timeValue = propertyMap.get(HistoryRecordEntity.CREATION_TIME);
        if (timeValue != null) {
            propertyMap.put(HistoryRecordEntity.CREATION_TIME, dateFormat.format(Long.valueOf(timeValue)));
        }
        return saveOrUpdateRecord(new HistoryRecordEntity(), propertyMap);
    }

    private String getValueOrEmpty(Map<String, String> requestParams, String valueKey) {
        if (!requestParams.containsKey(valueKey)) {
            return null;
        }

        String value = requestParams.get(valueKey);
        return StringUtils.isNotBlank(value) ? value : "";
    }

    @Override
    public long getCount() {
        return historyRecordRepository.count();
    }

    @Override
    public long getCount(Predicate predicate) {
        return searchConv.getCount(historyRecordRepository, predicate);
    }
}
