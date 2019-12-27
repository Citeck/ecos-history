package ru.citeck.ecos.history.dto;

import lombok.Data;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;

@Data
public class DocumentInfo {
    private String id;

    @MetaAtt("_type")
    private String documentType;

    @MetaAtt("icase:caseStatusAssoc.cm:name")
    private String statusName;

    @MetaAtt("icase:caseStatusAssoc.cm:title.ru")
    private String statusTitleRu;

    @MetaAtt("icase:caseStatusAssoc.cm:title.en")
    private String statusTitleEn;
}
