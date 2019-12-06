package ru.citeck.ecos.history.records.facade;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.history.mongo.domain.Record;
import ru.citeck.ecos.history.service.RecordsFacadeService;
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

    private static final String ALFRESCO_PREFIX = "alfresco@";

    private final RecordsFacadeService facadeService;

    {
        setId(ID);
    }

    public FacadeRecords(RecordsFacadeService facadeService) {
        this.facadeService = facadeService;
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
            Record record = facadeService.getByExternalId(ALFRESCO_PREFIX + id);
            if (record != null) {
                result.add(new FacadeRecordMeta(record));
            }
        }

        return result;
    }
}
