package ru.citeck.ecos.history.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.citeck.ecos.history.domain.ActorRecordEntity;
import ru.citeck.ecos.history.repository.ActorRecordRepository;
import ru.citeck.ecos.history.service.impl.ActorServiceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ActorServiceTest {

    @Test
    public void findOrCreateActorByExistingName() {
        ActorServiceImpl actorRecordService = new ActorServiceImpl();
        actorRecordService.setActorRecordRepository(setupRepositoryForSearchByExistingName());

        ActorRecordEntity actor = actorRecordService.findOrCreateActorByName("TESK_ACTOR_NAME");
        assertEquals("TESK_ACTOR_NAME", actor.getActorName());
    }

    @Test
    public void findOrCreateActorByExistingNameWithError() {
        ActorServiceImpl actorRecordService = new ActorServiceImpl();
        actorRecordService.setActorRecordRepository(setupRepositoryForSearchByExistingName());

        assertThrows(IllegalArgumentException.class, () ->
            actorRecordService.findOrCreateActorByName("NOT_EXISTING_ACTOR_NAME")
        );
    }

    private ActorRecordRepository setupRepositoryForSearchByExistingName() {
        ActorRecordRepository repository = Mockito.mock(ActorRecordRepository.class);

        ActorRecordEntity t = new ActorRecordEntity();
        t.setActorName("TESK_ACTOR_NAME");

        Mockito.when(repository.findByActorNameIgnoreCase("TESK_ACTOR_NAME")).thenReturn(t);
        Mockito.when(repository.findByActorNameIgnoreCase("NOT_EXISTING_ACTOR_NAME")).thenReturn(null);
        Mockito.when(repository.save(Mockito.any())).thenThrow(IllegalArgumentException.class);
        return repository;
    }
}
