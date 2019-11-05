package ru.citeck.ecos.history.service;

import ru.citeck.ecos.history.domain.ActorRecordEntity;

public interface ActorRecordService {

    ActorRecordEntity findOrCreateActorByName(String name);

}
