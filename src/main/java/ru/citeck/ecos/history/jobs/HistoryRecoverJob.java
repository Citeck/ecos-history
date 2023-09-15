package ru.citeck.ecos.history.jobs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.service.HistoryRecordService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service("historyRecoverJob")
public class HistoryRecoverJob {

    private static final String HISTORY_RECORD_FILE_NAME = "history_record";
    private static final String DELIMITER = "\\|\\|";
    private static final String[] KEYS = {
        "historyEventId",
        "documentId",
        "eventType",
        "comments",
        "version",
        "creationTime",
        "username",
        "userId",
        "taskRole",
        "taskOutcome",
        "taskType",
        "fullTaskType",
        "initiator",
        "workflowInstanceId",
        "workflowDescription",
        "taskEventInstanceId",
        "documentVersion",
        "propertyName",
        "expectedPerformTime",
        "taskTitle",
        "taskDefinitionKey",
        "taskOutcomeName"
    };

    @Value("${ecos-history.recover.sourceFolder}")
    private String csvFolder;
    @Value("${ecos-history.recover.errorsFolder}")
    private String errorsFolder;
    @Value("${ecos-history.recover.scheduled}")
    private Boolean enableScheduled;

    private HistoryRecordService historyRecordService;

    @Scheduled(initialDelay = 10000, fixedDelay = 30000)
    public void historyRecovering() {
        if (!enableScheduled) {
            return;
        }

        File csvDir = new File(csvFolder);
        if (!csvDir.exists()) {
            return;
        }

        File[] csvFiles = csvDir.listFiles();
        if (csvFiles == null) {
            return;
        }

        for (File file : csvFiles) {
            if (file.getName().startsWith(HISTORY_RECORD_FILE_NAME) && file.getName().endsWith(".csv")) {
                recoverHistoryRecord(file);
            }
        }
    }

    private void recoverHistoryRecord(File csvFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            for (String line; (line = reader.readLine()) != null; ) {
                String[] values = line.split(DELIMITER, -1);
                int keysCount = Math.min(KEYS.length, values.length);
                Map<String, String> requestParams = new HashMap<>();
                for (int i = 0; i < keysCount; i++) {
                    requestParams.put(KEYS[i], values[i]);
                }
                historyRecordService.saveOrUpdateRecord(new HistoryRecordEntity(), requestParams);
            }
        } catch (IOException | ParseException e) {
            log.error("Error while recovering history record from file: " + csvFile.getName(), e);
            moveToErrorDir(csvFile);
        }

        if (!csvFile.delete()) {
            log.warn("Error while deleting file: " + csvFile.getName());
        }
    }

    private void moveToErrorDir(File file) {
        Path errorsDir = Paths.get(errorsFolder + file.getName());
        try {
            Files.move(file.toPath(), errorsDir, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Error while moving file to error dir: " + file.getName(), e);
        }
    }

    @Autowired
    public void setHistoryRecordService(HistoryRecordService historyRecordService) {
        this.historyRecordService = historyRecordService;
    }
}
