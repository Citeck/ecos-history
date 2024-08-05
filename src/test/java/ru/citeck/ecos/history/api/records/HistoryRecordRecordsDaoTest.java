package ru.citeck.ecos.history.api.records;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.history.HistoryApp;
import ru.citeck.ecos.history.converter.HistoryRecordConverter;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.dto.HistoryRecordDto;
import ru.citeck.ecos.history.service.HistoryRecordService;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.predicate.model.Predicates;
import ru.citeck.ecos.records2.predicate.model.ValuePredicate;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes;
import ru.citeck.ecos.webapp.api.entity.EntityRef;
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(EcosSpringExtension.class)
@SpringBootTest(classes = HistoryApp.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class HistoryRecordRecordsDaoTest {

    static String STR = "?str";

    @Autowired
    private RecordsService recordsService;
    @Autowired
    private HistoryRecordService service;
    @Autowired
    private HistoryRecordConverter historyRecordConverter;

    private boolean isRecExists(Object ref) {
        return !recordsService.getAtt(EntityRef.valueOf(ref), "_notExists?bool").asBoolean();
    }

    @Test
    public void createHistoryRecord() {

        EntityRef mutRes = recordsService.mutate(
            HistoryRecordTestData.getEmptyId(),
            getHistoryRecordAttributes(HistoryRecordTestData.getTestHistoryRecord())
        );
        assertThat(mutRes.isEmpty()).isFalse();
        assertThat(isRecExists(HistoryRecordTestData.getEmptyId() + "test")).isFalse();
        assertThat(isRecExists(mutRes)).isTrue();
    }

    @Test
    public void queryEqualByUsername() {

        propagateTestHistoryRecord();

        RecsQueryRes<EntityRef> result = recordsService.query(
            getQuery(
                Predicates.eq(
                    HistoryRecordEntity.USERNAME,
                    HistoryRecordTestData
                        .getTestHistoryRecord()
                        .getUsername())
            ).build()
        );
        assertThat(result.getRecords().size()).isEqualTo(1);
    }

    @Test
    public void queryEqualByCreationTime() {

        propagateTestHistoryRecord();

        RecsQueryRes<EntityRef> result = recordsService.query(
            getQuery(
                Predicates.eq(
                    HistoryRecordEntity.CREATION_TIME,
                    HistoryRecordTestData
                        .getTestHistoryRecord()
                        .getCreationTime())
            ).build()
        );
        assertThat(result.getRecords().size()).isEqualTo(1);
    }

    @Test
    public void queryGtByCreationTime() {
        propagateTestHistoryRecord();

        RecsQueryRes<EntityRef> result = recordsService.query(
            getQuery(
                Predicates.gt(
                    HistoryRecordEntity.CREATION_TIME,
                    HistoryRecordTestData.getTestHistoryRecord().getCreationTime()
                )
            ).build()
        );
        assertThat(result.getRecords().size()).isEqualTo(3);
    }

    @Test
    public void queryLtByCreationTime() {

        List<HistoryRecordEntity> entities = propagateTestHistoryRecord();
        int last = entities.size() - 1;

        RecsQueryRes<EntityRef> result = recordsService.query(
            getQuery(
                ValuePredicate.lt(
                    HistoryRecordEntity.CREATION_TIME,
                    String.valueOf(entities.get(last).getCreationTime().getTime())
                )
            ).build()
        );
        assertThat(result.getRecords().size()).isEqualTo(3);
    }

    @Test
    public void queryGtByUsernameAndCreationTime() {

        propagateTestHistoryRecord();

        RecsQueryRes<EntityRef> result = recordsService.query(
            getQuery(
                Predicates.and(
                    Predicates.eq(HistoryRecordEntity.USERNAME, HistoryRecordTestData.ADMIN),
                    Predicates.gt(
                        HistoryRecordEntity.CREATION_TIME,
                        HistoryRecordTestData.PROP_CREATETIME_VALUE
                    )
                )

            ).build()
        );
        assertThat(result.getRecords().size()).isEqualTo(3);
    }

    @Test
    public void queryOrByUsername() {

        propagateTestHistoryRecord();

        RecsQueryRes<EntityRef> result = recordsService.query(
            getQuery(
                Predicates.or(
                    Predicates.eq(HistoryRecordEntity.USERNAME, HistoryRecordTestData.ADMIN),
                    Predicates.eq(
                        HistoryRecordEntity.USERNAME,
                        HistoryRecordTestData.getTestHistoryRecord().getUsername()
                    )
                )
            ).build()
        );
        assertThat(result.getRecords().size()).isEqualTo(4);
    }

    @Test
    public void queryPeriodByCreationTime() {

        List<HistoryRecordEntity> entities = propagateTestHistoryRecord();

        RecsQueryRes<EntityRef> result = recordsService.query(
            getQuery(
                Predicates.and(
                    ValuePredicate.gt(HistoryRecordEntity.CREATION_TIME, String.valueOf(HistoryRecordTestData.getTestHistoryRecord().getCreationTime())),
                    ValuePredicate.lt(HistoryRecordEntity.CREATION_TIME, String.valueOf(entities.get(entities.size() - 1).getCreationTime().getTime()))
                )
            ).build()
        );
        assertThat(result.getRecords().size()).isEqualTo(2);
    }

    @Test
    public void queryStartsByComment() {

        propagateTestHistoryRecord();

        RecsQueryRes<EntityRef> result = recordsService.query(
            getQuery(
                Predicates.like(HistoryRecordEntity.COMMENTS, "Some%")
            ).build()
        );
        assertThat(result.getRecords().size()).isEqualTo(3);
    }

    @Test
    public void queryEqualByUsernameAndEventType() throws Exception {

        propagateTestHistoryRecord();

        RecsQueryRes<EntityRef> result = recordsService.query(
            getQuery(
                Predicates.and(
                    Predicates.eq(HistoryRecordEntity.USERNAME, HistoryRecordTestData.ADMIN),
                    Predicates.eq(HistoryRecordEntity.EVENT_TYPE, HistoryRecordTestData.RECORD_CHANGED_EVENT_TYPE)
                )
            ).build()
        );
        assertThat(result.getRecords().size()).isEqualTo(3);
    }

    @Test
    public void updateRecord() {

        HistoryRecordEntity recordEntity = createTestHistoryRecord();
        String comment = "Updated test comment";

        RecordAtts toMutate = new RecordAtts(
            EntityRef.valueOf(
                HistoryRecordTestData.getEmptyId() + recordEntity.getId()
            )
        );
        toMutate.setAttributes(ObjectData.create()
            .set("historyEventId", recordEntity.getId())
            .set("documentId", "test/doc@123")
            .set("creationTime", System.currentTimeMillis())
            .set("eventType", "created")
            .set("userId", "admin")
            .set("username", "admin")
            .set(HistoryRecordEntity.COMMENTS, comment)
        );
        recordsService.mutate(toMutate);

        String recId = HistoryRecordTestData.getEmptyId() + recordEntity.getId();
        DataValue attsToLoad = getAttributesJsonArray();
        RecordAtts atts = recordsService.getAtts(recId, attsToLoad.toStrList());

        assertThat(atts.get(HistoryRecordEntity.COMMENTS + STR).asText()).isEqualTo(comment);
    }

    @SneakyThrows
    private HistoryRecordEntity createTestHistoryRecord() {
        HistoryRecordDto recordDto = HistoryRecordTestData.getTestHistoryRecord();
        return service.saveOrUpdateRecord(recordDto);
    }

    @SneakyThrows
    private List<HistoryRecordEntity> propagateTestHistoryRecord() {
        List<HistoryRecordEntity> entities = new ArrayList<>();
        HistoryRecordDto recordDto = HistoryRecordTestData.getTestHistoryRecord();
        entities.add(service.saveOrUpdateRecord(recordDto));
        for (int idx = 1; idx < 4; idx++) {
            HistoryRecordDto dto = HistoryRecordTestData.getNewHistoryRecord();
            dto.setCreationTime(System.currentTimeMillis() + 10000 * idx);
            dto.setEventType(HistoryRecordTestData.RECORD_CHANGED_EVENT_TYPE);
            dto.setComments("Some test comment for history record " + String.valueOf(idx));
            dto.setUsername(HistoryRecordTestData.ADMIN);
            dto.setHistoryEventId(String.valueOf(idx));
            entities.add(service.saveOrUpdateRecord(dto));
        }
        assertEquals(4, entities.size());
        return entities;
    }

    private static RecordsQuery.Builder getQuery(Predicate predicate) {
        return RecordsQuery.create()
            .withSourceId(HistoryRecordRecordsDao.ID)
            .withLanguage(PredicateService.LANGUAGE_PREDICATE)
            .withQuery(predicate);
    }

    private static DataValue getAttributesJsonArray() {
        return DataValue.createArr()
            .add(HistoryRecordTestData.PROP_ID + STR)
            .add(HistoryRecordEntity.USERNAME + STR)
            .add(HistoryRecordEntity.CREATION_TIME)// + "|fmt(\"yyyy__MM__dd HH:mm\")")
            .add(HistoryRecordEntity.COMMENTS + STR)
            .add(HistoryRecordEntity.EVENT_TYPE + STR)
            .add(HistoryRecordEntity.DOCUMENT_ID + STR);
    }

    private DataValue getHistoryRecordAttributes(HistoryRecordDto dto) {
        Map<String, String> attrMap = historyRecordConverter.toMap(dto);
        DataValue result = DataValue.createObj();
        attrMap.forEach(result::set);
        return result;
    }

    private static String getJsonToSend(String json) {
        return json.replace("\\/", "/");
    }
}
