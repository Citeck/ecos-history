package ru.citeck.ecos.history.service.task;

import ru.citeck.ecos.history.domain.HistoryRecordEntity;

import java.util.Map;

public interface TaskHistoryEventHandler {

    void handle(HistoryRecordEntity historyRecord, Map<String, String> requestParams);

}
