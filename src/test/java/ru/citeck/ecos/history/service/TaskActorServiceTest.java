package ru.citeck.ecos.history.service;

import org.junit.Test;
import ru.citeck.ecos.history.domain.ActorRecordEntity;
import ru.citeck.ecos.history.domain.TaskActorRecordEntity;
import ru.citeck.ecos.history.domain.TaskActorRecordEntityId;
import ru.citeck.ecos.history.domain.TaskRecordEntity;
import ru.citeck.ecos.history.repository.TaskActorRecordRepository;
import ru.citeck.ecos.history.service.impl.TaskActorRecordServiceImpl;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TaskActorServiceTest {

    private final TaskActorRecordEntityId existingId = new TaskActorRecordEntityId(1L, 2L);
    private final TaskActorRecordEntityId notExistingId = new TaskActorRecordEntityId(4L, 5L);

    @Test
    public void findOrCreateActorByExistingName() {
        TaskActorRecordServiceImpl taskActorRecordService = new TaskActorRecordServiceImpl();
        taskActorRecordService.setTaskActorRecordRepository(setupRepositoryForSearchByExistingName());

        TaskRecordEntity task = new TaskRecordEntity();
        task.setId(existingId.getTaskRecordsId());

        ActorRecordEntity actor = new ActorRecordEntity();
        actor.setId(existingId.getActorRecordsId());

        TaskActorRecordEntity taskActor = taskActorRecordService.findOrCreateByEntities(task, actor);
        assertEquals(taskActor.getId(), existingId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void findOrCreateActorByNotExistingName() {
        TaskActorRecordServiceImpl taskActorRecordService = new TaskActorRecordServiceImpl();
        taskActorRecordService.setTaskActorRecordRepository(setupRepositoryForSearchByExistingName());

        TaskRecordEntity task = new TaskRecordEntity();
        task.setId(notExistingId.getTaskRecordsId());

        ActorRecordEntity actor = new ActorRecordEntity();
        actor.setId(notExistingId.getActorRecordsId());

        taskActorRecordService.findOrCreateByEntities(task, actor);
    }

    private TaskActorRecordRepository setupRepositoryForSearchByExistingName() {
        TaskActorRecordRepository repository = mock(TaskActorRecordRepository.class);

        when(repository.findById(any())).then(
            invocation -> {
                TaskActorRecordEntityId id = invocation.getArgument(0);
                if (id.getTaskRecordsId() == existingId.getTaskRecordsId()
                    && id.getActorRecordsId() == existingId.getActorRecordsId()) {

                    TaskActorRecordEntity result = new TaskActorRecordEntity();
                    result.setId(existingId);
                    return Optional.of(result);
                }
                return Optional.empty();
            }
        );

        when(repository.save(any())).thenThrow(IllegalArgumentException.class);

        return repository;
    }
}
