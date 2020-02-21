package ru.citeck.ecos.history.service.task.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.history.domain.ActorRecordEntity;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.domain.TaskActorRecordEntity;
import ru.citeck.ecos.history.domain.TaskRecordEntity;
import ru.citeck.ecos.history.dto.AuthorityDto;
import ru.citeck.ecos.history.service.ActorService;
import ru.citeck.ecos.history.service.DeferredActorsLoadingService;
import ru.citeck.ecos.history.service.HistoryRecordService;
import ru.citeck.ecos.history.service.TaskActorRecordService;
import ru.citeck.ecos.history.service.task.AbstractTaskHistoryEventHandler;
import ru.citeck.ecos.history.service.utils.TaskPopulateUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static ru.citeck.ecos.history.service.HistoryRecordService.TASK_ACTORS;

@Slf4j
@Service
public class AssignTaskEventTypeHandler extends AbstractTaskHistoryEventHandler {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String ASSIGN_TASK_TYPE = "task.assign";

    private final ActorService actorService;
    private final TaskActorRecordService taskActorRecordService;
    private final DeferredActorsLoadingService deferredActorsLoadingService;
    private final TaskPopulateUtils populateUtils;

    public AssignTaskEventTypeHandler(ActorService actorService, TaskActorRecordService taskActorRecordService,
                                      DeferredActorsLoadingService deferredActorsLoadingService,
                                      TaskPopulateUtils populateUtils) {
        this.actorService = actorService;
        this.taskActorRecordService = taskActorRecordService;
        this.deferredActorsLoadingService = deferredActorsLoadingService;
        this.populateUtils = populateUtils;
    }

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

        populateUtils.populateWorkflowProps(taskRecordEntity, historyRecord);
        populateUtils.populateDocumentProps(taskRecordEntity, historyRecord);

        taskRecordEntity.setAssignEvent(historyRecord);
        taskRecordEntity.setAssignEventDate(historyRecord.getCreationTime());

        taskRecordEntity.setAssignee(historyRecord.getInitiator());
        taskRecordEntity.setAssigneeManager(requestParams.get(HistoryRecordService.TASK_ASSIGNEE_MANAGER));

        Set<String> actors = getActors(taskRecordEntity, historyRecord, requestParams);
        taskRecordEntity.setActors(findActorEntities(taskRecordEntity, actors));

        taskRecordRepository.save(taskRecordEntity);
    }

    private Set<String> getActors(TaskRecordEntity taskRecordEntity, HistoryRecordEntity historyRecord,
                                  Map<String, String> requestParams) {
        boolean remoteRequestIsRequired = true;

        Set<String> result = new HashSet<>();

        if (incomingActorsDataIsNoneBlank(requestParams)) {
            remoteRequestIsRequired = false;

            String actorsData = requestParams.get(TASK_ACTORS);
            try {
                result = parseActorsFromStr(actorsData);
                if (CollectionUtils.isEmpty(result)) {
                    log.warn("No actors coming from assign task event: {}. Trying resolve actors from remote alfresco",
                        taskRecordEntity.getTaskId());
                    remoteRequestIsRequired = true;
                }
            } catch (Exception e) {
                remoteRequestIsRequired = true;
                log.error("Failed parse actors from String. Document: " + historyRecord.getDocumentId() + ", taskId: "
                    + taskRecordEntity.getTaskId(), e);
            }
        }

        if (remoteRequestIsRequired) {
            try {
                String taskId = taskRecordEntity.getTaskId();
                log.warn("Remote request to alfresco, getting actors for task: {}", taskId);
                result = actorService.queryActorsFromRemote(taskId);
            } catch (Exception e) {
                log.error("Error while receiving actors from remote", e);
                deferredActorsLoadingService.enableDeferredActorLoading(taskRecordEntity);
            }
        }

        return result;
    }

    private boolean incomingActorsDataIsNoneBlank(Map<String, String> requestParams) {
        return requestParams.containsKey(TASK_ACTORS) && StringUtils.isNoneBlank(requestParams.get(TASK_ACTORS));
    }

    private Set<String> parseActorsFromStr(String actorsData) throws IOException {
        log.debug("Start parsing actors from String: {}", actorsData);

        AuthorityDto[] actors = OBJECT_MAPPER.readValue(actorsData, AuthorityDto[].class);

        log.debug("Parsed AuthorityDto: {}", Arrays.toString(actors));

        Set<String> result = new HashSet<>();

        if (actors != null && actors.length > 0) {
            result = Arrays.stream(actors)
                .map(actor -> {
                    String authorityName = actor.getAuthorityName();
                    String userName = actor.getUserName();

                    if (StringUtils.isNoneBlank(authorityName)) {
                        return authorityName;
                    } else if (StringUtils.isNoneBlank(userName)) {
                        return userName;
                    }

                    return null;
                })
                .filter(StringUtils::isNoneBlank)
                .collect(Collectors.toSet());
        }

        log.debug("Actors result: {}", Arrays.toString(result.toArray()));

        return result;
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

}
