package ru.citeck.ecos.history.records;

import com.google.common.collect.Sets;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.history.domain.TaskRecordEntity;
import ru.citeck.ecos.history.records.tasks.TaskCriteriaBuilder;
import ru.citeck.ecos.history.service.TaskRecordService;
import ru.citeck.ecos.records2.QueryContext;
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

@Service
public class TaskRecords extends LocalRecordsDAO implements
    RecordsQueryWithMetaLocalDAO<MetaValue>,
    RecordsMetaLocalDAO<MetaValue> {

    private static final String ID = "tasks";
    private static final String TASK_RECORD_DATA_KEY = "taskRecordDataKey";

    private static final String ATT_DOC_STATUS = "docStatus";
    private static final String ATT_DOC_TYPE = "docType";
    private static final String ATT_STARTED = "started";
    private static final String ATT_ACTIVE = "active";

    private static final String DOCUMENT_FIELD_PREFIX = "_ECM_";

    private static final Set<String> ATTRIBUTES_TO_RECEIVING_FROM_ALFRESCO = Sets.newHashSet(
        "docSum",
        "docDisplayName",
        "docStatusTitle",
        "sender",
        "dueDate",
        "assignee",
        "candidate",
        "actors",
        "lastcomment",
        "title",
        "reassignable",
        "releasable",
        "claimable",
        "assignable",
        "comment",
        "_formKey"
    );

    private final TaskCriteriaBuilder taskCriteriaBuilder;
    private final TaskRecordService taskRecordService;

    @Autowired
    public TaskRecords(TaskCriteriaBuilder taskCriteriaBuilder,
                       TaskRecordService taskRecordService) {
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

        private Task(TaskRecordEntity entity) {
            this.entity = entity;
        }

        @Override
        public <T extends QueryContext> void init(T context, MetaField field) {
            ContextData contextData = initializeAndGetContextData();

            Map<String, String> attributesMap = field.getInnerAttributesMap();
            for (String att : attributesMap.keySet()) {
                if (ATTRIBUTES_TO_RECEIVING_FROM_ALFRESCO.contains(att)) {
                    contextData.attributesToRequest.put(att, att);
                    continue;
                }
                if (att.startsWith(DOCUMENT_FIELD_PREFIX)) {
                    contextData.attributesToRequest.put(att, att);
                }
            }

            addTaskRefsIfNotExists(contextData);
        }

        private void addTaskRefsIfNotExists(ContextData contextData) {
            contextData.taskRefs.add(getRemoteRecordRef());
        }

        @Override
        public String getId() {
            return entity.getTaskId();
        }

        @Override
        public Object getAttribute(String name, MetaField field) {
            RecordMeta recordMeta = getDocData(getRemoteRecordRef());

            if (recordMeta.has(name)) {
                return new InnerMetaValue(recordMeta.get(name));
            }

            switch (name) {
                case ATT_DOC_STATUS:
                    return entity.getDocumentStatusName();
                case ATT_DOC_TYPE:
                    return entity.getDocumentType();
                case ATT_STARTED:
                    return entity.getStartEventDate();
                case ATT_ACTIVE:
                    return entity.getCompleteEventDate() == null;
            }

            return null;
        }

        private RecordRef getRemoteRecordRef() {
            return RecordRef.create("alfresco", "wftask", getId());
        }

        private RecordMeta getDocData(RecordRef ref) {
            ContextData contextData = initializeAndGetContextData();
            if (MapUtils.isEmpty(contextData.result)) {
                RecordsResult<RecordMeta> attributes = RemoteRecordsUtils.runAsSystem(() ->
                    recordsService.getAttributes(contextData.taskRefs, contextData.attributesToRequest));
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

}