package ru.citeck.ecos.history.api.records;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.citeck.ecos.events2.type.RecordCreatedEvent;
import ru.citeck.ecos.history.HistoryApp;
import ru.citeck.ecos.history.TestUtil;
import ru.citeck.ecos.history.converter.impl.HistoryRecordConverter;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.dto.HistoryRecordDto;
import ru.citeck.ecos.history.service.HistoryRecordService;
import ru.citeck.ecos.records2.predicate.PredicateService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = HistoryApp.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
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
        String jsonString = getJsonToSend(new JSONObject()
            .put(RECORDS, new JSONArray().put(
                new JSONObject().put(HistoryRecordTestData.PROP_ID, HistoryRecordTestData.getEmptyId())
                    .put(ATTRIBUTES, getHistoryRecordAttributes(HistoryRecordTestData.getTestHistoryRecord()))
            )).toString(2));

        final ResultActions resultActions =
            mockMvc.perform(MockMvcRequestBuilders.post(TestUtil.URL_RECORDS_MUTATE)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(jsonString));
        resultActions.andExpect(status().isOk())
            .andExpect(jsonPath("$." + RECORDS).isNotEmpty());
    }

    @Test
    public void queryByUsername() throws Exception {
        //можно сделать параметаризированный (пользователь - количество)
        List<HistoryRecordEntity> entities = propagateTestHistoryRecord();
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
    public void updateRecord() throws Exception {
        HistoryRecordEntity recordEntity = createTestHistoryRecord();
        String comment = "Updated test comment";

        String jsonString = getJsonToSend(new JSONObject()
            .put(RECORDS, new JSONArray().put(
                new JSONObject().put(HistoryRecordTestData.PROP_ID, HistoryRecordTestData.getEmptyId() + recordEntity.getId())
                    .put(ATTRIBUTES, new JSONObject()
                        .put(HistoryRecordEntity.COMMENTS, comment))
            )).toString(2));

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
            .andExpect(jsonPath("$." + RECORDS).isNotEmpty())
            .andExpect(jsonPath("$.." + HistoryRecordEntity.COMMENTS + STR).value(comment));
    }

    @Test
    public void deleteHistoryRecord() throws Exception {
        HistoryRecordEntity recordEntity = createTestHistoryRecord();
        String jsonString = getJsonToSend(new JSONObject()
            .put(RECORDS, new JSONArray()
                .put(HistoryRecordTestData.getEmptyId() + recordEntity.getId()))
            .toString(2));

        mockMvc.perform(
                MockMvcRequestBuilders.post(TestUtil.URL_RECORDS_DELETE)
                    .contentType(TestUtil.APPLICATION_JSON_UTF8)
                    .content(jsonString))
            .andExpect(status().isOk());

        mockMvc.perform(
                MockMvcRequestBuilders.post(TestUtil.URL_RECORDS_QUERY)
                    .contentType(TestUtil.APPLICATION_JSON_UTF8)
                    .content(queryTestHistoryRecordJson(recordEntity.getId())))
            .andDo(print());
        //.andExpect(jsonPath("$.." + BoardTestData.PROP_NAME + STR).value(IsNull.nullValue()));
    }

    private HistoryRecordEntity createTestHistoryRecord() {
        HistoryRecordDto recordDto = HistoryRecordTestData.getTestHistoryRecord();
        return service.saveOrUpdateRecord(recordDto);
    }

    private List<HistoryRecordEntity> propagateTestHistoryRecord() {
        List<HistoryRecordEntity> entities = new ArrayList<>();
        HistoryRecordDto recordDto = HistoryRecordTestData.getTestHistoryRecord();
        entities.add(service.saveOrUpdateRecord(recordDto));
        for (int idx = 0; idx < 3; idx++) {
            HistoryRecordDto dto = HistoryRecordTestData.getNewHistoryRecord();
            dto.setEventType(HistoryRecordTestData.RECORD_CHANGED_EVENT_TYPE);
            dto.setComments("Some test comment for history record " + String.valueOf(idx));
            dto.setUsername("admin");
            dto.setHistoryEventId(String.valueOf(idx));
            entities.add(service.saveOrUpdateRecord(dto));
        }
        Assert.assertEquals(4, entities.size());
        return entities;
    }

    private static JSONObject getPredicateJson(String attributeName, String attributeValue,
                                               String precicateType) throws Exception {
        //{"att":"_type","val":"emodel/type@journal","t":"eq"}
        return new JSONObject().put("att", attributeName).put("val", attributeValue)
            .put("t", precicateType);
    }

    private static String getQueryJson(JSONObject predicate) throws Exception {
        String jsonString = new JSONObject()
            .put(QUERY,
                new JSONObject().put("sourceId", HistoryRecordRecordsDao.ID)
                    .put(LANGUAGE, PredicateService.LANGUAGE_PREDICATE)
                    .put(QUERY, predicate)
                    .put(ATTRIBUTES, getAttributesJsonArray()))
            .toString(2);
        return jsonString;
    }

    private static String queryTestHistoryRecordJson(Long localRecordId) throws Exception {
        return getJsonToSend(new JSONObject()
            .put(RECORDS, new JSONArray().put(HistoryRecordTestData.getEmptyId() +
                (localRecordId != null ? localRecordId.toString() : "")))
            .put(ATTRIBUTES, getAttributesJsonArray())
            .toString(2));
    }

    private static JSONArray getAttributesJsonArray() throws Exception {
        return new JSONArray()
            .put(HistoryRecordTestData.PROP_ID + STR)
            .put(HistoryRecordEntity.USERNAME + STR)
            .put(HistoryRecordEntity.CREATION_TIME)// + "|fmt(\"yyyy__MM__dd HH:mm\")")
            .put(HistoryRecordEntity.COMMENTS + STR)
            .put(HistoryRecordEntity.EVENT_TYPE + STR)
            .put(HistoryRecordEntity.DOCUMENT_ID + STR);
    }

    private JSONObject getHistoryRecordAttributes(HistoryRecordDto dto) {
        Map<String, String> attrMap = historyRecordConverter.toMap(dto);
        JSONObject result = new JSONObject();
        attrMap.entrySet().stream().forEach(entry -> {
            try {
                result.put(entry.getKey(), entry.getValue());
            } catch (JSONException e) {
            }
        });
        return result;
    }

    /*    private void deleteAll() {
            repository.deleteAll();
            repository.flush();
        }*/
    private static String getJsonToSend(String json) {
        return json.replace("\\/", "/");
    }
}
