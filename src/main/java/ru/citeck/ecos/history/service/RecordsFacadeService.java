package ru.citeck.ecos.history.service;

import ru.citeck.ecos.events.data.dto.record.RecordEventDto;
import ru.citeck.ecos.history.mongo.domain.Record;

public interface RecordsFacadeService {

    Record save(RecordEventDto dto);

    Record getByExternalId(String externalId);

}
