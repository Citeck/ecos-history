package ru.citeck.ecos.history.service

enum class HistoryEventType(val value: String) {
    NODE_CREATED("node.created"),
    NODE_UPDATED("node.updated"),
    NODE_DELETED("node.deleted"),
    STATUS_CHANGED("status.changed"),
    TASK_CREATED("task.created"),
    TASK_COMPLETE("task.complete"),
    TASK_DELETE("task.delete"),
    TASK_ASSIGN("task.assign"),
    COMMENT_CREATED("comment.created"),
    COMMENT_DELETED("comment.deleted"),
    COMMENT_UPDATED("comment.updated"),
}
