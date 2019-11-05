package ru.citeck.ecos.history.service.task;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.domain.TaskRecordEntity;
import ru.citeck.ecos.history.repository.TaskRecordRepository;

import javax.annotation.PostConstruct;

public abstract class AbstractTaskHistoryEventHandler implements TaskHistoryEventHandler {

    protected TaskHistoryEventHandlerRegistry registry;
    protected TaskRecordRepository taskRecordRepository;

    @PostConstruct
    public void init() {
        registry.register(this);
    }

    public abstract String getEventType();

    protected TaskRecordEntity getOrCreateTask(HistoryRecordEntity historyRecordEntity) {
        String taskId = historyRecordEntity.getTaskEventInstanceId();
        if (StringUtils.isBlank(taskId)) {
            return null;
        }

        TaskRecordEntity task = taskRecordRepository.getByTaskId(taskId);
        if (task == null) {
            task = new TaskRecordEntity();
            task.setTaskId(taskId);
            task = taskRecordRepository.save(task);
        }
        return task;
    }

    @Autowired
    public void setRegistry(TaskHistoryEventHandlerRegistry registry) {
        this.registry = registry;
    }

    @Autowired
    public void setTaskRecordRepository(TaskRecordRepository taskRecordRepository) {
        this.taskRecordRepository = taskRecordRepository;
    }
}
