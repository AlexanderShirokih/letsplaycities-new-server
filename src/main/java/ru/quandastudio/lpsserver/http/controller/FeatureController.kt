package ru.quandastudio.lpsserver.http.controller

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import ru.quandastudio.lpsserver.data.entities.User
import ru.quandastudio.lpsserver.data.managers.feature.FeatureManager
import ru.quandastudio.lpsserver.models.features.AppState

/**
 * Controller that manager feature-on toggles
 */
@RestController
@RequestMapping("/features")
class FeatureController(
    private val featureManager: FeatureManager,
) {

    /**
     * Gets current app state.
     */
    @GetMapping
    @ResponseBody
    fun getAppState(
        @AuthenticationPrincipal user: User?,
    ): AppState {
        return featureManager.getAppState(user)
    }

}