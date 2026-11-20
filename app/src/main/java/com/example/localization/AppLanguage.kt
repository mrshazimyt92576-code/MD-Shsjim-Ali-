package com.example.localization

import androidx.compose.ui.unit.LayoutDirection

enum class AppLanguage(
    val code: String,
    val nativeLabel: String,
    val layoutDirection: LayoutDirection = LayoutDirection.Ltr
) {
    ENGLISH("en", "English"),
    BENGALI("bn", "বাংলা"),
    SPANISH("es", "Español"),
    HINDI("hi", "हिन्दी"),
    ARABIC("ar", "العربية", LayoutDirection.Rtl),
    JAPANESE("ja", "日本語");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}
