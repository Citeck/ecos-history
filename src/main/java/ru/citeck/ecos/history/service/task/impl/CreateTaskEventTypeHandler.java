package ru.citeck.ecos.history.service.task.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.domain.TaskRecordEntity;
import ru.citeck.ecos.history.dto.DocumentInfo;
import ru.citeck.ecos.history.service.HistoryRecordService;
import ru.citeck.ecos.history.service.task.AbstractTaskHistoryEventHandler;
import ru.citeck.ecos.history.service.utils.TaskPopulateUtils;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.spring.RemoteRecordsUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;

@Slf4j
@Service
public class CreateTaskEventTypeHandler extends AbstractTaskHistoryEventHandler {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    private static final String CREATE_TASK_TYPE = "task.create";
    private static final String WORKSPACE_SPACES_STORE = "workspace://SpacesStore/";

    private RecordsService recordsService;

    @Override
    public String getEventType() {
        return CREATE_TASK_TYPE;
    }

    @Override
    public void handle(HistoryRecordEntity historyRecord, Map<String, String> requestParams) {
        TaskRecordEntity taskRecordEntity = getOrCreateTask(historyRecord);
        if (taskRecordEntity == null) {
            return;
        }

        TaskPopulateUtils.populateWorkflowProps(taskRecordEntity, historyRecord);

        taskRecordEntity.setStartEvent(historyRecord);
        taskRecordEntity.setStartEventDate(historyRecord.getCreationTime());

        taskRecordEntity.setAssignee(historyRecord.getInitiator());
        taskRecordEntity.setAssigneeManager(requestParams.get(HistoryRecordService.TASK_ASSIGNEE_MANAGER));

        String dueDate = requestParams.get(HistoryRecordService.TASK_DUE_DATE);
        if (dueDate != null) {
            try {
                taskRecordEntity.setDueDate(dateFormat.parse(dueDate));
            } catch (ParseException e) {
                log.error("Can't parse due date: " + dueDate);
            }
        }

        RecordRef documentRef = composeRecordRef(historyRecord.getDocumentId());
        DocumentInfo documentMeta = RemoteRecordsUtils.runAsSystem(() ->
            recordsService.getMeta(documentRef, DocumentInfo.class));
        TaskPopulateUtils.populateDocumentProps(taskRecordEntity, documentMeta);

        taskRecordRepository.save(taskRecordEntity);
    }

    private RecordRef composeRecordRef(String documentId) {
        String id = WORKSPACE_SPACES_STORE + documentId;
        return RecordRef.create("alfresco", "", id);
    }

    @Autowired
    public void setRecordsService(RecordsService recordsService) {
        this.recordsService = recordsService;
    }

}
