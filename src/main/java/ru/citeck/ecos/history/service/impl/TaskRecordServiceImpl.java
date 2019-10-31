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
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.spring.RemoteRecordsUtils;

import java.util.List;
import java.util.Map;

@Service("taskRecordService")
public class TaskRecordServiceImpl implements TaskRecordService {

    private RecordsService recordsService;
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
    public void handleTaskFromHistoryRecord(HistoryRecordEntity historyRecord, Map<String, String> requestParams) {
        String eventType = historyRecord.getEventType();
        TaskHistoryEventHandler handler = eventHandlerRegistry.getHandler(eventType);
        if (handler == null) {
            return;
        }

        handler.handle(historyRecord, requestParams);
    }

    @Override
    public <T> T getTaskInfo(String taskId, Class<T> infoClass) {
        return RemoteRecordsUtils.runAsSystem(() ->
            recordsService.getMeta(composeTaskRecordRef(taskId), infoClass));
    }

    private RecordRef composeTaskRecordRef(String taskId) {
        return RecordRef.create("alfresco", "wftask", taskId);
    }

    @Autowired
    public void setRecordsService(RecordsService recordsService) {
        this.recordsService = recordsService;
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
