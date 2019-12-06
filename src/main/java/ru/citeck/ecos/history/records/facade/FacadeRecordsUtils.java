package ru.citeck.ecos.history.records.facade;

import org.apache.commons.collections4.CollectionUtils;
import org.bson.Document;

import java.util.*;

/**
 * @author Roman Makarskiy
 */
public class FacadeRecordsUtils {

    public static List<FacadeRecordAttrMeta> getAttrMetaFromDocuments(List<Document> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            return Collections.emptyList();
        }

        List<FacadeRecordAttrMeta> result = new ArrayList<>();

        for (Document d : documents) {
            Set<Map.Entry<String, Object>> entries = d.entrySet();
            Map<String, Object> map = new HashMap<>();
            entries.forEach(stringObjectEntry -> map.put(stringObjectEntry.getKey(), stringObjectEntry.getValue()));
            result.add(new FacadeRecordAttrMeta(map));
        }

        return result;
    }

}
