package ru.citeck.ecos.history.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.domain.TaskRecordEntity;

import java.util.List;
import java.util.Map;

public interface TaskRecordService {

    Page<TaskRecordEntity> findTasksBySpecification(Specification<TaskRecordEntity> specification, Pageable pageable);

    List<TaskRecordEntity> findTasksByTaskId(List<String> taskIds);

    void handleTaskFromHistoryRecord(HistoryRecordEntity historyRecord, Map<String, String> requestParams);

    <T> T getTaskInfo(String taskId, Class<T> infoClass);

}
