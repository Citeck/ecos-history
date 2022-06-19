package ru.citeck.ecos.history.api.records;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.history.HistoryApp;
import ru.citeck.ecos.history.TestUtil;
import ru.citeck.ecos.history.converter.HistoryRecordConverter;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.dto.HistoryRecordDto;
import ru.citeck.ecos.history.service.HistoryRecordService;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(EcosSpringExtension.class)
@SpringBootTest(classes = HistoryApp.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class HistoryRecordRecordsDaoTest {

    static String QUERY = "query";
    static String LANGUAGE = "language";
    static String RECORDS = "records";
    static String ATTRIBUTES = "attributes";
    static String STR = "?str";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private HistoryRecordService service;
    @Autowired
    private HistoryRecordConverter historyRecordConverter;

    @Test
    public void createHistoryRecord() throws Exception {
        String jsonString = getJsonToSend(DataValue.createObj()
            .set(RECORDS, DataValue.createArr().add(
                DataValue.createObj()
                    .set(HistoryRecordTestData.PROP_ID, HistoryRecordTestData.getEmptyId())
                    .set(ATTRIBUTES, getHistoryRecordAttributes(HistoryRecordTestData.getTestHistoryRecord()))
            )).toString());

        final ResultActions resultActions =
            mockMvc.perform(MockMvcRequestBuilders.post(TestUtil.URL_RECORDS_MUTATE)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(jsonString));
        resultActions.andExpect(status().isOk())
            .andExpect(jsonPath("$." + RECORDS).isNotEmpty());
    }

    @Test
    public void queryEqualByUsername() throws Exception {
        propagateTestHistoryRecord();
        String jsonString = getJsonToSend(getQueryJson(getPredicateJson(HistoryRecordEntity.USERNAME,
            HistoryRecordTestData.getTestHistoryRecord().getUsername(),
            HistoryRecordTestData.PREDICATE_TYPE_EQUAL)));

        ResultActions resultActions = mockMvc.perform(
            MockMvcRequestBuilders.post(TestUtil.URL_RECORDS_QUERY)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(jsonString));
        resultActions.andExpect(status().isOk())
            .andExpect(jsonPath("$.totalCount").value(1))
            .andDo(print());
    }

    @Test
    public void queryEqualByCreationTime() throws Exception {
        propagateTestHistoryRecord();
        String jsonString = getJsonToSend(getQueryJson(getPredicateJson(HistoryRecordEntity.CREATION_TIME,
            String.valueOf(HistoryRecordTestData.getTestHistoryRecord().getCreationTime()),
            HistoryRecordTestData.PREDICATE_TYPE_EQUAL)));

        ResultActions resultActions = mockMvc.perform(
            MockMvcRequestBuilders.post(TestUtil.URL_RECORDS_QUERY)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(jsonString));
        resultActions.andExpect(status().isOk())
            .andExpect(jsonPath("$.totalCount").value(1))
            .andDo(print());
    }

    @Test
    public void queryGtByCreationTime() throws Exception {
        List<HistoryRecordEntity> entities = propagateTestHistoryRecord();
        String jsonString = getJsonToSend(getQueryJson(getPredicateJson(HistoryRecordEntity.CREATION_TIME,
            String.valueOf(HistoryRecordTestData.getTestHistoryRecord().getCreationTime()), //.PROP_CREATETIME_VALUE),
            HistoryRecordTestData.PREDICATE_TYPE_GREATER_THAN)));
        ResultActions resultActions = mockMvc.perform(
            MockMvcRequestBuilders.post(TestUtil.URL_RECORDS_QUERY)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(jsonString));
        resultActions.andExpect(status().isOk())
            .andExpect(jsonPath("$.totalCount").value(3))
            .andDo(print());
    }

    @Test
    public void queryLtByCreationTime() throws Exception {
        List<HistoryRecordEntity> entities = propagateTestHistoryRecord();
        int last = entities.size() - 1;
        String jsonString = getJsonToSend(getQueryJson(getPredicateJson(HistoryRecordEntity.CREATION_TIME,
            String.valueOf(entities.get(last).getCreationTime().getTime()),
            HistoryRecordTestData.PREDICATE_TYPE_LESS_THAN)));

        ResultActions resultActions = mockMvc.perform(
            MockMvcRequestBuilders.post(TestUtil.URL_RECORDS_QUERY)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(jsonString));
        resultActions.andExpect(status().isOk())
            .andExpect(jsonPath("$.totalCount").value(3))
            .andDo(print());
    }

    @Test
    public void queryGtByUsernameAndCreationTime() throws Exception {
        propagateTestHistoryRecord();
        DataValue composed = getComposedPredicateJson(HistoryRecordTestData.PREDICATE_TYPE_AND,
            getPredicateJson(HistoryRecordEntity.USERNAME,
                HistoryRecordTestData.ADMIN,
                HistoryRecordTestData.PREDICATE_TYPE_EQUAL),
            getPredicateJson(HistoryRecordEntity.CREATION_TIME,
                String.valueOf(HistoryRecordTestData.PROP_CREATETIME_VALUE),
                HistoryRecordTestData.PREDICATE_TYPE_GREATER_THAN));
        String jsonString = getJsonToSend(getQueryJson(composed));

        ResultActions resultActions = mockMvc.perform(
            MockMvcRequestBuilders.post(TestUtil.URL_RECORDS_QUERY)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(jsonString));
        resultActions.andExpect(status().isOk())
            .andExpect(jsonPath("$.totalCount").value(3))
            .andDo(print());
    }

    @Test
    public void queryOrByUsername() throws Exception {
        propagateTestHistoryRecord();
        DataValue composed = getComposedPredicateJson(HistoryRecordTestData.PREDICATE_TYPE_OR,
            getPredicateJson(HistoryRecordEntity.USERNAME,
                HistoryRecordTestData.ADMIN,
                HistoryRecordTestData.PREDICATE_TYPE_EQUAL),
            getPredicateJson(HistoryRecordEntity.USERNAME,
                HistoryRecordTestData.getTestHistoryRecord().getUsername(),
                HistoryRecordTestData.PREDICATE_TYPE_EQUAL));
        String jsonString = getJsonToSend(getQueryJson(composed));

        ResultActions resultActions = mockMvc.perform(
            MockMvcRequestBuilders.post(TestUtil.URL_RECORDS_QUERY)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(jsonString));
        resultActions.andExpect(status().isOk())
            .andExpect(jsonPath("$.totalCount").value(4))
            .andDo(print());
    }

    @Test
    public void queryPeriodByCreationTime() throws Exception {
        List<HistoryRecordEntity> entities = propagateTestHistoryRecord();
        DataValue composed = getComposedPredicateJson(HistoryRecordTestData.PREDICATE_TYPE_AND,
            getPredicateJson(HistoryRecordEntity.CREATION_TIME,
                String.valueOf(HistoryRecordTestData.getTestHistoryRecord().getCreationTime()),
                HistoryRecordTestData.PREDICATE_TYPE_GREATER_THAN),
            getPredicateJson(HistoryRecordEntity.CREATION_TIME,
                String.valueOf(entities.get(entities.size() - 1).getCreationTime().getTime()),
                HistoryRecordTestData.PREDICATE_TYPE_LESS_THAN));
        String jsonString = getJsonToSend(getQueryJson(composed));

        ResultActions resultActions = mockMvc.perform(
            MockMvcRequestBuilders.post(TestUtil.URL_RECORDS_QUERY)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(jsonString));
        resultActions.andExpect(status().isOk())
            .andExpect(jsonPath("$.totalCount").value(2))
            .andDo(print());
    }

    @Test
    public void queryStartsByComment() throws Exception {
        propagateTestHistoryRecord();
        String jsonString = getJsonToSend(getQueryJson(getPredicateJson(HistoryRecordEntity.COMMENTS,
            "Some",
            HistoryRecordTestData.PREDICATE_TYPE_STARTS)));

        ResultActions resultActions = mockMvc.perform(
            MockMvcRequestBuilders.post(TestUtil.URL_RECORDS_QUERY)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(jsonString));
        resultActions.andExpect(status().isOk())
            .andExpect(jsonPath("$.totalCount").value(3))
            .andDo(print());
    }

    @Test
    public void queryEqualByUsernameAndEventType() throws Exception {
        propagateTestHistoryRecord();
        DataValue composed = getComposedPredicateJson(HistoryRecordTestData.PREDICATE_TYPE_AND,
            getPredicateJson(HistoryRecordEntity.USERNAME,
                HistoryRecordTestData.ADMIN,
                HistoryRecordTestData.PREDICATE_TYPE_EQUAL),
            getPredicateJson(HistoryRecordEntity.EVENT_TYPE,
                HistoryRecordTestData.RECORD_CHANGED_EVENT_TYPE,
                HistoryRecordTestData.PREDICATE_TYPE_EQUAL));
        String jsonString = getJsonToSend(getQueryJson(composed));

        ResultActions resultActions = mockMvc.perform(
            MockMvcRequestBuilders.post(TestUtil.URL_RECORDS_QUERY)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(jsonString));
        resultActions.andExpect(status().isOk())
            .andExpect(jsonPath("$.totalCount").value(3))
            .andDo(print());
    }

    @Test
    public void updateRecord() throws Exception {
        HistoryRecordEntity recordEntity = createTestHistoryRecord();
        String comment = "Updated test comment";

        String jsonString = getJsonToSend(DataValue.createObj()
            .set(RECORDS, DataValue.createArr().add(
                DataValue.createObj().set(
                    "id", HistoryRecordTestData.getEmptyId() + recordEntity.getId()
                ).set(ATTRIBUTES, DataValue.createObj()
                    .set("historyEventId", recordEntity.getId())
                    .set("documentId", "test/doc@123")
                    .set("creationTime", System.currentTimeMillis())
                    .set("eventType", "created")
                    .set("userId", "admin")
                    .set("username", "admin")
                    .set(HistoryRecordEntity.COMMENTS, comment))
            )).toString());

        ResultActions resultActions = mockMvc.perform(
            MockMvcRequestBuilders.post(TestUtil.URL_RECORDS_MUTATE)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(jsonString));
        resultActions.andExpect(status().isOk())
            .andExpect(jsonPath("$." + RECORDS).isNotEmpty());


        resultActions = mockMvc.perform(
            MockMvcRequestBuilders.post(TestUtil.URL_RECORDS_QUERY)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(queryTestHistoryRecordJson(recordEntity.getId())));
        resultActions.andExpect(status().isOk())
            .andDo(print())
            .andExpect(jsonPath("$." + RECORDS).isNotEmpty())
            .andExpect(jsonPath("$.." + HistoryRecordEntity.COMMENTS + STR).value(comment));
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

    private static DataValue getPredicateJson(String attributeName, String attributeValue,
                                              String precicateType) throws Exception {
        return DataValue.createObj().set("att", attributeName)
            .set(HistoryRecordTestData.PREDICATE_VAL, attributeValue)
            .set(HistoryRecordTestData.PREDICATE_TYPE, precicateType);
    }

    private static DataValue getComposedPredicateJson(String precicateType, DataValue... predicates)
        throws Exception {
        DataValue jsonArray = DataValue.createArr();
        for (DataValue predicate : predicates) {
            jsonArray.add(predicate);
        }
        return DataValue.createObj().set(HistoryRecordTestData.PREDICATE_TYPE, precicateType)
            .set(HistoryRecordTestData.PREDICATE_VAL, jsonArray);
    }

    private static String getQueryJson(DataValue predicate) throws Exception {
        String jsonString = DataValue.createObj()
            .set(QUERY,
                DataValue.createObj().set("sourceId", HistoryRecordRecordsDao.ID)
                    .set(LANGUAGE, PredicateService.LANGUAGE_PREDICATE)
                    .set(QUERY, predicate)
                    .set(ATTRIBUTES, getAttributesJsonArray()))
            .toString();
        return jsonString;
    }

    private static String queryTestHistoryRecordJson(Long localRecordId) throws Exception {
        return getJsonToSend(DataValue.createObj()
            .set(RECORDS, DataValue.createArr().add(HistoryRecordTestData.getEmptyId() +
                (localRecordId != null ? localRecordId.toString() : "")))
            .set(ATTRIBUTES, getAttributesJsonArray())
            .toString());
    }

    private static DataValue getAttributesJsonArray() throws Exception {
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
        attrMap.entrySet().forEach(entry -> {
            result.set(entry.getKey(), entry.getValue());
        });
        return result;
    }

    private static String getJsonToSend(String json) {
        return json.replace("\\/", "/");
    }
}
