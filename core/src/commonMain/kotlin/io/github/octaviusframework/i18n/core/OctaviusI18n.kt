package io.github.octaviusframework.i18n.core

/**
 * Main utility object for Octavius I18n plugin.
 * Holds the current language.
 */
object OctaviusI18n {

    /**
     * The currently selected language code (e.g. "en", "pl").
     * Change this property to switch the application's locale at runtime.
     */
    var currentLanguage: String = "en"
}
