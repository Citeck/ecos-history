package ru.citeck.ecos.history.repository;

import org.springframework.data.repository.CrudRepository;
import ru.citeck.ecos.history.domain.ActorRecordEntity;

public interface ActorRecordRepository extends CrudRepository<ActorRecordEntity, Long> {

    ActorRecordEntity findByActorName(String actorName);

}
