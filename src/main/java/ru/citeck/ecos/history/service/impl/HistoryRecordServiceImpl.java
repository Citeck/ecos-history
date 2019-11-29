package ru.citeck.ecos.history.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.repository.HistoryRecordRepository;
import ru.citeck.ecos.history.service.HistoryRecordService;
import ru.citeck.ecos.history.service.TaskRecordService;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.*;

@Service("historyRecordService")
public class HistoryRecordServiceImpl implements HistoryRecordService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final SimpleDateFormat dateFormat;

    static {
        dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone(ZoneId.of("UTC")));
    }

    private HistoryRecordRepository historyRecordRepository;
    private TaskRecordService taskRecordService;

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

    @Override
    public HistoryRecordEntity saveOrUpdateRecord(HistoryRecordEntity historyRecord, Map<String, String> requestParams) throws ParseException {
        HistoryRecordEntity result = historyRecord != null ? historyRecord : new HistoryRecordEntity();

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
            result.setComments(requestParams.get(COMMENTS));
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

        if (requestParams.containsKey(TASK_ROLE)) {
            result.setTaskRole(requestParams.get(TASK_ROLE));
        }

        if (requestParams.containsKey(TASK_OUTCOME)) {
            result.setTaskOutcome(requestParams.get(TASK_OUTCOME));
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

        if (requestParams.containsKey(EXPECTED_PERFORM_TIME) && StringUtils.isNotEmpty(requestParams.get(EXPECTED_PERFORM_TIME))) {
            result.setExpectedPerformTime(Integer.valueOf(requestParams.get(EXPECTED_PERFORM_TIME)));
        }

        if (requestParams.containsKey(TASK_FORM_KEY)) {
            String taskFormKey = requestParams.get(TASK_FORM_KEY);
            if (StringUtils.isNotBlank(taskFormKey)) {
                result.setTaskFormKey(requestParams.get(TASK_FORM_KEY));
            }
        }

        historyRecordRepository.save(result);

        taskRecordService.handleTaskFromHistoryRecord(result, requestParams);

        return result;
    }

    @Autowired
    public void setHistoryRecordRepository(HistoryRecordRepository historyRecordRepository) {
        this.historyRecordRepository = historyRecordRepository;
    }

    @Autowired
    public void setTaskRecordService(TaskRecordService taskRecordService) {
        this.taskRecordService = taskRecordService;
    }
}
