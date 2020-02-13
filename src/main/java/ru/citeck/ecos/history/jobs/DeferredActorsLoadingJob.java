package ru.citeck.ecos.history.jobs;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.history.domain.ActorRecordEntity;
import ru.citeck.ecos.history.domain.DeferredActorsLoadingEntity;
import ru.citeck.ecos.history.domain.TaskActorRecordEntity;
import ru.citeck.ecos.history.domain.TaskRecordEntity;
import ru.citeck.ecos.history.service.ActorService;
import ru.citeck.ecos.history.service.DeferredActorsLoadingService;
import ru.citeck.ecos.history.service.TaskActorRecordService;
import ru.citeck.ecos.history.service.TaskRecordService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DeferredActorsLoadingJob {

    private DeferredActorsLoadingService deferredActorsLoadingService;
    private TaskActorRecordService taskActorRecordService;
    private TaskRecordService taskRecordService;
    private ActorService actorService;

    @Value("${ecos-history.deferred-actors-job-enabled}")
    private boolean deferredActorsLoadingJobEnabled;

    @Autowired
    public DeferredActorsLoadingJob(DeferredActorsLoadingService deferredActorsLoadingService,
                                    TaskActorRecordService taskActorRecordService,
                                    TaskRecordService taskRecordService,
                                    ActorService actorService) {
        this.deferredActorsLoadingService = deferredActorsLoadingService;
        this.taskActorRecordService = taskActorRecordService;
        this.taskRecordService = taskRecordService;
        this.actorService = actorService;
    }

    @Scheduled(initialDelay = 10_000, fixedDelay = 60_000)
    public void execute() {
        if (!deferredActorsLoadingJobEnabled) {
            return;
        }

        List<DeferredActorsLoadingEntity> allDeferred = IterableUtils.toList(deferredActorsLoadingService.findAll());
        if (CollectionUtils.isEmpty(allDeferred)) {
            return;
        }

        log.info(String.format("Founded %s tasks for deferred actors loading", allDeferred.size()));

        for (DeferredActorsLoadingEntity deferredEntity : allDeferred) {
            try {
                executeImpl(deferredEntity);
            } catch (Exception e) {
                log.error("Error while deferred actor loading for: " + deferredEntity, e);
            }
        }

        log.info("DeferredActorsLoadingJob work is performed");
    }

    private void executeImpl(DeferredActorsLoadingEntity deferredEntity) {
        Set<String> actors = actorService.queryActorsFromRemote(deferredEntity.getTaskId());

        TaskRecordEntity taskRecordEntity = taskRecordService.findTaskByTaskId(deferredEntity.getTaskId());

        List<TaskActorRecordEntity> actorEntities = findActorEntities(taskRecordEntity, actors);

        taskRecordEntity.setActors(actorEntities);
        taskRecordService.save(taskRecordEntity);

        deferredActorsLoadingService.disableDeferredActorLoading(deferredEntity);
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
