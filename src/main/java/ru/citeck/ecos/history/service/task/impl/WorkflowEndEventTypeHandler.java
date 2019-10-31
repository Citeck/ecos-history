package ru.citeck.ecos.history.service.task.impl;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.domain.TaskRecordEntity;
import ru.citeck.ecos.history.service.task.AbstractTaskHistoryEventHandler;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class WorkflowEndEventTypeHandler extends AbstractTaskHistoryEventHandler {

    private static final String WORKFLOW_END_TYPE = "workflow.end";

    @Override
    public String getEventType() {
        return WORKFLOW_END_TYPE;
    }

    @Override
    public void handle(HistoryRecordEntity historyRecord, Map<String, String> requestParams) {
        String workflowId = historyRecord.getWorkflowInstanceId();
        if (StringUtils.isBlank(workflowId)) {
            return;
        }

        List<TaskRecordEntity> tasks = taskRecordRepository.findAllByWorkflowId(workflowId);
        if (CollectionUtils.isEmpty(tasks)) {
            return;
        }

        Date completeDate = historyRecord.getCreationTime();

        for (TaskRecordEntity taskRecordEntity : tasks) {
            if (taskRecordEntity.getCompleteEventDate() != null) {
                continue;
            }

            taskRecordEntity.setCompleteEventDate(completeDate);
            taskRecordRepository.save(taskRecordEntity);
        }
    }

}
