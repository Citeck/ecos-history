package ru.citeck.ecos.history.service.task.impl

import ru.citeck.ecos.context.lib.auth.AuthContext

/**
 * For testing purposes: emulate auth context for ecos-events emitted
 */
fun emitEventAsUser(user: String, action: () -> Unit) {
    AuthContext.runAsFull(user) {
        AuthContext.runAsSystem {
            action()
        }
    }
}
