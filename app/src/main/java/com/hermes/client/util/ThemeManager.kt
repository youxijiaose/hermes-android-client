package com.hermes.client.util

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.hermes.client.R

object ThemeManager {
    const val THEME_SYSTEM = "system"
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"
    const val THEME_HERMES = "hermes"      // Gold/Black theme
    const val THEME_MIDNIGHT = "midnight"   // Dark blue theme
    const val THEME_CYBERPUNK = "cyberpunk" // Neon theme

    fun applyTheme(context: Context, themeName: String) {
        when (themeName) {
            THEME_SYSTEM -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
            THEME_LIGHT -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            THEME_DARK -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            THEME_HERMES -> {
                // Custom Hermes theme - gold on dark
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                // In future: apply custom color scheme
            }
            THEME_MIDNIGHT -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            THEME_CYBERPUNK -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }
    }

    fun getThemeName(themeCode: Int): String {
        return when (themeCode) {
            AppCompatDelegate.MODE_NIGHT_NO -> THEME_LIGHT
            AppCompatDelegate.MODE_NIGHT_YES -> THEME_DARK
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> THEME_SYSTEM
            else -> THEME_SYSTEM
        }
    }

    fun getAvailableThemes(): List<ThemeOption> {
        return listOf(
            ThemeOption(THEME_SYSTEM, "Follow System", "Use system theme"),
            ThemeOption(THEME_LIGHT, "Light", "Always light theme"),
            ThemeOption(THEME_DARK, "Dark", "Always dark theme"),
            ThemeOption(THEME_HERMES, "Hermes Gold", "Gold accent on dark"),
            ThemeOption(THEME_MIDNIGHT, "Midnight", "Deep blue dark theme"),
            ThemeOption(THEME_CYBERPUNK, "Cyberpunk", "Neon accents")
        )
    }

    data class ThemeOption(
        val id: String,
        val name: String,
        val description: String
    )
}
