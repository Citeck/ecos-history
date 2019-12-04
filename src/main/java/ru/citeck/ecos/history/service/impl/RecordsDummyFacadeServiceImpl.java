package ru.citeck.ecos.history.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.events.data.dto.record.RecordEventDto;
import ru.citeck.ecos.history.mongo.domain.Record;
import ru.citeck.ecos.history.service.RecordsFacadeService;

@Profile("!facade")
@Slf4j
@Service
public class RecordsDummyFacadeServiceImpl implements RecordsFacadeService {
    @Override
    public Record save(RecordEventDto dto) {
        log.warn("Facade is not enabled, cannot save RecordEventDto:{}", dto.toString());
        return null;
    }

    @Override
    public Record getByExternalId(String externalId) {
        return null;
    }
}
