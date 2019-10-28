package ru.citeck.ecos.history.repository;

import org.springframework.data.repository.CrudRepository;
import ru.citeck.ecos.history.domain.TaskActorRecordEntity;
import ru.citeck.ecos.history.domain.TaskActorRecordEntityId;

public interface TaskActorRecordRepository extends CrudRepository<TaskActorRecordEntity, TaskActorRecordEntityId> {
}
