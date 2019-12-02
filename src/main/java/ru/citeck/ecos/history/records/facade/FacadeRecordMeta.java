package ru.citeck.ecos.history.records.facade;

import lombok.NonNull;
import org.bson.Document;
import ru.citeck.ecos.history.mongo.domain.Record;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;

import java.util.List;
import java.util.Map;

/**
 * @author Roman Makarskiy
 */
public class FacadeRecordMeta implements MetaValue {

    private final Map facadeAtts;

    public FacadeRecordMeta(@NonNull Record facadeRecord) {
        this.facadeAtts = facadeRecord.getAttributes();
    }

    @Override
    public Object getAttribute(String name, MetaField field) {
        List<Document> attributes = getAttributeAsMongoDocument(name);
        return FacadeRecordsUtils.getAttrMetaFromDocuments(attributes);
    }

    @SuppressWarnings("unchecked")
    private List<Document> getAttributeAsMongoDocument(String name) {
        return (List<Document>) facadeAtts.get(name);
    }

}
