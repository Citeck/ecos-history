package ru.citeck.ecos.history.service;

import ru.citeck.ecos.history.domain.DeferredActorsLoadingEntity;
import ru.citeck.ecos.history.domain.TaskRecordEntity;

public interface DeferredActorsLoadingService {

    boolean enableDeferredActorLoading(TaskRecordEntity taskRecordEntity);

    boolean disableDeferredActorLoading(TaskRecordEntity taskRecordEntity);

    void disableDeferredActorLoading(DeferredActorsLoadingEntity entity);

    Iterable<DeferredActorsLoadingEntity> findAll();

}
