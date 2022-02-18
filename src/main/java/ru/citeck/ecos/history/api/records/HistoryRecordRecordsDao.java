package ru.citeck.ecos.history.api.records;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.dto.HistoryRecordDto;
import ru.citeck.ecos.history.service.HistoryRecordService;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao;
import ru.citeck.ecos.records3.record.dao.delete.DelStatus;
import ru.citeck.ecos.records3.record.dao.delete.RecordDeleteDao;
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDtoDao;
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao;
import ru.citeck.ecos.records3.record.dao.query.dto.query.QueryPage;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class HistoryRecordRecordsDao extends AbstractRecordsDao implements RecordAttsDao,
    RecordsQueryDao,
    RecordMutateDtoDao<HistoryRecordDto>,
    RecordDeleteDao {

    public static final String ID = "history-record";
    private final HistoryRecordService historyRecordService;

    @Autowired
    public HistoryRecordRecordsDao(HistoryRecordService service) {
        this.historyRecordService = service;
    }

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @Nullable
    @Override
    public HistoryRecordDto getRecordAtts(@NotNull String localHistoryRecordId) {
        if (localHistoryRecordId.isEmpty()) {
            log.warn("Failed to get history record attributes - local ID was not defined");
            return null;
        } else {
            HistoryRecordDto historyRecord = historyRecordService.getHistoryRecordById(localHistoryRecordId);
            if (historyRecord == null) {
                log.warn("History record with ID {} was not found", localHistoryRecordId);
                return null;
            }
            return historyRecord;
        }
    }

    @Nullable
    @Override
    public RecsQueryRes<HistoryRecordDto> queryRecords(@NotNull RecordsQuery recordsQuery) {
        RecsQueryRes<HistoryRecordDto> result = new RecsQueryRes<>();
        List<Sort.Order> sorts = recordsQuery.getSortBy()
            .stream()
            .map(sortBy -> {
                String attribute = sortBy.getAttribute();
                if (StringUtils.isNotBlank(attribute)) {
                    return Optional.of(sortBy.getAscending() ? Sort.Order.asc(attribute) : Sort.Order.desc(attribute));
                }
                return Optional.<Sort.Order>empty();
            })
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
        Sort sort = sorts.isEmpty() ? null : Sort.by(sorts);
        final QueryPage page = recordsQuery.getPage();
        int maxItemsCount = page.getMaxItems() <= 0 ? 10000 : page.getMaxItems();
        int skipCount = page.getSkipCount();

        if (PredicateService.LANGUAGE_PREDICATE.equals(recordsQuery.getLanguage())) {
            Predicate predicate = recordsQuery.getQuery(Predicate.class);
            List<HistoryRecordDto> historyRecordDtoList =
                historyRecordService.getAll(maxItemsCount, skipCount, predicate, sort);
            result.setRecords(historyRecordDtoList);
            result.setTotalCount(historyRecordDtoList.size());
        } else {
            log.warn("Unsupported query language '{}'", recordsQuery.getLanguage());
            result.setRecords(Collections.emptyList());
            result.setTotalCount(0);
        }

        return result;
    }

    @Override
    public HistoryRecordDto getRecToMutate(@NotNull String localHistoryRecordId) {
        HistoryRecordDto historyRecord = historyRecordService.getHistoryRecordById(localHistoryRecordId);
        return historyRecord == null? new HistoryRecordDto(): new HistoryRecordDto(historyRecord);
    }

    @NotNull
    @Override
    public String saveMutatedRec(HistoryRecordDto historyRecordDto) {
        HistoryRecordEntity entity =
            historyRecordService.saveOrUpdateRecord(historyRecordDto);
        return String.valueOf(entity.getId());
    }

    @NotNull
    @Override
    public DelStatus delete(@NotNull String localHistoryRecordId) {
        if (StringUtils.isNotBlank(localHistoryRecordId)) {
            Long entityId = null;
            try {
                entityId = Long.valueOf(localHistoryRecordId);
                historyRecordService.delete(entityId);
                return DelStatus.OK;
            } catch (NumberFormatException e) {
                log.error("Failed to delete history record {}", localHistoryRecordId, e);
                return DelStatus.ERROR;
            }
        }
        return DelStatus.NOT_SUPPORTED;
    }
}
