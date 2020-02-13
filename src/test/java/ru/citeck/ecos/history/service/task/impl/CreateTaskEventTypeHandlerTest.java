package ru.citeck.ecos.history.service.task.impl;

import org.junit.Test;
import org.mockito.Mock;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.domain.TaskRecordEntity;
import ru.citeck.ecos.history.dto.DocumentInfo;
import ru.citeck.ecos.history.repository.TaskRecordRepository;
import ru.citeck.ecos.history.service.HistoryRecordService;
import ru.citeck.ecos.history.service.utils.TaskPopulateUtils;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceImpl;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class CreateTaskEventTypeHandlerTest {

    private static final Long TASK_RECORD_ID = 78L;
    private static final String TASK_ID = "activiti$1";
    private static final String WORKFLOW_TASK_ID = "activiti$3";
    private static final String INITIATOR = "init";
    private static final String ASSIGNEE_MANAGER = "assigneeManager";
    private static final String DOCUMENT_ID = "ef28313f-0367-4803-afc7-98f40e7ad0d2";
    private static final String STATUS_DRAFT = "draft";
    private static final String DOCUMENT_TYPE = "contracts:agreement";
    private static final String LAST_TASK_COMMENT = "lastComments";

    @Test
    public void handle() {
        RecordsService recordsService = mock(RecordsServiceImpl.class);
        TaskPopulateUtils taskPopulateUtils = new TaskPopulateUtils(recordsService);
        CreateTaskEventTypeHandler handler = new CreateTaskEventTypeHandler(taskPopulateUtils);

        TaskRecordRepository repository = mock(TaskRecordRepository.class);
        when(repository.getByTaskId(anyString())).thenReturn(null);
        when(repository.save(any())).then(invocation -> {
            TaskRecordEntity record = invocation.getArgument(0);
            record.setId(TASK_RECORD_ID);
            return record;
        }).then(invocation -> {
            TaskRecordEntity record = invocation.getArgument(0);
            performAssertion(record);
            return record;
        });
        handler.setTaskRecordRepository(repository);


        when(recordsService.getMeta(any(RecordRef.class), any()))
            .then(invocation -> {
                RecordRef documentRef = invocation.getArgument(0);
                DocumentInfo result = new DocumentInfo();
                result.setId(documentRef.toString());
                result.setDocumentType(DOCUMENT_TYPE);
                result.setStatusName(STATUS_DRAFT);
                result.setStatusTitleRu(STATUS_DRAFT);
                result.setStatusTitleEn(STATUS_DRAFT);
                return result;
            });

        HistoryRecordEntity historyRecordEntity = new HistoryRecordEntity();
        historyRecordEntity.setTaskEventInstanceId(TASK_ID);
        historyRecordEntity.setDocumentId(DOCUMENT_ID);
        historyRecordEntity.setWorkflowInstanceId(WORKFLOW_TASK_ID);
        historyRecordEntity.setInitiator(INITIATOR);
        historyRecordEntity.setCreationTime(new Date());
        historyRecordEntity.setLastTaskComment(LAST_TASK_COMMENT);

        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(HistoryRecordService.TASK_ASSIGNEE_MANAGER, ASSIGNEE_MANAGER);
        requestParams.put(HistoryRecordService.TASK_DUE_DATE, "31.10.2019 12:12:12");

        handler.handle(historyRecordEntity, requestParams);
    }

    private void performAssertion(TaskRecordEntity record) {
        assertEquals(record.getId(), TASK_RECORD_ID);
        assertEquals(record.getTaskId(), TASK_ID);
        assertEquals(record.getDocumentId(), DOCUMENT_ID);
        assertEquals(record.getWorkflowId(), WORKFLOW_TASK_ID);
        assertEquals(record.getAssignee(), INITIATOR);
        assertEquals(record.getAssigneeManager(), ASSIGNEE_MANAGER);
        assertEquals(record.getDocumentType(), DOCUMENT_TYPE);
        assertEquals(record.getDocumentStatusName(), STATUS_DRAFT);
        assertEquals(record.getDocumentStatusTitle(), STATUS_DRAFT + "|" + STATUS_DRAFT);
        assertEquals(record.getLastTaskComment(), LAST_TASK_COMMENT);
        assertNotNull(record.getStartEventDate());
    }

}
