package ru.citeck.ecos.history.service.task.impl;

import org.junit.Test;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.domain.TaskRecordEntity;
import ru.citeck.ecos.history.repository.TaskRecordRepository;
import ru.citeck.ecos.history.service.utils.TaskPopulateUtils;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceImpl;

import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CompleteTaskEventTypeHandlerTest {

    private static final Long TASK_RECORD_ID = 78L;
    private static final String TASK_ID = "activiti$1";
    private static final String WORKFLOW_TASK_ID = "activiti$3";
    private static final String DOCUMENT_ID = "ef28313f-0367-4803-afc7-98f40e7ad0d2";
    private static final String COMMENT = "comments";
    private static final String LAST_TASK_COMMENT = "lastComments";

    @Test
    public void handle() {
        RecordsService recordsService = mock(RecordsServiceImpl.class);
        TaskPopulateUtils taskPopulateUtils = new TaskPopulateUtils(recordsService);
        CompleteTaskEventTypeHandler handler = new CompleteTaskEventTypeHandler(taskPopulateUtils);

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

        HistoryRecordEntity historyRecordEntity = new HistoryRecordEntity();
        historyRecordEntity.setTaskEventInstanceId(TASK_ID);
        historyRecordEntity.setDocumentId(DOCUMENT_ID);
        historyRecordEntity.setWorkflowInstanceId(WORKFLOW_TASK_ID);
        historyRecordEntity.setCreationTime(new Date());
        historyRecordEntity.setComments(COMMENT);
        historyRecordEntity.setLastTaskComment(LAST_TASK_COMMENT);

        handler.handle(historyRecordEntity, Collections.emptyMap());
    }

    private void performAssertion(TaskRecordEntity record) {
        assertEquals(record.getId(), TASK_RECORD_ID);
        assertEquals(record.getTaskId(), TASK_ID);
        assertEquals(record.getDocumentId(), DOCUMENT_ID);
        assertEquals(record.getWorkflowId(), WORKFLOW_TASK_ID);
        assertNotNull(record.getCompleteEventDate());
        assertEquals(record.getCompletionComment(), COMMENT);
        assertEquals(record.getLastTaskComment(), LAST_TASK_COMMENT);
    }
}
