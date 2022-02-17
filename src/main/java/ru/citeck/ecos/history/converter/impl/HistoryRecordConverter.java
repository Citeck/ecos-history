package ru.citeck.ecos.history.converter.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.history.converter.Converter;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.dto.HistoryRecordDto;

import java.lang.reflect.Field;
import java.time.ZoneId;
import java.util.*;

@Slf4j
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

        result.setTaskTitle(historyRecordEntity.getTaskTitle());
        result.setTaskRole(historyRecordEntity.getTaskRole());
        result.setTaskOutcome(historyRecordEntity.getTaskOutcome());
        result.setTaskOutcomeName(historyRecordEntity.getTaskOutcomeName());
        result.setTaskDefinitionKey(historyRecordEntity.getTaskDefinitionKey());
        result.setTaskType(historyRecordEntity.getFullTaskType());
        result.setDocumentId(historyRecordEntity.getDocumentId());
        return result;
    }

    public Map<String, String> toMap(HistoryRecordDto historyRecordDto){
        HashMap<String, String> propertyMap = new HashMap<>();
        Field[] fields = historyRecordDto.getClass().getDeclaredFields();
        for (Field property : fields) {
            property.setAccessible(true);
            Object objectValue = null;
            try {
                objectValue = property.get(historyRecordDto);
                propertyMap.put(property.getName(), objectValue!=null? String.valueOf(objectValue): null);
            } catch (IllegalArgumentException | IllegalAccessException exception) {
                log.error(String.format("Failed to get attribute '%s' for %s", property.getName(), historyRecordDto),
                    exception);
            }
        }
        return propertyMap;
    }
}
