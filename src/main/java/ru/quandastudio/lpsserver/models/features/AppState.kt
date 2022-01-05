package ru.quandastudio.lpsserver.models.features

/**
 * Describes server common properties and feature toggle state
 */
data class AppState(
    /**
     * Latest application version posted in the stores
     */
    val latestAppVersionCode: Int,
    /**
     * Feature toggles map. Used to control some functionality
     */
    val featureToggles: Map<String, Boolean>,
)
