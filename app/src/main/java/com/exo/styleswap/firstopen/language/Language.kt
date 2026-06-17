package com.exo.styleswap.firstopen.language

/**
 * One language row in the picker.
 *
 * @param code  locale tag used for `values-<code>/` (legacy "in" for Indonesian)
 * @param flag  emoji flag (kept as text to avoid shipping 20 vector flags)
 * @param name  native, untranslated language name
 */
data class Language(val code: String, val flag: String, val name: String) {
    companion object {
        /** The 20 supported languages, mirroring res/xml/locales_config.xml. */
        val ALL: List<Language> = listOf(
            Language("en", "🇬🇧", "English"),
            Language("es", "🇪🇸", "Español"),
            Language("pt", "🇵🇹", "Português"),
            Language("in", "🇮🇩", "Bahasa Indonesia"),
            Language("de", "🇩🇪", "Deutsch"),
            Language("fr", "🇫🇷", "Français"),
            Language("ko", "🇰🇷", "한국어"),
            Language("ja", "🇯🇵", "日本語"),
            Language("zh", "🇨🇳", "中文"),
            Language("hi", "🇮🇳", "हिन्दी"),
            Language("vi", "🇻🇳", "Tiếng Việt"),
            Language("it", "🇮🇹", "Italiano"),
            Language("ru", "🇷🇺", "Русский"),
            Language("tr", "🇹🇷", "Türkçe"),
            Language("mr", "🇮🇳", "मराठी"),
            Language("ta", "🇮🇳", "தமிழ்"),
            Language("bn", "🇧🇩", "বাংলা"),
            Language("th", "🇹🇭", "ไทย"),
            Language("ar", "🇸🇦", "العربية"),
            Language("ur", "🇵🇰", "اردو")
        )

        fun nameOf(code: String?): String =
            ALL.firstOrNull { it.code == code }?.name ?: "English"
    }
}
