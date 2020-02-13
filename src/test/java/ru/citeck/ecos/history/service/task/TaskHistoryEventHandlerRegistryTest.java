package ru.citeck.ecos.history.service.task;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ru.citeck.ecos.history.HistoryApp;
import ru.citeck.ecos.history.service.task.impl.AssignTaskEventTypeHandler;
import ru.citeck.ecos.history.service.task.impl.CompleteTaskEventTypeHandler;
import ru.citeck.ecos.history.service.task.impl.CreateTaskEventTypeHandler;
import ru.citeck.ecos.history.service.task.impl.StatusChangeEventTypeHandler;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = HistoryApp.class)
public class TaskHistoryEventHandlerRegistryTest {

    @Autowired
    TaskHistoryEventHandlerRegistry registry;

    @Test
    public void getHandler() {
        Assert.assertTrue(registry.getHandler("task.create") instanceof CreateTaskEventTypeHandler);
        Assert.assertTrue(registry.getHandler("task.assign") instanceof AssignTaskEventTypeHandler);
        Assert.assertTrue(registry.getHandler("task.complete") instanceof CompleteTaskEventTypeHandler);
        Assert.assertTrue(registry.getHandler("status.changed") instanceof StatusChangeEventTypeHandler);
    }
}
