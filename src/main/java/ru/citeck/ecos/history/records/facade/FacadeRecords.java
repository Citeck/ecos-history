package ru.citeck.ecos.history.records.facade;

import org.apache.commons.lang.StringUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.history.mongo.domain.Record;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsMetaLocalDAO;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Roman Makarskiy
 */
@Component
public class FacadeRecords extends LocalRecordsDAO implements RecordsMetaLocalDAO<FacadeRecordMeta> {

    public static final String ID = "facade";

    private final MongoTemplate mongoTemplate;

    {
        setId(ID);
    }

    public FacadeRecords(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<FacadeRecordMeta> getMetaValues(List<RecordRef> list) {
        List<FacadeRecordMeta> result = new ArrayList<>();

        for (RecordRef recordRef : list) {
            String id = recordRef.getId();
            if (StringUtils.isBlank(id)) {
                continue;
            }

            //TODO: fix explicit set alfresco@
            Query query = new Query(Criteria.where("ext_id").is("alfresco@" + id));
            Record one = mongoTemplate.findOne(query, Record.class);

            result.add(new FacadeRecordMeta(one));
        }

        return result;
    }
}
