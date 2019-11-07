package ru.citeck.ecos.history.service.impl;

import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.history.domain.ActorRecordEntity;
import ru.citeck.ecos.history.repository.ActorRecordRepository;
import ru.citeck.ecos.history.service.ActorService;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.spring.RemoteRecordsUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service("actorRecordService")
public class ActorServiceImpl implements ActorService {

    private ActorRecordRepository actorRecordRepository;
    private RecordsService recordsService;

    @Override
    public ActorRecordEntity findOrCreateActorByName(String name) {
        ActorRecordEntity actor = actorRecordRepository.findByActorNameIgnoreCase(name);
        if (actor == null) {
            actor = new ActorRecordEntity();
            actor.setActorName(name);
            actor = actorRecordRepository.save(actor);
        }
        return actor;
    }

    @Override
    public Set<String> queryActorsFromRemote(String taskId) throws IllegalStateException {
        ActorsInfo actorsInfo = getTaskInfo(taskId);
        Set<String> resultActors = new HashSet<>();
        if (actorsInfo != null && CollectionUtils.isNotEmpty(actorsInfo.actors)) {
            actorsInfo.actors.forEach(actor -> {
                if (StringUtils.isNotBlank(actor.authorityName)) {
                    resultActors.add(actor.authorityName);
                } else if (StringUtils.isNotBlank(actor.userName)) {
                    resultActors.add(actor.userName);
                }
            });
        }
        return resultActors;
    }

    private ActorsInfo getTaskInfo(String taskId) throws IllegalStateException {
        return RemoteRecordsUtils.runAsSystem(() ->
            recordsService.getMeta(composeTaskRecordRef(taskId), ActorsInfo.class));
    }

    private RecordRef composeTaskRecordRef(String taskId) {
        return RecordRef.create("alfresco", "wftask", taskId);
    }

    @Autowired
    public void setActorRecordRepository(ActorRecordRepository actorRecordRepository) {
        this.actorRecordRepository = actorRecordRepository;
    }

    @Autowired
    public void setRecordsService(RecordsService recordsService) {
        this.recordsService = recordsService;
    }


    @Data
    private static class ActorsInfo {
        private List<AuthorityDto> actors;
    }

    @Data
    private static class AuthorityDto {
        private String authorityName;
        private String userName;
    }
}
