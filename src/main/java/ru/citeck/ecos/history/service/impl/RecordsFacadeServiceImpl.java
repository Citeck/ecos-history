package ru.citeck.ecos.history.service.impl;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.events.data.dto.record.AttrChanged;
import ru.citeck.ecos.events.data.dto.record.RecordEventDto;
import ru.citeck.ecos.history.mongo.domain.Record;
import ru.citeck.ecos.history.service.RecordsFacadeService;

import java.util.List;

@Slf4j
@Service
public class RecordsFacadeServiceImpl implements RecordsFacadeService {

    private static final String RECORD_ATTR = "attr.";
    private static final String RECORD_EXTERNAL_ID = "ext_id";

    private final MongoTemplate mongoTemplate;

    public RecordsFacadeServiceImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Record save(@NonNull RecordEventDto dto) {
        List<AttrChanged> changes = dto.getChanges();

        Query query = new Query(Criteria.where(RECORD_EXTERNAL_ID).is(dto.getDocId()));
        Update update = new Update();
        Record result;

        for (AttrChanged attrChanged : changes) {
            String attr = attrChanged.getAttribute();
            if (StringUtils.isBlank(attr)) {
                throw new IllegalArgumentException("Attribute can not be empty, dto: " + dto);
            }

            update = update.set(RECORD_ATTR + attr, attrChanged.getNewValue());
        }

        result = mongoTemplate.findAndModify(query, update, new FindAndModifyOptions().returnNew(true)
            .upsert(true), Record.class);

        log.debug("Updated: " + result);

        return result;
    }
}
