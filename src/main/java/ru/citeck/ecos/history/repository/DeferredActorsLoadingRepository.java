package ru.citeck.ecos.history.repository;

import org.springframework.data.repository.CrudRepository;
import ru.citeck.ecos.history.domain.DeferredActorsLoadingEntity;

public interface DeferredActorsLoadingRepository extends CrudRepository<DeferredActorsLoadingEntity, Long> {

    DeferredActorsLoadingEntity findByTaskId(String taskId);

}
