package ru.citeck.ecos.history.service.impl;

import org.apache.commons.collections4.IterableUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.domain.TaskRecordEntity;
import ru.citeck.ecos.history.repository.TaskRecordRepository;
import ru.citeck.ecos.history.service.TaskRecordService;
import ru.citeck.ecos.history.service.task.TaskHistoryEventHandler;
import ru.citeck.ecos.history.service.task.TaskHistoryEventHandlerRegistry;

import java.util.List;
import java.util.Map;

@Service("taskRecordService")
public class TaskRecordServiceImpl implements TaskRecordService {

    private TaskRecordRepository taskRecordRepository;
    private TaskHistoryEventHandlerRegistry eventHandlerRegistry;

    @Override
    public Page<TaskRecordEntity> findTasksBySpecification(Specification<TaskRecordEntity> specification,
                                                           Pageable pageable) {

        return taskRecordRepository.findAll(specification, pageable);
    }

    @Override
    public List<TaskRecordEntity> findTasksByTaskId(List<String> taskIds) {
        return IterableUtils.toList(taskRecordRepository.findAllByTaskIdIn(taskIds));
    }

    @Override
    public TaskRecordEntity findTaskByTaskId(String taskId) {
        return taskRecordRepository.getByTaskId(taskId);
    }

    @Override
    public void handleTaskFromHistoryRecord(HistoryRecordEntity historyRecord, Map<String, String> requestParams) {
        String eventType = historyRecord.getEventType();
        TaskHistoryEventHandler handler = eventHandlerRegistry.getHandler(eventType);
        if (handler == null) {
            return;
        }

        handler.handle(historyRecord, requestParams);
    }

    @Override
    public TaskRecordEntity save(TaskRecordEntity taskRecordEntity) {
        return taskRecordRepository.save(taskRecordEntity);
    }

    @Autowired
    public void setTaskRecordRepository(TaskRecordRepository taskRecordRepository) {
        this.taskRecordRepository = taskRecordRepository;
    }

    @Autowired
    public void setEventHandlerRegistry(TaskHistoryEventHandlerRegistry eventHandlerRegistry) {
        this.eventHandlerRegistry = eventHandlerRegistry;
    }

}
