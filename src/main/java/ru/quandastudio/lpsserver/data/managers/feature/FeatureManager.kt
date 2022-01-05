package ru.quandastudio.lpsserver.data.managers.feature

import ru.quandastudio.lpsserver.data.entities.User
import ru.quandastudio.lpsserver.models.features.AppState

/**
 * Manager that handles [AppState]
 */
interface FeatureManager {
    /**
     * Gets an app state
     */
    fun getAppState(user: User?): AppState
}