package ru.citeck.ecos.history.service.task.impl;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.domain.TaskRecordEntity;
import ru.citeck.ecos.history.service.DeferredActorsLoadingService;
import ru.citeck.ecos.history.service.task.AbstractTaskHistoryEventHandler;

import java.util.List;
import java.util.Map;

@Service
public class WorkflowEndCancelledEventTypeHandler extends AbstractTaskHistoryEventHandler {

    private static final String WORKFLOW_END_CANCELLED_TYPE = "workflow.end.cancelled";

    private DeferredActorsLoadingService deferredActorsLoadingService;

    @Override
    public String getEventType() {
        return WORKFLOW_END_CANCELLED_TYPE;
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

        for (TaskRecordEntity taskRecordEntity : tasks) {
            taskRecordRepository.delete(taskRecordEntity);
            deferredActorsLoadingService.disableDeferredActorLoading(taskRecordEntity);
        }
    }

    @Autowired
    public void setDeferredActorsLoadingService(DeferredActorsLoadingService deferredActorsLoadingService) {
        this.deferredActorsLoadingService = deferredActorsLoadingService;
    }
}
