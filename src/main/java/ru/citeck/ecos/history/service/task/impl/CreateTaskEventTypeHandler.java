package ru.citeck.ecos.history.service.task.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.FastDateFormat;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.domain.TaskRecordEntity;
import ru.citeck.ecos.history.service.HistoryRecordService;
import ru.citeck.ecos.history.service.task.AbstractTaskHistoryEventHandler;
import ru.citeck.ecos.history.service.utils.TaskPopulateUtils;

import java.text.ParseException;
import java.util.Map;

@Slf4j
@Service
public class CreateTaskEventTypeHandler extends AbstractTaskHistoryEventHandler {

    private static final FastDateFormat dateFormat = FastDateFormat.getInstance("dd.MM.yyyy HH:mm:ss");
    private static final String CREATE_TASK_TYPE = "task.create";

    private final TaskPopulateUtils taskPopulateUtils;

    public CreateTaskEventTypeHandler(TaskPopulateUtils taskPopulateUtils) {
        this.taskPopulateUtils = taskPopulateUtils;
    }

    @Override
    public String getEventType() {
        return CREATE_TASK_TYPE;
    }

    @Override
    public void handle(HistoryRecordEntity historyRecord, Map<String, String> requestParams) {
        TaskRecordEntity taskRecordEntity = getOrCreateTask(historyRecord);
        if (taskRecordEntity == null) {
            return;
        }

        taskPopulateUtils.populateWorkflowProps(taskRecordEntity, historyRecord);
        taskPopulateUtils.populateDocumentProps(taskRecordEntity, historyRecord);

        taskRecordEntity.setStartEvent(historyRecord);
        taskRecordEntity.setStartEventDate(historyRecord.getCreationTime());

        taskRecordEntity.setAssignee(historyRecord.getInitiator());
        taskRecordEntity.setAssigneeManager(requestParams.get(HistoryRecordService.TASK_ASSIGNEE_MANAGER));

        String dueDate = requestParams.get(HistoryRecordService.TASK_DUE_DATE);
        if (dueDate != null) {
            try {
                taskRecordEntity.setDueDate(dateFormat.parse(dueDate));
            } catch (ParseException e) {
                log.error("Can't parse due date: " + dueDate);
            }
        }

        taskRecordRepository.save(taskRecordEntity);
    }

}
