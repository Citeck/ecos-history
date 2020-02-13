package ru.citeck.ecos.history.service.task.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.domain.TaskRecordEntity;
import ru.citeck.ecos.history.dto.DocumentInfo;
import ru.citeck.ecos.history.service.task.AbstractTaskHistoryEventHandler;
import ru.citeck.ecos.history.service.utils.TaskPopulateUtils;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class StatusChangeEventTypeHandler extends AbstractTaskHistoryEventHandler {

    private static final String STATUS_CHANGE_TYPE = "status.changed";

    private final TaskPopulateUtils taskPopulateUtils;

    public StatusChangeEventTypeHandler(TaskPopulateUtils taskPopulateUtils) {
        this.taskPopulateUtils = taskPopulateUtils;
    }

    @Override
    public String getEventType() {
        return STATUS_CHANGE_TYPE;
    }

    @Override
    public void handle(HistoryRecordEntity historyRecord, Map<String, String> requestParams) {
        String documentId = historyRecord.getDocumentId();
        if (StringUtils.isBlank(documentId)) {
            return;
        }

        List<TaskRecordEntity> taskRecordEntities = taskRecordRepository.getByDocumentId(documentId);
        if (CollectionUtils.isEmpty(taskRecordEntities)) {
            return;
        }

        DocumentInfo documentInfo = taskPopulateUtils.getDocumentInfo(historyRecord);

        for (TaskRecordEntity taskRecordEntity : taskRecordEntities) {
            taskPopulateUtils.fillDocProps(taskRecordEntity, historyRecord, documentInfo);
            taskRecordRepository.save(taskRecordEntity);
        }
    }

}
