package io.github.octaviusframework.i18n.core

import kotlin.concurrent.Volatile

/**
 * Main utility object for Octavius I18n plugin.
 * Holds the current language.
 */
object OctaviusI18n {

    /**
     * The currently selected language code (e.g. "en", "pl").
     * Change this property to switch the application's locale at runtime; every thread sees the change.
     */
    @Volatile
    var currentLanguage: String = "en"
}
