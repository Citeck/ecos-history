package ru.citeck.ecos.history.service.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.domain.TaskRecordEntity;
import ru.citeck.ecos.history.dto.DocumentInfo;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.rest.RemoteRecordsUtils;

@Slf4j
@Component
public class TaskPopulateUtils {

    public static final String STATUS_TITLE_SEPARATOR = "|";

    private static final String WORKSPACE_SPACES_STORE = "workspace://SpacesStore/";

    private final RecordsService recordsService;

    public TaskPopulateUtils(RecordsService recordsService) {
        this.recordsService = recordsService;
    }

    public void populateWorkflowProps(TaskRecordEntity taskRecordEntity,
                                      HistoryRecordEntity historyRecordEntity) {

        taskRecordEntity.setDocumentId(historyRecordEntity.getDocumentId());
        taskRecordEntity.setWorkflowId(historyRecordEntity.getWorkflowInstanceId());
        taskRecordEntity.setFormKey(historyRecordEntity.getTaskFormKey());
        taskRecordEntity.setLastTaskComment(historyRecordEntity.getLastTaskComment());
    }

    public void populateDocumentProps(TaskRecordEntity taskRecordEntity, HistoryRecordEntity historyRecord) {
        DocumentInfo documentInfo = getDocumentInfo(historyRecord);
        fillDocProps(taskRecordEntity, historyRecord, documentInfo);
    }

    public void fillDocProps(TaskRecordEntity taskRecordEntity, HistoryRecordEntity historyRecord,
                             DocumentInfo documentInfo) {
        taskRecordEntity.setDocumentType(historyRecord.getDocType() != null
            ? historyRecord.getDocType()
            : documentInfo.getDocumentType());

        taskRecordEntity.setDocumentStatusName(historyRecord.getDocStatusName() != null
            ? historyRecord.getDocStatusName()
            : documentInfo.getStatusName());

        taskRecordEntity.setDocumentStatusTitle(historyRecord.getDocStatusTitle() != null
            ? historyRecord.getDocStatusTitle()
            : documentInfo.getStatusTitleEn() + STATUS_TITLE_SEPARATOR + documentInfo.getStatusTitleRu());
    }

    public DocumentInfo getDocumentInfo(HistoryRecordEntity historyRecord) {
        if (receivedDataFromLegacyAlfrescoSource(historyRecord)) {
            RecordRef documentRef = composeRecordRef(historyRecord.getDocumentId());

            log.warn("Remote request to alfresco, getting DocumentInfo, doc: {}", documentRef);

            return RemoteRecordsUtils.runAsSystem(() ->
                recordsService.getMeta(documentRef, DocumentInfo.class));
        }
        return new DocumentInfo();
    }

    private static boolean receivedDataFromLegacyAlfrescoSource(HistoryRecordEntity historyRecord) {
        RecordRef docRef = RecordRef.valueOf(historyRecord.getDocumentId());
        return docRef.appName.isEmpty() && docRef.getSourceId().isEmpty()
            && (historyRecord.getDocStatusName() == null || historyRecord.getDocType() == null);
    }

    public static RecordRef composeRecordRef(String documentId) {
        String id = WORKSPACE_SPACES_STORE + documentId;
        return RecordRef.create("alfresco", "", id);
    }

}
