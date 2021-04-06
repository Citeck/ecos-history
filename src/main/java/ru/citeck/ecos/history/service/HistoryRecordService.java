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
    String LAST_TASK_COMMENT = "lastTaskComment";
    String VERSION = "version";
    String CREATION_TIME = "creationTime";
    String USERNAME = "username";
    String USER_ID = "userId";
    String TASK_TITLE = "taskTitle";
    String TASK_ROLE = "taskRole";
    String TASK_OUTCOME = "taskOutcome";
    String TASK_DEFINITION_KEY = "taskDefinitionKey";
    String TASK_TYPE = "taskType";
    String FULL_TASK_TYPE = "fullTaskType";
    String INITIATOR = "initiator";
    String WORKFLOW_INSTANCE_ID = "workflowInstanceId";
    String WORKFLOW_DESCRIPTION = "workflowDescription";
    String TASK_EVENT_INSTANCE_ID = "taskEventInstanceId";
    String DOCUMENT_VERSION = "documentVersion";
    String PROPERTY_NAME = "propertyName";
    String EXPECTED_PERFORM_TIME = "expectedPerformTime";
    String TASK_DUE_DATE = "taskDueDate";
    String TASK_ASSIGNEE_MANAGER = "assigneeManager";
    String TASK_FORM_KEY = "taskFormKey";
    String DOC_TYPE = "docType";
    String DOC_STATUS_NAME = "docStatusName";
    String DOC_STATUS_TITLE = "docStatusTitle";
    String TASK_ACTORS = "taskActors";
    String EMPTY_VALUE_KEY = "{empty}";

    List<HistoryRecordEntity> saveOrUpdateRecords(String jsonRecords) throws IOException, ParseException;

    HistoryRecordEntity saveOrUpdateRecord(HistoryRecordEntity historyRecord, Map<String, String> requestParams)
        throws ParseException;
}
