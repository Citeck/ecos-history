package ru.citeck.ecos.history.records.facade;

import lombok.NonNull;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;

import java.util.Map;

/**
 * @author Roman Makarskiy
 */
public class FacadeRecordAttrMeta implements MetaValue {

    private static final String ATTR_DISP = "_disp";
    private static final String ATTR_STR = "_str";

    private final Map<String, Object> attr;

    public FacadeRecordAttrMeta(@NonNull Map<String, Object> attr) {
        this.attr = attr;
    }

    @Override
    public String getString() {
        return (String) attr.get(ATTR_STR);
    }

    @Override
    public String getDisplayName() {
        return (String) attr.get(ATTR_DISP);
    }
}
