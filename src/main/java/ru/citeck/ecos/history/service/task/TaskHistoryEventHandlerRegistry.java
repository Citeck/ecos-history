package ru.citeck.ecos.history.service.task;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TaskHistoryEventHandlerRegistry {

    private Map<String, TaskHistoryEventHandler> registry = new ConcurrentHashMap<>();

    public void register(AbstractTaskHistoryEventHandler handler) {
        registry.put(handler.getEventType(), handler);
    }

    public TaskHistoryEventHandler getHandler(String eventType) {
        if (eventType == null) {
            return null;
        }
        return registry.get(eventType);
    }

}
