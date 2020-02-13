package ru.citeck.ecos.history.service.task.impl;

import org.springframework.stereotype.Service;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.domain.TaskRecordEntity;
import ru.citeck.ecos.history.service.task.AbstractTaskHistoryEventHandler;
import ru.citeck.ecos.history.service.utils.TaskPopulateUtils;

import java.util.Map;

@Service
public class CompleteTaskEventTypeHandler extends AbstractTaskHistoryEventHandler {

    private static final String COMPLETE_TASK_TYPE = "task.complete";

    private final TaskPopulateUtils taskPopulateUtils;

    public CompleteTaskEventTypeHandler(TaskPopulateUtils taskPopulateUtils) {
        this.taskPopulateUtils = taskPopulateUtils;
    }

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

        taskPopulateUtils.populateWorkflowProps(taskRecordEntity, historyRecord);

        taskRecordEntity.setCompleteEvent(historyRecord);
        taskRecordEntity.setCompleteEventDate(historyRecord.getCreationTime());

        taskRecordEntity.setCompletionComment(historyRecord.getComments());

        taskRecordRepository.save(taskRecordEntity);
    }

}
