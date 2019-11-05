package ru.citeck.ecos.history.service;

import ru.citeck.ecos.history.domain.ActorRecordEntity;
import ru.citeck.ecos.history.domain.TaskActorRecordEntity;
import ru.citeck.ecos.history.domain.TaskRecordEntity;

public interface TaskActorRecordService {

    TaskActorRecordEntity findOrCreateByEntities(TaskRecordEntity taskRecordEntity, ActorRecordEntity actorRecordEntity);

}
