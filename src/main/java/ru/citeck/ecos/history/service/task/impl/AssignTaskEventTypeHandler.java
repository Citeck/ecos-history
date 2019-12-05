package ru.citeck.ecos.history.service.task.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.history.domain.ActorRecordEntity;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.domain.TaskActorRecordEntity;
import ru.citeck.ecos.history.domain.TaskRecordEntity;
import ru.citeck.ecos.history.service.ActorService;
import ru.citeck.ecos.history.service.DeferredActorsLoadingService;
import ru.citeck.ecos.history.service.HistoryRecordService;
import ru.citeck.ecos.history.service.TaskActorRecordService;
import ru.citeck.ecos.history.service.task.AbstractTaskHistoryEventHandler;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AssignTaskEventTypeHandler extends AbstractTaskHistoryEventHandler {

    private static final String ASSIGN_TASK_TYPE = "task.assign";

    private ActorService actorService;
    private TaskActorRecordService taskActorRecordService;
    private DeferredActorsLoadingService deferredActorsLoadingService;

    @Override
    public String getEventType() {
        return ASSIGN_TASK_TYPE;
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

        taskRecordEntity.setAssignee(historyRecord.getInitiator());
        taskRecordEntity.setAssigneeManager(requestParams.get(HistoryRecordService.TASK_ASSIGNEE_MANAGER));

        taskRecordEntity.setAssignEvent(historyRecord);
        taskRecordEntity.setAssignEventDate(historyRecord.getCreationTime());

        taskRecordEntity.setLastTaskComment(historyRecord.getLastTaskComment());

        try {
            Set<String> actors = actorService.queryActorsFromRemote(taskRecordEntity.getTaskId());
            taskRecordEntity.setActors(findActorEntities(taskRecordEntity, actors));
        } catch (Exception e) {
            log.error("Error while receiving actors from remote", e);
            deferredActorsLoadingService.enableDeferredActorLoading(taskRecordEntity);
        }

        taskRecordRepository.save(taskRecordEntity);
    }

    private List<TaskActorRecordEntity> findActorEntities(TaskRecordEntity taskRecordEntity, Set<String> actors) {
        if (CollectionUtils.isEmpty(actors)) {
            return null;
        }

        Set<ActorRecordEntity> actorRecordEntities = actors.stream()
            .map(actorService::findOrCreateActorByName)
            .collect(Collectors.toSet());

        return actorRecordEntities.stream()
            .map(actorRecordEntity -> taskActorRecordService.findOrCreateByEntities(taskRecordEntity, actorRecordEntity))
            .collect(Collectors.toList());
    }

    @Autowired
    public void setActorService(ActorService actorService) {
        this.actorService = actorService;
    }

    @Autowired
    public void setTaskActorRecordService(TaskActorRecordService taskActorRecordService) {
        this.taskActorRecordService = taskActorRecordService;
    }

    @Autowired
    public void setDeferredActorsLoadingService(DeferredActorsLoadingService deferredActorsLoadingService) {
        this.deferredActorsLoadingService = deferredActorsLoadingService;
    }
}
