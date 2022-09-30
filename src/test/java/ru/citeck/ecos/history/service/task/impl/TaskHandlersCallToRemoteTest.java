package ru.citeck.ecos.history.service.task.impl;

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import ru.citeck.ecos.history.HistoryApp;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.dto.ActorsInfo;
import ru.citeck.ecos.history.dto.DocumentInfo;
import ru.citeck.ecos.history.service.ActorService;
import ru.citeck.ecos.history.service.HistoryRecordService;
import ru.citeck.ecos.history.service.utils.TaskPopulateUtils;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;
import static ru.citeck.ecos.history.service.HistoryRecordService.*;
import static ru.citeck.ecos.history.service.task.impl.HandlersTestUtils.generateTaskBaseParams;

@ExtendWith(EcosSpringExtension.class)
@SpringBootTest(classes = HistoryApp.class)
public class TaskHandlersCallToRemoteTest {

    @MockBean
    private RecordsService recordsService;

    @Autowired
    private HistoryRecordService historyRecordService;

    @Autowired
    private ActorService actorService;

    @Test
    public void callToRemoteTaskAssign() throws ParseException {
        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put(TASK_ACTORS, "[{\"id\":\"workspace://SpacesStore/some-node-ref-48484\"," +
            "\"authorityName\":\"GROUP_clerks\",\"userName\":null,\"firstName\":null,\"lastName\":null," +
            "\"middleName\":null,\"displayName\":\"Делопроизводители\",\"containedUsers\":[]}]");

        RecordRef recordRefAssign = saveTaskEvent("task.assign", additionalParams);
        verify(recordsService).getMeta(recordRefAssign, DocumentInfo.class);
    }

    @Test
    public void callToRemoteAlfrescoTaskRefTest() throws ParseException {
        String taskId = "activiti&37373";

        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put(DOC_TYPE, "payment");
        additionalParams.put(DOC_STATUS_NAME, "draft");
        additionalParams.put(TASK_ACTORS, "");
        additionalParams.put(TASK_EVENT_INSTANCE_ID, taskId);

        saveTaskEvent("task.assign", additionalParams, null);

        RecordRef recordRefTask = RecordRef.valueOf("alfresco/wftask@" + taskId);

        verify(recordsService).getMeta(recordRefTask, ActorsInfo.class);
    }

    @Test
    public void callToRemoteTaskRefTest() throws ParseException {
        String taskId = "eproc/task@123";

        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put(DOC_TYPE, "payment");
        additionalParams.put(DOC_STATUS_NAME, "draft");
        additionalParams.put(TASK_ACTORS, "");
        additionalParams.put(TASK_EVENT_INSTANCE_ID, taskId);

        saveTaskEvent("task.assign", additionalParams, null);

        RecordRef recordRefTask = RecordRef.valueOf(taskId);

        verify(recordsService).getMeta(recordRefTask, ActorsInfo.class);
    }

    @Test
    public void callToRemoteTaskAssignBecauseEmptyActorsData() throws ParseException {
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
    public void callToRemoteTaskAssignBecauseEmptyListActorsData() throws ParseException {
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
    public void callToRemoteTaskAssignBecauseInvalidActorsData() throws ParseException {
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
    public void callToRemoteTaskAssignBecauseNullActorsData() throws ParseException {
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
    public void callToRemoteTaskAssignAllDataMissing() throws ParseException {
        String taskId = "activiti&4009";

        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put(TASK_EVENT_INSTANCE_ID, taskId);

        RecordRef recordRef = saveTaskEvent("task.assign", additionalParams, null);

        RecordRef recordRefTask = actorService.composeTaskRecordRef(taskId);

        verify(recordsService).getMeta(recordRef, DocumentInfo.class);
        verify(recordsService).getMeta(recordRefTask, ActorsInfo.class);
    }

    @Test
    public void callToRemoteTaskCreate() throws ParseException {
        RecordRef recordRefCreate = saveTaskEvent("task.create");
        verify(recordsService).getMeta(recordRefCreate, DocumentInfo.class);
    }

    @Test
    public void callToRemoteStatusChangedWithOneTask() throws ParseException {
        String contractDocId = "contract-document-23";

        RecordRef documentRecordRef = saveTaskEvent("task.create", contractDocId);

        saveTaskEvent("status.changed", contractDocId);

        verify(recordsService, times(2)).getMeta(documentRecordRef,
            DocumentInfo.class);
    }

    @Test
    public void callToRemoteStatusChangedWithMultipleTasks() throws ParseException {
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
    public void notcallToRemoteTaskCreate() throws ParseException {
        saveTaskEventWithFilledData("task.create");
        verifyRecordGetMetaNotCall();
    }

    @Test
    public void notcallToRemoteTaskAssign() throws ParseException {
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
    public void notcallToRemoteTaskComplete() throws ParseException {
        saveTaskEventWithFilledData("task.complete");
        verifyRecordGetMetaNotCall();
    }

    @Test
    public void notcallToRemoteWfEndCancelled() throws ParseException {
        saveTaskEventWithFilledData("workflow.end.cancelled");
        verifyRecordGetMetaNotCall();
    }

    @Test
    public void notcallToRemoteWfEnd() throws ParseException {
        saveTaskEventWithFilledData("workflow.end");
        verifyRecordGetMetaNotCall();
    }

    @Test
    public void notcallToRemoteStatusStatusChangedOneTask() throws ParseException {
        saveTaskEventWithFilledData("status.changed");
        verifyRecordGetMetaNotCall();
    }

    @Test
    public void notcallToRemoteStatusStatusChangedMultipleTask() throws ParseException {
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
        verify(recordsService, times(0)).getMeta(anyCollection(), any(Class.class));
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
