package ru.citeck.ecos.history.api.records;

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
import ru.citeck.ecos.history.HistoryApp;
import ru.citeck.ecos.history.TestUtil;
import ru.citeck.ecos.history.converter.impl.HistoryRecordConverter;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.dto.HistoryRecordDto;
import ru.citeck.ecos.history.service.HistoryRecordService;

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
                    .content(getTestHistoryRecordJson(recordEntity.getId())))
            .andDo(print());
        //.andExpect(jsonPath("$.." + BoardTestData.PROP_NAME + STR).value(IsNull.nullValue()));
    }

    public HistoryRecordEntity createTestHistoryRecord() {
        HistoryRecordDto recordDto = HistoryRecordTestData.getTestHistoryRecord();
        return service.saveOrUpdateRecord(recordDto);
    }

    private static String getTestHistoryRecordJson(Long localRecordId) throws Exception {
        return getJsonToSend(new JSONObject()
            .put(RECORDS, new JSONArray().put(HistoryRecordTestData.getEmptyId() +
                (localRecordId != null ? localRecordId.toString() : "")))
            .put(ATTRIBUTES, new JSONArray()
                .put(HistoryRecordTestData.PROP_ID + STR)
                .put(HistoryRecordEntity.USERNAME + STR)
                .put(HistoryRecordEntity.DOCUMENT_ID + STR)
                .put(HistoryRecordTestData.PROP_COLUMNS + "[]" + STR))
            .toString(2));
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
