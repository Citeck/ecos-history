package ru.citeck.ecos.history.service.impl;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.history.domain.ActorRecordEntity;
import ru.citeck.ecos.history.dto.ActorsInfo;
import ru.citeck.ecos.history.repository.ActorRecordRepository;
import ru.citeck.ecos.history.service.ActorService;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.rest.RemoteRecordsUtils;

import java.util.HashSet;
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
        if (actorsInfo != null && CollectionUtils.isNotEmpty(actorsInfo.getActors())) {
            actorsInfo.getActors().forEach(actor -> {
                if (StringUtils.isNotBlank(actor.getAuthorityName())) {
                    resultActors.add(actor.getAuthorityName());
                } else if (StringUtils.isNotBlank(actor.getUserName())) {
                    resultActors.add(actor.getUserName());
                }
            });
        }
        return resultActors;
    }

    private ActorsInfo getTaskInfo(String taskId) throws IllegalStateException {
        return RemoteRecordsUtils.runAsSystem(() ->
            recordsService.getMeta(composeTaskRecordRef(taskId), ActorsInfo.class));
    }

    @Autowired
    public void setActorRecordRepository(ActorRecordRepository actorRecordRepository) {
        this.actorRecordRepository = actorRecordRepository;
    }

    @Autowired
    public void setRecordsService(RecordsService recordsService) {
        this.recordsService = recordsService;
    }

}
