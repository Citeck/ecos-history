package ru.citeck.ecos.history.service.task.impl;

import org.junit.Before;
import org.junit.Test;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.domain.TaskRecordEntity;
import ru.citeck.ecos.history.repository.TaskRecordRepository;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StatusChangeEventTypeHandlerTest {

    private static final String DOCUMENT_ID = "ef28313f-0367-4803-afc7-98f40e7ad0d2";
    private static final String STATUS_DRAFT = "draft";

    private static final int SAVES_COUNT_REQUIRED = 2;

    private int saveCount;

    @Before
    public void setUp() {
        saveCount = 0;
    }

    @Test
    public void handle() {
        StatusChangeEventTypeHandler handler = new StatusChangeEventTypeHandler();

        TaskRecordRepository repository = mock(TaskRecordRepository.class);
        when(repository.getByDocumentId(eq(DOCUMENT_ID))).then(invocation -> mockFindTasksByDocumentId());
        when(repository.save(any())).then(invocation -> {
            TaskRecordEntity record = invocation.getArgument(0);
            saveCount++;
            performAssertion(record);
            return record;
        });
        handler.setTaskRecordRepository(repository);

        RecordsService recordsService = mock(RecordsServiceImpl.class);
        when(recordsService.getMeta(any(RecordRef.class), any())).then(invocation -> {
            RecordRef documentRef = invocation.getArgument(0);
            StatusChangeEventTypeHandler.DocumentStatus result = new StatusChangeEventTypeHandler.DocumentStatus();
            result.setId(documentRef.toString());
            result.setStatusName(STATUS_DRAFT);
            result.setStatusTitleRu(STATUS_DRAFT);
            result.setStatusTitleEn(STATUS_DRAFT);
            return result;
        });
        handler.setRecordsService(recordsService);

        HistoryRecordEntity historyRecordEntity = new HistoryRecordEntity();
        historyRecordEntity.setDocumentId(DOCUMENT_ID);

        handler.handle(historyRecordEntity, Collections.emptyMap());

        assertEquals(saveCount, SAVES_COUNT_REQUIRED);
    }

    private List<TaskRecordEntity> mockFindTasksByDocumentId() {
        List<TaskRecordEntity> result = new ArrayList<>();
        for (int i = 0; i < SAVES_COUNT_REQUIRED; i++) {
            TaskRecordEntity entity = new TaskRecordEntity();
            entity.setDocumentId(DOCUMENT_ID);
            result.add(entity);
        }
        return result;
    }

    private void performAssertion(TaskRecordEntity record) {
        assertEquals(record.getDocumentStatusName(), STATUS_DRAFT);
        assertEquals(record.getDocumentStatusTitle(), STATUS_DRAFT + "|" + STATUS_DRAFT);
    }

}
