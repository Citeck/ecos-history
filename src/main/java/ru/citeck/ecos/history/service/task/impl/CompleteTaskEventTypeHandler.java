package ru.citeck.ecos.history.service.task.impl;

import org.springframework.stereotype.Service;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.domain.TaskRecordEntity;
import ru.citeck.ecos.history.service.task.AbstractTaskHistoryEventHandler;

import java.util.Map;

@Service
public class CompleteTaskEventTypeHandler extends AbstractTaskHistoryEventHandler {

    private static final String COMPLETE_TASK_TYPE = "task.complete";

    @Override
    public String getEventType() {
        return COMPLETE_TASK_TYPE;
    }

    @Override
    public void handle(HistoryRecordEntity historyRecord, Map<String, String> requestParams) {
        TaskRecordEntity taskRecordEntity = getOrCreateTask(historyRecord);
        if (taskRecordEntity == null) {
            return;
        }

        taskRecordEntity.setDocumentId(historyRecord.getDocumentId());
        taskRecordEntity.setWorkflowId(historyRecord.getWorkflowInstanceId());
        taskRecordEntity.setFormKey(historyRecord.getTaskFormKey());

        taskRecordEntity.setCompleteEvent(historyRecord);
        taskRecordEntity.setCompleteEventDate(historyRecord.getCreationTime());

        taskRecordEntity.setCompletionComment(historyRecord.getComments());

        taskRecordEntity.setLastTaskComment(historyRecord.getLastTaskComment());

        taskRecordRepository.save(taskRecordEntity);
    }

}
