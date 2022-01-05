package ru.quandastudio.lpsserver.data.managers.feature

import org.springframework.stereotype.Service
import ru.quandastudio.lpsserver.data.entities.User
import ru.quandastudio.lpsserver.models.features.AppState

@Service
class FeatureManagerImpl : FeatureManager {
    override fun getAppState(user: User?): AppState {
        return AppState(
            latestAppVersionCode = 0,
            featureToggles = mapOf(),
        )
    }
}