package ru.citeck.ecos.history.service.task;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import ru.citeck.ecos.history.service.task.impl.AssignTaskEventTypeHandler;
import ru.citeck.ecos.history.service.task.impl.CompleteTaskEventTypeHandler;
import ru.citeck.ecos.history.service.task.impl.CreateTaskEventTypeHandler;
import ru.citeck.ecos.history.service.task.impl.StatusChangeEventTypeHandler;

import static org.junit.Assert.*;

public class TaskHistoryEventHandlerRegistryTest {

    @Test
    public void getHandler() {
        TaskHistoryEventHandlerRegistry registry = new TaskHistoryEventHandlerRegistry();

        registry.register(new CreateTaskEventTypeHandler());
        registry.register(new AssignTaskEventTypeHandler());
        registry.register(new CompleteTaskEventTypeHandler());
        registry.register(new StatusChangeEventTypeHandler());

        Assert.assertTrue(registry.getHandler("task.create") instanceof CreateTaskEventTypeHandler);
        Assert.assertTrue(registry.getHandler("task.assign") instanceof AssignTaskEventTypeHandler);
        Assert.assertTrue(registry.getHandler("task.complete") instanceof CompleteTaskEventTypeHandler);
        Assert.assertTrue(registry.getHandler("status.changed") instanceof StatusChangeEventTypeHandler);
    }
}
