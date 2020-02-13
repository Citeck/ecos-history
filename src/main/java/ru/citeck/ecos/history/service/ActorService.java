package ru.citeck.ecos.history.service;

import ru.citeck.ecos.history.domain.ActorRecordEntity;
import ru.citeck.ecos.records2.RecordRef;

import java.util.Set;

public interface ActorService {

    ActorRecordEntity findOrCreateActorByName(String name);

    Set<String> queryActorsFromRemote(String taskId);

    default RecordRef composeTaskRecordRef(String taskId) {
        return RecordRef.create("alfresco", "wftask", taskId);
    }

}
