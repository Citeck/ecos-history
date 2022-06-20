package ru.citeck.ecos.history.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.FastDateFormat;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.history.converter.HistoryRecordConverter;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.dto.HistoryRecordDto;
import ru.citeck.ecos.history.repository.HistoryRecordRepository;
import ru.citeck.ecos.history.service.HistoryRecordService;
import ru.citeck.ecos.history.service.TaskRecordService;
import org.apache.commons.lang3.StringUtils;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.predicate.model.AndPredicate;
import ru.citeck.ecos.records2.predicate.model.ComposedPredicate;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.predicate.model.ValuePredicate;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
    private final TaskRecordService taskRecordService;

    private final HistoryRecordConverter historyRecordConverter;

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
            result.setCreationTime(dateFormat.parse(dateString));
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

        result.setLastTaskComment(getValueOrEmpty(requestParams, LAST_TASK_COMMENT));
        result.setDocType(getValueOrEmpty(requestParams, DOC_TYPE));
        result.setDocStatusName(getValueOrEmpty(requestParams, DOC_STATUS_NAME));
        result.setDocStatusTitle(getValueOrEmpty(requestParams, DOC_STATUS_TITLE));

        if (log.isInfoEnabled()) {
            log.info(Json.getMapper().toString(historyRecordConverter.toDto(result)));
        }

        result = historyRecordRepository.save(result);

        taskRecordService.handleTaskFromHistoryRecord(result, requestParams);

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
    public List<HistoryRecordDto> getAll(int maxItems, int skipCount, Predicate predicate, Sort sort) {
        if (maxItems == 0) {
            return Collections.emptyList();
        }
        final PageRequest page = PageRequest.of(skipCount / maxItems, maxItems,
            sort != null ? sort : Sort.by(Sort.Direction.DESC, CREATION_TIME));
        Specification<HistoryRecordEntity> entitySpecification = specificationFromPredicate(predicate);
        return historyRecordRepository.findAll(entitySpecification, page)
            .stream()
            .map(historyRecordConverter::toDto)
            .collect(Collectors.toList());
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

    private Specification<HistoryRecordEntity> specificationFromPredicate(Predicate predicate) {
        if (predicate == null) {
            return null;
        }
        Specification<HistoryRecordEntity> result = null;
        if (predicate instanceof ComposedPredicate) {
            ComposedPredicate composedPredicate = (ComposedPredicate) predicate;
            ArrayList<Specification<HistoryRecordEntity>> specifications = new ArrayList<>();
            composedPredicate.getPredicates().forEach(subPredicate -> {
                Specification<HistoryRecordEntity> subSpecification = null;
                if (subPredicate instanceof ValuePredicate) {
                    subSpecification = fromValuePredicate((ValuePredicate) subPredicate);
                } else if (subPredicate instanceof ComposedPredicate) {
                    subSpecification = specificationFromPredicate(subPredicate);
                }
                if (subSpecification != null) {
                    specifications.add(subSpecification);
                }
            });

            if (!specifications.isEmpty()) {
                result = specifications.get(0);
                if (specifications.size() > 1) {
                    for (int idx = 1; idx < specifications.size(); idx++) {
                        result = (composedPredicate instanceof AndPredicate) ?
                            result.and(specifications.get(idx)) :
                            result.or(specifications.get(idx));
                    }
                }
            }
            return result;
        } else if (predicate instanceof ValuePredicate) {
            return fromValuePredicate((ValuePredicate) predicate);
        }
        log.warn("Unexpected predicate class: {}", predicate.getClass());
        return null;
    }

    @Override
    public long getCount() {
        return historyRecordRepository.count();
    }

    @Override
    public long getCount(Predicate predicate) {
        Specification<HistoryRecordEntity> specification = specificationFromPredicate(predicate);
        return specification != null ? historyRecordRepository.count(specification) : getCount();
    }

    private Specification<HistoryRecordEntity> fromValuePredicate(ValuePredicate valuePredicate) {
        //ValuePredicate.Type.IN was not implemented
        if (StringUtils.isBlank(valuePredicate.getAttribute())) {
            return null;
        }
        String attributeName = HistoryRecordEntity.replaceNameValid(StringUtils.trim(valuePredicate.getAttribute()));
        if (!HistoryRecordEntity.isAttributeNameValid(attributeName)) {
            return null;
        }

        Specification<HistoryRecordEntity> specification = null;
        if (ValuePredicate.Type.CONTAINS.equals(valuePredicate.getType())
            || ValuePredicate.Type.LIKE.equals(valuePredicate.getType())) {
            RecordRef recordRef = RecordRef.valueOf(valuePredicate.getValue().asText());
            String tmpValue = RecordRef.isEmpty(recordRef) ?
                valuePredicate.getValue().asText() :
                recordRef.getId();
            String attributeValue =
                ValuePredicate.Type.CONTAINS.equals(valuePredicate.getType()) ?
                    "%" + tmpValue.toLowerCase() + "%" :
                    tmpValue.toLowerCase();
            specification = (root, query, builder) ->
                builder.like(builder.lower(root.get(attributeName)), attributeValue);
        } else {
            Comparable objectValue = getObjectValue(attributeName, valuePredicate.getValue().asText());
            if (objectValue != null) {
                if (ValuePredicate.Type.EQ.equals(valuePredicate.getType())) {
                    specification = (root, query, builder) ->
                        builder.equal(root.get(attributeName), objectValue);
                } else if (ValuePredicate.Type.GT.equals(valuePredicate.getType())) {
                    specification = (root, query, builder) ->
                        builder.greaterThan(root.get(attributeName), objectValue);
                } else if (ValuePredicate.Type.GE.equals(valuePredicate.getType())) {
                    specification = (root, query, builder) ->
                        builder.greaterThanOrEqualTo(root.get(attributeName), objectValue);
                } else if (ValuePredicate.Type.LT.equals(valuePredicate.getType())) {
                    specification = (root, query, builder) ->
                        builder.lessThan(root.get(attributeName), objectValue);
                } else if (ValuePredicate.Type.LE.equals(valuePredicate.getType())) {
                    specification = (root, query, builder) ->
                        builder.lessThanOrEqualTo(root.get(attributeName), objectValue);
                } else if (ValuePredicate.Type.IN.equals(valuePredicate.getType())) {
                    specification = (root, query, builder) ->
                        builder.isTrue(root.get(attributeName).in(valuePredicate.getValue().asStrList()));
                }
            }
        }
        return specification;
    }

    private Comparable getObjectValue(String attributeName, String attributeValue) {
        try {
            Field searchField = HistoryRecordEntity.class.getDeclaredField(attributeName);
            if (searchField.getType() == String.class) {
                return attributeValue;
            } else if (searchField.getType() == Long.class) {
                return Long.valueOf(attributeValue);
            } else if (searchField.getType() == Date.class) {
                //saved values has no milliseconds part cause of dateFormat
                try {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(Long.valueOf(attributeValue));
                    calendar.set(Calendar.MILLISECOND, 0);
                    return calendar.getTime();
                } catch (NumberFormatException formatException) {
                    try {
                        Instant valueObject =
                            Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(attributeValue));
                        valueObject = valueObject.truncatedTo(ChronoUnit.SECONDS);
                        return Date.from(valueObject);
                    } catch (DateTimeException e) {
                        log.error("Failed to convert attribute '{}' value ({}) to date", attributeName,
                            attributeValue, e);
                    }
                }
            } else {
                log.error("Unexpected attribute type {} for predicate", searchField.getType());
            }
        } catch (NumberFormatException e) {
            log.error("Failed to convert attribute '{}' value ({}) to number", attributeName,
                attributeValue, e);
        } catch (NoSuchFieldException e) {
        }
        return null;
    }

    static class PredicateMap {

    }
}
