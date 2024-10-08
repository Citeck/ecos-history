package ru.citeck.ecos.history.controllers;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import ru.citeck.ecos.history.converter.HistoryRecordConverter;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.repository.HistoryRecordRepository;
import ru.citeck.ecos.history.service.HistoryRecordService;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/history_records")
public class HistoryRecordsController {

    private HistoryRecordRepository historyRecordRepository;
    private HistoryRecordConverter historyRecordConverter;
    private HistoryRecordService historyRecordService;

    @RequestMapping(method = RequestMethod.GET, value = "/all_records/page/{page}/limit/{limit}")
    public Object getAllRecords(@PathVariable Integer page, @PathVariable Integer limit) {
        List<HistoryRecordEntity> records = historyRecordRepository.getAllRecords(PageRequest.of(page, limit));
        return historyRecordConverter.toDto(records);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/by_document_id/{documentId}")
    public Object getAllRecordsByDocumentId(@PathVariable String documentId) {
        List<HistoryRecordEntity> records = historyRecordRepository.getRecordsByDocumentId(documentId);
        return historyRecordConverter.toDto(records);
    }

    @PostMapping("/get_document_history")
    public Object getAllRecordsByDocumentIdParam(@RequestBody String documentId) {
        List<HistoryRecordEntity> records = historyRecordRepository.getRecordsByDocumentId(documentId);
        return historyRecordConverter.toDto(records);
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/by_document_id/{documentId}")
    public Object removeAllRecordsByDocumentId(@PathVariable String documentId) {
        List<HistoryRecordEntity> allRecords = historyRecordRepository.getRecordsByDocumentId(documentId);
        historyRecordRepository.deleteAll(allRecords);
        return allRecords.size();
    }

    @PostMapping("/delete_document_history")
    public Object removeAllRecordsByDocumentIdParam(@RequestBody String documentId) {
        List<HistoryRecordEntity> allRecords = historyRecordRepository.getRecordsByDocumentId(documentId);
        historyRecordRepository.deleteAll(allRecords);
        return allRecords.size();
    }

    @RequestMapping(method = RequestMethod.GET, value = "/by_username/{username}/limit/{limit}")
    public Object getAllRecordsByUsername(@PathVariable String username, @PathVariable Integer limit) {
        List<HistoryRecordEntity> records = historyRecordRepository
            .getAllHistoryRecordsByUsername(username, PageRequest.of(0, limit));
        return historyRecordConverter.toDto(records);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/by_username/{username}/start_date/{startDate}/limit/{limit}")
    public Object getAllRecordsByUsernameWithDateFilter(@PathVariable String username,
                                                        @PathVariable Long startDate,
                                                        @PathVariable Integer limit) {
        Date start = new Date(startDate);

        List<HistoryRecordEntity> records = historyRecordRepository.getAllHistoryRecordsByUsernameWithStartDate(
            username, start, PageRequest.of(0, limit));
        return historyRecordConverter.toDto(records);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/by_username/{username}/start_date/{startDate}/end_date/{endDate}/limit/{limit}")
    public Object getAllRecordsByUsernameWithDateFilter(@PathVariable String username,
                                                        @PathVariable Long startDate,
                                                        @PathVariable Long endDate,
                                                        @PathVariable Integer limit) {
        Date start = new Date(startDate);
        Date end = new Date(endDate);

        List<HistoryRecordEntity> records = historyRecordRepository.getAllHistoryRecordsByUsernameWithStartEndDate(
            username, start, end, PageRequest.of(0, limit));
        return historyRecordConverter.toDto(records);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/insert_record")
    public boolean insertHistoryRecord(HttpServletRequest request) throws ParseException {
        Map<String, String> requestParams = transformMap(request.getParameterMap());
        HistoryRecordEntity result = historyRecordService.saveOrUpdateRecord(new HistoryRecordEntity(), requestParams);
        return result != null;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/insert_records")
    public boolean insertHistoryRecords(@RequestParam(value = "records", required = false) String records) throws IOException, ParseException {
        List<HistoryRecordEntity> result = historyRecordService.saveOrUpdateRecords(records);
        return result != null;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/update_record/{recordId}")
    public HistoryRecordEntity updateHistoryRecord(HttpServletRequest request, @PathVariable String recordId) throws ParseException {
        String errorMsg = "No history record with uuid \"" + recordId + "\" has been found";
        try {
            Long idValue = Long.valueOf(recordId);
            HistoryRecordEntity recordEntity = historyRecordRepository.findById(idValue).orElse(null);
            if (recordEntity == null) {
                throw new RuntimeException(errorMsg);
            } else {
                return historyRecordService.saveOrUpdateRecord(recordEntity, transformMap(request.getParameterMap()));
            }
        } catch (NumberFormatException e){
            throw new RuntimeException(errorMsg, e);
        }
    }

    private Map<String, String> transformMap(Map<String, String[]> sourceMap) {
        Map<String, String> resultMap = new HashMap<>();
        for (String key : sourceMap.keySet()) {
            if (sourceMap.get(key).length > 0) {
                resultMap.put(key, sourceMap.get(key)[0]);
            } else {
                resultMap.put(key, null);
            }
        }
        return resultMap;
    }

    @Data
    private static class DocumentDto {
        private String documentId;
    }

    @Autowired
    public void setHistoryRecordRepository(HistoryRecordRepository historyRecordRepository) {
        this.historyRecordRepository = historyRecordRepository;
    }

    @Autowired
    public void setHistoryRecordConverter(HistoryRecordConverter historyRecordConverter) {
        this.historyRecordConverter = historyRecordConverter;
    }

    @Autowired
    public void setHistoryRecordService(HistoryRecordService historyRecordService) {
        this.historyRecordService = historyRecordService;
    }
}
