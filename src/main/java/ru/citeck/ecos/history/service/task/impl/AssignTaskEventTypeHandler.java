package ru.citeck.ecos.history.service.task.impl;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.history.domain.ActorRecordEntity;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.domain.TaskActorRecordEntity;
import ru.citeck.ecos.history.domain.TaskRecordEntity;
import ru.citeck.ecos.history.service.ActorRecordService;
import ru.citeck.ecos.history.service.HistoryRecordService;
import ru.citeck.ecos.history.service.TaskActorRecordService;
import ru.citeck.ecos.history.service.TaskRecordService;
import ru.citeck.ecos.history.service.task.AbstractTaskHistoryEventHandler;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AssignTaskEventTypeHandler extends AbstractTaskHistoryEventHandler {

    private static final String ASSIGN_TASK_TYPE = "task.assign";

    private TaskRecordService taskRecordService;
    private ActorRecordService actorRecordService;
    private TaskActorRecordService taskActorRecordService;

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

        taskRecordEntity.setAssignee(historyRecord.getInitiator());
        taskRecordEntity.setAssigneeManager(requestParams.get(HistoryRecordService.TASK_ASSIGNEE_MANAGER));

        taskRecordEntity.setAssignEvent(historyRecord);
        taskRecordEntity.setAssignEventDate(historyRecord.getCreationTime());

        Set<String> actors = findActors(historyRecord);
        taskRecordEntity.setActors(findActorEntities(taskRecordEntity, actors));

        taskRecordRepository.save(taskRecordEntity);
    }

    private Set<String> findActors(HistoryRecordEntity historyRecord) {
        ActorsInfo actorsInfo = taskRecordService.getTaskInfo(historyRecord.getTaskEventInstanceId(), ActorsInfo.class);

        Set<String> resultActors = new HashSet<>();
        if (CollectionUtils.isNotEmpty(actorsInfo.actors)) {
            actorsInfo.actors.forEach(actor -> {
                if (StringUtils.isNotBlank(actor.authorityName)) {
                    resultActors.add(actor.authorityName);
                } else if (StringUtils.isNotBlank(actor.userName)) {
                    resultActors.add(actor.userName);
                }
            });
        }

        return resultActors;
    }

    private List<TaskActorRecordEntity> findActorEntities(TaskRecordEntity taskRecordEntity, Set<String> actors) {
        if (CollectionUtils.isEmpty(actors)) {
            return null;
        }

        Set<ActorRecordEntity> actorRecordEntities = actors.stream()
            .map(actorRecordService::findOrCreateActorByName)
            .collect(Collectors.toSet());

        return actorRecordEntities.stream()
            .map(actorRecordEntity -> taskActorRecordService.findOrCreateByEntities(taskRecordEntity, actorRecordEntity))
            .collect(Collectors.toList());
    }

    @Autowired
    public void setTaskRecordService(TaskRecordService taskRecordService) {
        this.taskRecordService = taskRecordService;
    }

    @Autowired
    public void setActorRecordService(ActorRecordService actorRecordService) {
        this.actorRecordService = actorRecordService;
    }

    @Autowired
    public void setTaskActorRecordService(TaskActorRecordService taskActorRecordService) {
        this.taskActorRecordService = taskActorRecordService;
    }

    @Data
    private static class ActorsInfo {
        private List<AuthortyDto> actors;
    }

    @Data
    private static class AuthortyDto {
        private String authorityName;
        private String userName;
    }
}
