package ru.citeck.ecos.history.records;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Sets;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.history.domain.TaskRecordEntity;
import ru.citeck.ecos.history.mongo.domain.Record;
import ru.citeck.ecos.history.records.facade.FacadeRecords;
import ru.citeck.ecos.history.records.facade.FacadeRecordsUtils;
import ru.citeck.ecos.history.records.tasks.TaskCriteriaBuilder;
import ru.citeck.ecos.history.service.RecordsFacadeService;
import ru.citeck.ecos.history.service.TaskRecordService;
import ru.citeck.ecos.records2.QueryContext;
import ru.citeck.ecos.records2.RecordConstants;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.InnerMetaValue;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.request.result.RecordsResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsMetaLocalDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsQueryWithMetaLocalDAO;
import ru.citeck.ecos.records2.spring.RemoteRecordsUtils;

import java.util.*;
import java.util.stream.Collectors;

import static ru.citeck.ecos.history.service.HistoryRecordService.EMPTY_VALUE_KEY;
import static ru.citeck.ecos.history.service.utils.TaskPopulateUtils.STATUS_TITLE_SEPARATOR;

@Slf4j
@Service
public class TaskRecords extends LocalRecordsDAO implements
    RecordsQueryWithMetaLocalDAO<MetaValue>,
    RecordsMetaLocalDAO<MetaValue> {

    private static final String ID = "tasks";
    private static final String TASK_RECORD_DATA_KEY = "taskRecordDataKey";

    private static final String ATT_STARTED = "started";
    private static final String ATT_ACTIVE = "active";
    private static final String ATT_FORM_KEY = "_formKey";
    private static final String ATT_LAST_COMMENT = "lastcomment";

    private static final String ATT_DOC_STATUS = "docStatus";
    private static final String ATT_DOC_TYPE = "docType";
    private static final String ATT_DOC_STATUS_TITLE = "docStatusTitle";
    private static final String ATT_DOC_DISPLAY_NAME = "docDisplayName";
    private static final String ATT_ECM_NODE_UUID = "_ECM_sys:node-uuid";

    private static final String ECM_DOCUMENT_FIELD_PREFIX = "_ECM_";

    private static final String ALFRESCO_SPACES_STORE_PREFIX = "alfresco@workspace://SpacesStore/";

    private static final Set<String> ATTRIBUTES_TO_RECEIVING_FROM_ALFRESCO = Sets.newHashSet(
        ATT_FORM_KEY,
        ATT_DOC_DISPLAY_NAME,
        ATT_DOC_STATUS_TITLE,
        ATT_LAST_COMMENT,
        "docSum",
        "sender",
        "dueDate",
        "assignee",
        "candidate",
        "actors",
        "title",
        "reassignable",
        "releasable",
        "claimable",
        "assignable",
        "comment"
    );

    private final TaskCriteriaBuilder taskCriteriaBuilder;
    private final TaskRecordService taskRecordService;
    private final RecordsFacadeService facadeService;

    @Autowired
    public TaskRecords(TaskCriteriaBuilder taskCriteriaBuilder,
                       TaskRecordService taskRecordService, RecordsFacadeService facadeService) {
        this.facadeService = facadeService;
        setId(ID);
        this.taskCriteriaBuilder = taskCriteriaBuilder;
        this.taskRecordService = taskRecordService;
    }

    @Override
    public RecordsQueryResult<MetaValue> getMetaValues(RecordsQuery recordsQuery) {
        Specification<TaskRecordEntity> searchSpecification = taskCriteriaBuilder.buildSpecification(recordsQuery);
        if (searchSpecification == null) {
            return new RecordsQueryResult<>();
        }

        Pageable searchPageable = taskCriteriaBuilder.buildPageable(recordsQuery);

        Page<TaskRecordEntity> taskRecordEntityPage = taskRecordService
            .findTasksBySpecification(searchSpecification, searchPageable);

        RecordsQueryResult<MetaValue> result = new RecordsQueryResult<>();
        result.setHasMore(taskRecordEntityPage.hasNext());
        result.setTotalCount(taskRecordEntityPage.getTotalElements());
        result.setRecords(taskRecordEntityPage.stream().map(Task::new).collect(Collectors.toList()));

        if (CollectionUtils.isEmpty(result.getRecords())) {
            return new RecordsQueryResult<>();
        }

        return result;
    }

    @Override
    public List<MetaValue> getMetaValues(List<RecordRef> list) {
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }

        List<String> taskIds = list.stream().map(RecordRef::getId).collect(Collectors.toList());
        List<TaskRecordEntity> entities = taskRecordService.findTasksByTaskId(taskIds);
        return entities.stream().map(Task::new).collect(Collectors.toList());
    }

    public class Task implements MetaValue {

        private TaskRecordEntity entity;
        private Record facadeDocument;

        private Task(TaskRecordEntity entity) {
            this.entity = entity;
            this.facadeDocument = facadeService.getByExternalId(ALFRESCO_SPACES_STORE_PREFIX
                + entity.getDocumentId());
        }

        @Override
        public <T extends QueryContext> void init(T context, MetaField field) {
            ContextData contextData = initializeAndGetContextData();
            Map facadeAtts = facadeDocument != null ? facadeDocument.getAttributes() : Collections.EMPTY_MAP;

            Map<String, String> attributesMap = field.getInnerAttributesMap();
            for (String att : attributesMap.keySet()) {
                String facadeAttribute = fixLegacyAttNameForFacade(att);
                if (facadeAtts.containsKey(facadeAttribute)) {
                    continue;
                }

                if (ATT_ECM_NODE_UUID.equals(att) && StringUtils.isNotBlank(entity.getDocumentId())) {
                    continue;
                }

                if (ATT_FORM_KEY.equals(att) && StringUtils.isNotBlank(entity.getFormKey())) {
                    continue;
                }

                if (ATT_LAST_COMMENT.equals(att) && StringUtils.isNotBlank(entity.getLastTaskComment())) {
                    continue;
                }

                if (ATT_DOC_STATUS_TITLE.equals(att) && StringUtils.isNotBlank(entity.getDocumentStatusTitle())) {
                    continue;
                }

                String attrSchema = attributesMap.get(att);
                if (ATTRIBUTES_TO_RECEIVING_FROM_ALFRESCO.contains(att)) {
                    printWarnRemoteAttributeAccess(att, attrSchema, entity);
                    contextData.attributesToRequest.put(att, attrSchema);
                    continue;
                }
                if (att.startsWith(ECM_DOCUMENT_FIELD_PREFIX)
                    && !facadeAtts.containsKey(facadeAttribute)) {
                    printWarnRemoteAttributeAccess(att, attrSchema, entity);
                    contextData.attributesToRequest.put(att, attrSchema);
                }
            }

            addTaskRefsIfNotExists(contextData);
        }

        private void addTaskRefsIfNotExists(ContextData contextData) {
            contextData.taskRefs.add(getAlfrescoWfTaskRemoteRecordRef());
        }

        @Override
        public String getId() {
            return entity.getTaskId();
        }

        @Override
        public Object getAttribute(String name, MetaField field) {
            RecordMeta recordMeta = getDocData(getAlfrescoWfTaskRemoteRecordRef());

            if (recordMeta.has(name)) {
                JsonNode node = recordMeta.get(name);
                if (node instanceof ArrayNode) {
                    List<InnerMetaValue> result = new ArrayList<>();
                    node.forEach(jsonNode -> result.add(new InnerMetaValue(jsonNode)));
                    return result;
                }

                return new InnerMetaValue(node);
            }

            switch (name) {
                case ATT_STARTED:
                    return entity.getStartEventDate();
                case ATT_ACTIVE:
                    return entity.getCompleteEventDate() == null;
                case ATT_FORM_KEY:
                    return entity.getFormKey();
                case ATT_DOC_STATUS:
                    JsonNode statusStr = recordsService.getAttribute(RecordRef.create("", FacadeRecords.ID,
                        "workspace://SpacesStore/" + entity.getDocumentId()), "caseStatus{.str}");
                    String status = statusStr != null && StringUtils.isNotBlank(statusStr.asText())
                        ? statusStr.asText() : entity.getDocumentStatusName();
                    return EMPTY_VALUE_KEY.equals(status) ? null : status;
                case ATT_DOC_TYPE:
                    String documentType = entity.getDocumentType();
                    if (EMPTY_VALUE_KEY.equals(documentType)) {
                        return null;
                    }

                    if (StringUtils.isNotBlank(documentType)) {
                        return documentType;
                    }
                case ATT_ECM_NODE_UUID:
                    return entity.getDocumentId();
                case ATT_LAST_COMMENT:
                    String lastTaskComment = entity.getLastTaskComment();
                    return EMPTY_VALUE_KEY.equals(lastTaskComment) ? null : lastTaskComment;
                case ATT_DOC_STATUS_TITLE:
                    String statusTitle = entity.getDocumentStatusTitle();
                    if (EMPTY_VALUE_KEY.equals(statusTitle)) {
                        return null;
                    }

                    //TODO: remove hard coded returning RU status title
                    String[] titles = StringUtils.split(statusTitle, STATUS_TITLE_SEPARATOR);
                    if (titles.length == 0) {
                        return null;
                    }

                    if (titles.length > 1) {
                        return titles[1];
                    }

                    return titles[0];
            }

            String facadeAttr = fixLegacyAttNameForFacade(name);
            List<Document> attributes = getAttributeAsMongoDocument(facadeAttr);
            return FacadeRecordsUtils.getAttrMetaFromDocuments(attributes);
        }

        private RecordRef getAlfrescoWfTaskRemoteRecordRef() {
            return RecordRef.create("alfresco", "wftask", getId());
        }

        private String fixLegacyAttNameForFacade(String name) {
            if (StringUtils.startsWith(name, ECM_DOCUMENT_FIELD_PREFIX)) {
                return StringUtils.remove(name, ECM_DOCUMENT_FIELD_PREFIX);
            }

            if (ATT_DOC_TYPE.equals(name)) {
                return name.replace(ATT_DOC_TYPE, RecordConstants.ATT_TYPE);
            }

            if (StringUtils.startsWith(name, ATT_DOC_STATUS_TITLE)) {
                return name.replace(ATT_DOC_STATUS_TITLE, "caseStatus");
            }

            if (ATT_DOC_DISPLAY_NAME.equals(name)) {
                return "cm:title";
            }

            return name;
        }

        @SuppressWarnings("unchecked")
        private List<Document> getAttributeAsMongoDocument(String name) {
            if (facadeDocument == null) {
                return Collections.emptyList();
            }
            return (List<Document>) facadeDocument.getAttributes().get(name);
        }

        private RecordMeta getDocData(RecordRef ref) {
            ContextData contextData = initializeAndGetContextData();
            if (MapUtils.isEmpty(contextData.result)) {
                log.debug("Request attr from alfresco: {}", contextData.attributesToRequest);

                //TODO: fix runAsSystem
                RecordsResult<RecordMeta> attributes = RemoteRecordsUtils.runAsSystem(() ->
                    recordsService.getRawAttributes(contextData.taskRefs, contextData.attributesToRequest));
                attributes.getRecords().forEach(r -> contextData.result.put(r.getId(), r));
            }
            return contextData.result.get(ref);
        }

        private ContextData initializeAndGetContextData() {
            QueryContext current = QueryContext.getCurrent();
            ContextData contextData;
            if (current.hasData(TASK_RECORD_DATA_KEY)) {
                contextData = current.getData(TASK_RECORD_DATA_KEY);
            } else {
                contextData = new ContextData();
                contextData.attributesToRequest = new HashMap<>();
                contextData.taskRefs = new HashSet<>();
                contextData.result = new HashMap<>();
                current.putData(TASK_RECORD_DATA_KEY, contextData);
            }
            return contextData;
        }
    }

    @Data
    private static class ContextData {
        private Map<String, String> attributesToRequest;
        private Set<RecordRef> taskRefs;
        private Map<RecordRef, RecordMeta> result;
    }

    private void printWarnRemoteAttributeAccess(String attr, String schema, TaskRecordEntity entity) {
        log.warn("Remote access attribute from alfresco - taskId:{}, docId:{}, key:{}, value:{}", entity.getTaskId(),
            entity.getDocumentId(), attr, schema);
    }

}
