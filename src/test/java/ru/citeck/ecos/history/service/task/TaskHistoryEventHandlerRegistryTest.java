package ru.citeck.ecos.history.service.task;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.citeck.ecos.history.HistoryApp;
import ru.citeck.ecos.history.service.task.impl.AssignTaskEventTypeHandler;
import ru.citeck.ecos.history.service.task.impl.CompleteTaskEventTypeHandler;
import ru.citeck.ecos.history.service.task.impl.CreateTaskEventTypeHandler;
import ru.citeck.ecos.history.service.task.impl.StatusChangeEventTypeHandler;
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(EcosSpringExtension.class)
@SpringBootTest(classes = HistoryApp.class)
public class TaskHistoryEventHandlerRegistryTest {

    @Autowired
    TaskHistoryEventHandlerRegistry registry;

    @Test
    public void getHandler() {
        assertTrue(registry.getHandler("task.create") instanceof CreateTaskEventTypeHandler);
        assertTrue(registry.getHandler("task.assign") instanceof AssignTaskEventTypeHandler);
        assertTrue(registry.getHandler("task.complete") instanceof CompleteTaskEventTypeHandler);
        assertTrue(registry.getHandler("status.changed") instanceof StatusChangeEventTypeHandler);
    }
}
