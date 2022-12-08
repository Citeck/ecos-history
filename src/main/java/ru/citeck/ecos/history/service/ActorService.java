package ru.citeck.ecos.history.service;

import org.apache.commons.lang3.StringUtils;
import ru.citeck.ecos.history.domain.ActorRecordEntity;
import ru.citeck.ecos.records2.RecordRef;

import java.util.Set;

public interface ActorService {

    ActorRecordEntity findOrCreateActorByName(String name);

    Set<String> queryActorsFromRemote(String taskId);

    default RecordRef composeTaskRecordRef(String taskId) {
        RecordRef taskRef = RecordRef.valueOf(taskId);

        if (StringUtils.isBlank(taskRef.appName)) {
            taskRef = taskRef.withAppName("alfresco");
        }

        if (StringUtils.isBlank(taskRef.sourceId)) {
            taskRef = taskRef.withSourceId("wftask");
        }

        return taskRef;
    }

}
