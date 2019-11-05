package ru.citeck.ecos.history.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.history.domain.ActorRecordEntity;
import ru.citeck.ecos.history.domain.TaskActorRecordEntity;
import ru.citeck.ecos.history.domain.TaskActorRecordEntityId;
import ru.citeck.ecos.history.domain.TaskRecordEntity;
import ru.citeck.ecos.history.repository.TaskActorRecordRepository;
import ru.citeck.ecos.history.service.TaskActorRecordService;

@Service("taskActorRecordService")
public class TaskActorRecordServiceImpl implements TaskActorRecordService {

    private TaskActorRecordRepository taskActorRecordRepository;

    @Override
    public TaskActorRecordEntity findOrCreateByEntities(TaskRecordEntity taskRecordEntity,
                                                        ActorRecordEntity actorRecordEntity) {
        Long taskId = taskRecordEntity.getId();
        Long actorId = actorRecordEntity.getId();

        TaskActorRecordEntityId id = new TaskActorRecordEntityId(taskId, actorId);

        TaskActorRecordEntity result = taskActorRecordRepository.findById(id).orElse(null);
        if (result == null) {
            TaskActorRecordEntity entity = new TaskActorRecordEntity();
            entity.setId(id);
            entity.setTask(taskRecordEntity);
            entity.setActor(actorRecordEntity);
            result = taskActorRecordRepository.save(entity);
        }

        return result;
    }

    @Autowired
    public void setTaskActorRecordRepository(TaskActorRecordRepository taskActorRecordRepository) {
        this.taskActorRecordRepository = taskActorRecordRepository;
    }
}
