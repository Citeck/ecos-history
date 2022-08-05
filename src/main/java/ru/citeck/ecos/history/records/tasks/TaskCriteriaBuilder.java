package ru.citeck.ecos.history.records.tasks;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.context.lib.auth.AuthContext;
import ru.citeck.ecos.history.domain.TaskRecordEntity;
import ru.citeck.ecos.history.repository.specifications.TaskSpecification;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.SortBy;
import ru.citeck.ecos.records2.request.query.page.SkipPage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class TaskCriteriaBuilder {

    private static final String CURRENT_USER = "$CURRENT";

    public Specification<TaskRecordEntity> buildSpecification(RecordsQuery recordsQuery) {
        return composeSpecifications(parseSpecifications(recordsQuery));
    }

    private List<Specification<TaskRecordEntity>> parseSpecifications(RecordsQuery recordsQuery) {
        ObjectData query = recordsQuery.getQuery(ObjectData.class);
        List<Specification<TaskRecordEntity>> searchSpecifications = new ArrayList<>();
        if (query.has("actor")) {
            DataValue actorNode = query.get("actor");
            if (actorNode.isArray()) {
                List<String> actors = getArrayActors(actorNode);
                searchSpecifications.add(TaskSpecification.hasActor(actors));
            } else if (actorNode.isTextual()) {
                String actor = actorNode.asText();
                if (CURRENT_USER.equals(actor)) {
                    List<String> currentUserAuthorities = AuthContext.getCurrentUserWithAuthorities();
                    if (CollectionUtils.isEmpty(currentUserAuthorities)) {
                        log.warn("For request not founded current user authorities");
                    } else {
                        searchSpecifications.add(TaskSpecification.hasActor(currentUserAuthorities));
                    }
                } else {
                    searchSpecifications.add(TaskSpecification.hasActor(Collections.singletonList(actor)));
                }
            }
        }

        if (query.has("active")) {
            DataValue activeNode = query.get("active");
            if (activeNode.isBoolean()) {
                searchSpecifications.add(TaskSpecification.isActive(activeNode.asBoolean()));
            } else if (activeNode.isTextual()) {
                String rawActive = activeNode.asText();
                if (StringUtils.isNotBlank(rawActive)) {
                    searchSpecifications.add(TaskSpecification.isActive(Boolean.parseBoolean(rawActive)));
                }
            } else {
                log.warn("Active not search criteria not parsed: " + activeNode.asText());
            }
        }

        if (query.has("docType")) {
            DataValue docTypeNode = query.get("docType");
            String documentType = docTypeNode.asText();
            if (StringUtils.isNotBlank(documentType)) {
                searchSpecifications.add(TaskSpecification.hasDocumentType(documentType));
            }
        }

        if (query.has("docStatus")) {
            DataValue docStatusNode = query.get("docStatus");
            String documentStatus = docStatusNode.asText();
            if (StringUtils.isNotBlank(documentStatus)) {
                searchSpecifications.add(TaskSpecification.hasDocumentStatus(documentStatus));
            }
        }

        if (query.has("document")) {
            String documentRef = query.get("document").asText();
            if (StringUtils.isNotBlank(documentRef)) {
                String documentId = documentRef;
                int alfrescoIdIndex = documentRef.indexOf("workspace://SpacesStore/");
                if (alfrescoIdIndex >= 0) {
                    int alfrescoIdLength = "workspace://SpacesStore/".length();
                    documentId = documentRef.substring(alfrescoIdIndex + alfrescoIdLength);
                }
                searchSpecifications.add(TaskSpecification.hasDocument(documentId));
            }
        }

        return searchSpecifications;
    }

    private List<String> getArrayActors(DataValue actorArrayNode) {
        List<String> result = new ArrayList<>();
        for (DataValue actorNode : actorArrayNode) {
            String actor = actorNode.asText();
            if (StringUtils.isNotBlank(actor)) {
                result.add(actor);
            }
        }
        return result;
    }

    private Specification<TaskRecordEntity> composeSpecifications(List<Specification<TaskRecordEntity>> specifications) {
        if (CollectionUtils.isEmpty(specifications)) {
            return null;
        }

        if (specifications.size() == 1) {
            return specifications.get(0);
        }

        Specification<TaskRecordEntity> resultSpecification = specifications.get(0);
        for (int i = 1; i < specifications.size(); i++) {
            resultSpecification = resultSpecification.and(specifications.get(i));
        }

        return resultSpecification;
    }

    public Pageable buildPageable(RecordsQuery query) {
        SkipPage skipPage = query.getSkipPage();

        int maxItems = skipPage.getMaxItems();
        if (maxItems < 1) {
            maxItems = 10;
        }

        int skipCount = skipPage.getSkipCount();
        int pageNumber = skipCount / maxItems;
        if (pageNumber < 0) {
            pageNumber = 0;
        }

        return PageRequest.of(pageNumber, maxItems, Sort.by(parseSortList(query)));
    }

    private List<Sort.Order> parseSortList(RecordsQuery query) {
        List<Sort.Order> sortResultList = new ArrayList<>();
        List<SortBy> sortByList = query.getSortBy();
        for (SortBy sortBy : sortByList) {
            switch (sortBy.getAttribute()) {
                case "cm:created":
                case "createdBy"://TODO: used for correct working of mobile app with old version. Remove in future.
                case "createdDate":
                    sortResultList.add(createOrder("createdDate", sortBy.isAscending()));
                    break;
                case "cm:modified":
                case "modifiedDate":
                    sortResultList.add(createOrder("lastModifiedDate", sortBy.isAscending()));
                    break;
                default:
                    log.warn("Received not supported sorting attribute: " + sortBy.getAttribute());
            }
        }

        if (CollectionUtils.isEmpty(sortResultList)) {
            log.info("No Sort parsed. Will be used default sorting by id");
            sortResultList.add(createOrder("id", false));
        }

        return sortResultList;
    }

    private Sort.Order createOrder(String attribute, boolean ascending) {
        if (ascending) {
            return Sort.Order.asc(attribute);
        } else {
            return Sort.Order.desc(attribute);
        }
    }
}
