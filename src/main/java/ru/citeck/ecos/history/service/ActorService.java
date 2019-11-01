package ru.citeck.ecos.history.service;

import ru.citeck.ecos.history.domain.ActorRecordEntity;

import java.util.Set;

public interface ActorService {

    ActorRecordEntity findOrCreateActorByName(String name);

    Set<String> queryActorsFromRemote(String taskId);

}
