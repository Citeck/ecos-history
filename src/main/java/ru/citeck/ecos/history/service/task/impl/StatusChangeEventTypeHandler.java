package ru.citeck.ecos.history.service.task.impl;

import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.domain.TaskRecordEntity;
import ru.citeck.ecos.history.service.task.AbstractTaskHistoryEventHandler;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.spring.RemoteRecordsUtils;

import java.util.List;
import java.util.Map;

@Service
public class StatusChangeEventTypeHandler extends AbstractTaskHistoryEventHandler {

    private static final String STATUS_CHANGE_TYPE = "status.changed";
    private static final String WORKSPACE_SPACES_STORE = "workspace://SpacesStore/";

    private RecordsService recordsService;

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

        DocumentStatus result = RemoteRecordsUtils.runAsSystem(() ->
            recordsService.getMeta(composeRecordRef(documentId), DocumentStatus.class));

        if (result == null) {
            return;
        }

        for (TaskRecordEntity taskRecordEntity : taskRecordEntities) {
            taskRecordEntity.setDocumentStatusName(result.statusName);
            taskRecordEntity.setDocumentStatusTitle(result.statusTitleEn + "|" + result.statusTitleRu);
            taskRecordRepository.save(taskRecordEntity);
        }
    }

    private RecordRef composeRecordRef(String documentId) {
        String id = WORKSPACE_SPACES_STORE + documentId;
        return RecordRef.create("alfresco", "", id);
    }

    @Autowired
    public void setRecordsService(RecordsService recordsService) {
        this.recordsService = recordsService;
    }

    @Data
    private static class DocumentStatus {
        private String id;

        @MetaAtt("icase:caseStatusAssoc.cm:name")
        private String statusName;

        @MetaAtt("icase:caseStatusAssoc.cm:title.ru")
        private String statusTitleRu;

        @MetaAtt("icase:caseStatusAssoc.cm:title.en")
        private String statusTitleEn;
    }
}
