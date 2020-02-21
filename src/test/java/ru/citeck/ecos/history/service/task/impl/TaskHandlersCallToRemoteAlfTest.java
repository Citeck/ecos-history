package ru.citeck.ecos.history.service.task.impl;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import ru.citeck.ecos.history.HistoryApp;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.dto.ActorsInfo;
import ru.citeck.ecos.history.dto.DocumentInfo;
import ru.citeck.ecos.history.service.ActorService;
import ru.citeck.ecos.history.service.HistoryRecordService;
import ru.citeck.ecos.history.service.utils.TaskPopulateUtils;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static ru.citeck.ecos.history.service.HistoryRecordService.*;
import static ru.citeck.ecos.history.service.task.impl.HandlersTestUtils.generateTaskBaseParams;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = HistoryApp.class)
public class TaskHandlersCallToRemoteAlfTest {

    @MockBean
    private RecordsService recordsService;

    @Autowired
    private HistoryRecordService historyRecordService;

    @Autowired
    private ActorService actorService;

    @Test
    public void callToRemoteAlfrescoTaskAssign() throws ParseException {
        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put(TASK_ACTORS, "[{\"id\":\"workspace://SpacesStore/some-node-ref-48484\"," +
            "\"authorityName\":\"GROUP_clerks\",\"userName\":null,\"firstName\":null,\"lastName\":null," +
            "\"middleName\":null,\"displayName\":\"Делопроизводители\",\"containedUsers\":[]}]");

        RecordRef recordRefAssign = saveTaskEvent("task.assign", additionalParams);
        verify(recordsService).getMeta(recordRefAssign, DocumentInfo.class);
    }

    @Test
    public void callToRemoteAlfrescoTaskAssignBecauseEmptyActorsData() throws ParseException {
        String taskId = "activiti&37373";

        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put(DOC_TYPE, "payment");
        additionalParams.put(DOC_STATUS_NAME, "draft");
        additionalParams.put(TASK_ACTORS, "");
        additionalParams.put(TASK_EVENT_INSTANCE_ID, taskId);

        saveTaskEvent("task.assign", additionalParams, null);

        RecordRef recordRefTask = actorService.composeTaskRecordRef(taskId);

        verify(recordsService).getMeta(recordRefTask, ActorsInfo.class);
    }

    @Test
    public void callToRemoteAlfrescoTaskAssignBecauseEmptyListActorsData() throws ParseException {
        String taskId = "activiti&67544332";

        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put(DOC_TYPE, "payment");
        additionalParams.put(DOC_STATUS_NAME, "draft");
        additionalParams.put(TASK_ACTORS, "[]");
        additionalParams.put(TASK_EVENT_INSTANCE_ID, taskId);

        saveTaskEvent("task.assign", additionalParams, null);

        RecordRef recordRefTask = actorService.composeTaskRecordRef(taskId);

        verify(recordsService).getMeta(recordRefTask, ActorsInfo.class);
    }

    @Test
    public void callToRemoteAlfrescoTaskAssignBecauseInvalidActorsData() throws ParseException {
        String taskId = "activiti&8003";

        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put(DOC_TYPE, "payment");
        additionalParams.put(DOC_STATUS_NAME, "draft");
        additionalParams.put(TASK_ACTORS, "its invalid json");
        additionalParams.put(TASK_EVENT_INSTANCE_ID, taskId);

        saveTaskEvent("task.assign", additionalParams, null);

        RecordRef recordRefTask = actorService.composeTaskRecordRef(taskId);

        verify(recordsService).getMeta(recordRefTask, ActorsInfo.class);
    }

    @Test
    public void callToRemoteAlfrescoTaskAssignBecauseNullActorsData() throws ParseException {
        String taskId = "activiti&8003";

        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put(DOC_TYPE, "payment");
        additionalParams.put(DOC_STATUS_NAME, "draft");
        additionalParams.put(TASK_ACTORS, null);
        additionalParams.put(TASK_EVENT_INSTANCE_ID, taskId);

        saveTaskEvent("task.assign", additionalParams, null);

        RecordRef recordRefTask = actorService.composeTaskRecordRef(taskId);

        verify(recordsService).getMeta(recordRefTask, ActorsInfo.class);
    }

    @Test
    public void callToRemoteAlfrescoTaskAssignAllDataMissing() throws ParseException {
        String taskId = "activiti&4009";

        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put(TASK_EVENT_INSTANCE_ID, taskId);

        RecordRef recordRef = saveTaskEvent("task.assign", additionalParams, null);

        RecordRef recordRefTask = actorService.composeTaskRecordRef(taskId);

        verify(recordsService).getMeta(recordRef, DocumentInfo.class);
        verify(recordsService).getMeta(recordRefTask, ActorsInfo.class);
    }

    @Test
    public void callToRemoteAlfrescoTaskCreate() throws ParseException {
        RecordRef recordRefCreate = saveTaskEvent("task.create");
        verify(recordsService).getMeta(recordRefCreate, DocumentInfo.class);
    }

    @Test
    public void callToRemoteAlfrescoStatusChangedWithOneTask() throws ParseException {
        String contractDocId = "contract-document-23";

        RecordRef documentRecordRef = saveTaskEvent("task.create", contractDocId);

        saveTaskEvent("status.changed", contractDocId);

        verify(recordsService, times(2)).getMeta(documentRecordRef,
            DocumentInfo.class);
    }

    @Test
    public void callToRemoteAlfrescoStatusChangedWithMultipleTasks() throws ParseException {
        String contractDocId = "contract-document-876";

        RecordRef documentRecordRef = saveTaskEvent("task.create", contractDocId);
        saveTaskEvent("task.create", contractDocId);
        saveTaskEvent("task.create", contractDocId);
        saveTaskEvent("task.create", contractDocId);

        saveTaskEvent("status.changed", contractDocId);

        verify(recordsService, times(5)).getMeta(documentRecordRef,
            DocumentInfo.class);
    }


    @Test
    public void notCallToRemoteAlfrescoTaskCreate() throws ParseException {
        saveTaskEventWithFilledData("task.create");
        verifyRecordGetMetaNotCall();
    }

    @Test
    public void notCallToRemoteAlfrescoTaskAssign() throws ParseException {
        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put(DOC_TYPE, "payment");
        additionalParams.put(DOC_STATUS_NAME, "draft");
        additionalParams.put(TASK_ACTORS, "[{\"id\":\"workspace://SpacesStore/some-node-ref-48484\"," +
            "\"authorityName\":\"GROUP_clerks\",\"userName\":null,\"firstName\":null,\"lastName\":null," +
            "\"middleName\":null,\"displayName\":\"Делопроизводители\",\"containedUsers\":[]}]");

        saveTaskEvent("task.assign", additionalParams);
        verifyRecordGetMetaNotCall();
    }

    @Test
    public void notCallToRemoteAlfrescoTaskComplete() throws ParseException {
        saveTaskEventWithFilledData("task.complete");
        verifyRecordGetMetaNotCall();
    }

    @Test
    public void notCallToRemoteAlfrescoWfEndCancelled() throws ParseException {
        saveTaskEventWithFilledData("workflow.end.cancelled");
        verifyRecordGetMetaNotCall();
    }

    @Test
    public void notCallToRemoteAlfrescoWfEnd() throws ParseException {
        saveTaskEventWithFilledData("workflow.end");
        verifyRecordGetMetaNotCall();
    }

    @Test
    public void notCallToRemoteAlfrescoStatusStatusChangedOneTask() throws ParseException {
        saveTaskEventWithFilledData("status.changed");
        verifyRecordGetMetaNotCall();
    }

    @Test
    public void notCallToRemoteAlfrescoStatusStatusChangedMultipleTask() throws ParseException {
        String documentId = "document-1376";

        saveTaskEventWithFilledData("task.create", documentId);
        saveTaskEventWithFilledData("task.create", documentId);
        saveTaskEventWithFilledData("task.create", documentId);
        saveTaskEventWithFilledData("task.create", documentId);

        saveTaskEventWithFilledData("status.changed", documentId);

        verifyRecordGetMetaNotCall();
    }

    private void verifyRecordGetMetaNotCall() {
        verify(recordsService, times(0)).getMeta(any(RecordRef.class), any());
        verify(recordsService, times(0)).getMeta(anyCollection(), anyString());
        verify(recordsService, times(0)).getMeta(anyCollection(),
            any(Class.class));
    }

    private RecordRef saveTaskEvent(String type) throws ParseException {
        return saveTaskEvent(type, new HashMap<>());
    }

    private RecordRef saveTaskEvent(String type, String id) throws ParseException {
        return saveTaskEvent(type, new HashMap<>(), id);
    }

    private RecordRef saveTaskEvent(String type, Map<String, String> additionalParams) throws ParseException {
        return saveTaskEvent(type, additionalParams, null);
    }

    private RecordRef saveTaskEvent(String type, Map<String, String> additionalParams, String docId)
        throws ParseException {
        String id = StringUtils.isNotBlank(docId) ? docId : UUID.randomUUID().toString();
        RecordRef recordRef = TaskPopulateUtils.composeRecordRef(id);

        Map<String, String> requestParams = generateTaskBaseParams(id);
        requestParams.put(EVENT_TYPE, type);
        requestParams.putAll(additionalParams);

        when(recordsService.getMeta(recordRef, DocumentInfo.class)).thenReturn(new DocumentInfo());

        historyRecordService.saveOrUpdateRecord(new HistoryRecordEntity(), requestParams);

        return recordRef;
    }

    private void saveTaskEventWithFilledData(String type) throws ParseException {
        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put(DOC_TYPE, "some-doc-type");
        additionalParams.put(DOC_STATUS_NAME, "some-status");
        saveTaskEvent(type, additionalParams, null);
    }

    private void saveTaskEventWithFilledData(String type, String docId) throws ParseException {
        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put(DOC_TYPE, "some-doc-type");
        additionalParams.put(DOC_STATUS_NAME, "some-status");
        saveTaskEvent(type, additionalParams, docId);
    }

}
