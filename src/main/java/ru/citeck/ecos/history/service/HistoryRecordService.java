package ru.citeck.ecos.history.service;

import ru.citeck.ecos.history.domain.HistoryRecordEntity;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

public interface HistoryRecordService {

    String HISTORY_EVENT_ID = "historyEventId";
    String DOCUMENT_ID = "documentId";
    String EVENT_TYPE = "eventType";
    String COMMENTS = "comments";
    String VERSION = "version";
    String CREATION_TIME = "creationTime";
    String USERNAME = "username";
    String USER_ID = "userId";
    String TASK_ROLE = "taskRole";
    String TASK_OUTCOME = "taskOutcome";
    String TASK_TYPE = "taskType";
    String FULL_TASK_TYPE = "fullTaskType";
    String INITIATOR = "initiator";
    String WORKFLOW_INSTANCE_ID = "workflowInstanceId";
    String WORKFLOW_DESCRIPTION = "workflowDescription";
    String TASK_EVENT_INSTANCE_ID = "taskEventInstanceId";
    String DOCUMENT_VERSION = "documentVersion";
    String PROPERTY_NAME = "propertyName";
    String EXPECTED_PERFORM_TIME = "expectedPerformTime";

    List<HistoryRecordEntity> saveOrUpdateRecords(String jsonRecords) throws IOException, ParseException;

    HistoryRecordEntity saveOrUpdateRecord(HistoryRecordEntity historyRecord, Map<String, String> requestParams) throws ParseException;
}
