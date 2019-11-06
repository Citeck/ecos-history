package ru.citeck.ecos.history.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.history.domain.ActorRecordEntity;
import ru.citeck.ecos.history.repository.ActorRecordRepository;
import ru.citeck.ecos.history.service.ActorRecordService;

@Service("actorRecordService")
public class ActorRecordServiceImpl implements ActorRecordService {

    private ActorRecordRepository actorRecordRepository;

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

    @Autowired
    public void setActorRecordRepository(ActorRecordRepository actorRecordRepository) {
        this.actorRecordRepository = actorRecordRepository;
    }
}
