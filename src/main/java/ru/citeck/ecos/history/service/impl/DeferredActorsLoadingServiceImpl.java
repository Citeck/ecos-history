package ru.citeck.ecos.history.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.history.domain.DeferredActorsLoadingEntity;
import ru.citeck.ecos.history.domain.TaskRecordEntity;
import ru.citeck.ecos.history.repository.DeferredActorsLoadingRepository;
import ru.citeck.ecos.history.service.DeferredActorsLoadingService;

@Slf4j
@Service
public class DeferredActorsLoadingServiceImpl implements DeferredActorsLoadingService {

    private DeferredActorsLoadingRepository repository;

    @Override
    public boolean enableDeferredActorLoading(TaskRecordEntity taskRecordEntity) {
        String taskId = taskRecordEntity.getTaskId();
        if (StringUtils.isBlank(taskId)) {
            log.info("Received task has no taskId: " + taskRecordEntity);
            return false;
        }

        DeferredActorsLoadingEntity entity = repository.findByTaskId(taskId);
        if (entity != null) {
            log.info("Received task already enabled for deferred actor loading: " + taskId);
            return false;
        }

        DeferredActorsLoadingEntity newEntity = new DeferredActorsLoadingEntity();
        newEntity.setTaskId(taskId);
        repository.save(newEntity);
        return true;
    }

    @Override
    public boolean disableDeferredActorLoading(TaskRecordEntity taskRecordEntity) {
        String taskId = taskRecordEntity.getTaskId();
        if (StringUtils.isBlank(taskId)) {
            log.info("Received task has no taskId: " + taskRecordEntity);
            return false;
        }

        DeferredActorsLoadingEntity entity = repository.findByTaskId(taskId);
        if (entity == null) {
            log.info("Received task already is not enabled for deferred actor loading: " + taskId);
            return false;
        }

        repository.delete(entity);
        return true;
    }

    @Override
    public void disableDeferredActorLoading(DeferredActorsLoadingEntity entity) {
        repository.delete(entity);
    }

    @Override
    public Iterable<DeferredActorsLoadingEntity> findAll() {
        return repository.findAll();
    }

    @Autowired
    public void setRepository(DeferredActorsLoadingRepository repository) {
        this.repository = repository;
    }

}
