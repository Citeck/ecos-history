package ru.citeck.ecos.history.converter.impl;

import org.springframework.stereotype.Service;
import ru.citeck.ecos.history.converter.Converter;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.dto.HistoryRecordDto;

import java.util.Date;

@Service("historyRecordConverter")
public class HistoryRecordConverter implements Converter<HistoryRecordEntity, HistoryRecordDto> {

    @Override
    public HistoryRecordDto convert(HistoryRecordEntity historyRecordEntity) {
        HistoryRecordDto result = new HistoryRecordDto();

        Long id = historyRecordEntity.getId();
        result.setUuid(id != null ? id.toString() : "no_uuid");

        result.setHistoryEventId(historyRecordEntity.getHistoryEventId());
        result.setUserId(historyRecordEntity.getUserId());
        result.setUsername(historyRecordEntity.getUsername());
        result.setComments(historyRecordEntity.getComments());
        result.setVersion(historyRecordEntity.getVersion());
        result.setEventType(historyRecordEntity.getEventType());

        Date creationTime = historyRecordEntity.getCreationTime();
        result.setCreationTime(creationTime != null ? creationTime.getTime() : 0L);

        result.setTaskRole(historyRecordEntity.getTaskRole());
        result.setTaskOutcome(historyRecordEntity.getTaskOutcome());
        result.setTaskDefinitionKey(historyRecordEntity.getTaskDefinitionKey());
        result.setTaskType(historyRecordEntity.getFullTaskType());
        result.setDocumentId(historyRecordEntity.getDocumentId());
        return result;
    }
}
