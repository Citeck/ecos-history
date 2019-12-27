package ru.citeck.ecos.history.service.utils;

import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.domain.TaskRecordEntity;
import ru.citeck.ecos.history.dto.DocumentInfo;

public class TaskPopulateUtils {

    public static void populateWorkflowProps(TaskRecordEntity taskRecordEntity,
                                             HistoryRecordEntity historyRecordEntity) {

        taskRecordEntity.setDocumentId(historyRecordEntity.getDocumentId());
        taskRecordEntity.setWorkflowId(historyRecordEntity.getWorkflowInstanceId());
        taskRecordEntity.setFormKey(historyRecordEntity.getTaskFormKey());
        taskRecordEntity.setLastTaskComment(historyRecordEntity.getLastTaskComment());
    }

    public static void populateDocumentProps(TaskRecordEntity taskRecordEntity, DocumentInfo documentInfo) {
        if (documentInfo != null) {
            taskRecordEntity.setDocumentType(documentInfo.getDocumentType());
            taskRecordEntity.setDocumentStatusName(documentInfo.getStatusName());
            String documentStatusTitle = documentInfo.getStatusTitleEn() + "|" + documentInfo.getStatusTitleRu();
            taskRecordEntity.setDocumentStatusTitle(documentStatusTitle);
        }
    }

}
