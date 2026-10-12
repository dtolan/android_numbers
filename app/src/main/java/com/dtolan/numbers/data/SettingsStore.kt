package com.dtolan.numbers.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dtolan.numbers.logic.PracticeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "numbers_settings")

/** Persists the user's chosen language and practice settings across app launches. */
class SettingsStore(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language_code")
        val MIN_VAL = intPreferencesKey("min_val")
        val MAX_VAL = intPreferencesKey("max_val")
        val DECIMAL_PLACES = intPreferencesKey("decimal_places")
        val YEAR_MIN = intPreferencesKey("year_min")
        val YEAR_MAX = intPreferencesKey("year_max")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val CONFIRM_LANGUAGE = stringPreferencesKey("confirm_language_code")
        fun category(c: Category) = booleanPreferencesKey("category_${c.name}")
        fun learnUnlocked(code: String) = intPreferencesKey("learn_unlocked_$code")
    }

    val languageCode: Flow<String> = context.dataStore.data.map { it[Keys.LANGUAGE] ?: Languages.FRENCH.code }

    val settings: Flow<PracticeSettings> = context.dataStore.data.map { prefs ->
        PracticeSettings(
            minVal = prefs[Keys.MIN_VAL] ?: 0,
            maxVal = prefs[Keys.MAX_VAL] ?: 100,
            decimalPlaces = prefs[Keys.DECIMAL_PLACES] ?: 2,
            yearMin = prefs[Keys.YEAR_MIN] ?: 1950,
            yearMax = prefs[Keys.YEAR_MAX] ?: 2026,
            speechRate = prefs[Keys.SPEECH_RATE] ?: 0.9f,
            enabledCategories = Category.entries.filter { prefs[Keys.category(it)] ?: (it == Category.INTEGER) }.toSet(),
        )
    }

    suspend fun setLanguage(code: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = code }
    }

    suspend fun setCategoryEnabled(category: Category, enabled: Boolean) {
        context.dataStore.edit { it[Keys.category(category)] = enabled }
    }

    suspend fun updateRanges(minVal: Int, maxVal: Int, decimalPlaces: Int, yearMin: Int, yearMax: Int) {
        context.dataStore.edit {
            it[Keys.MIN_VAL] = minVal
            it[Keys.MAX_VAL] = maxVal
            it[Keys.DECIMAL_PLACES] = decimalPlaces
            it[Keys.YEAR_MIN] = yearMin
            it[Keys.YEAR_MAX] = yearMax
        }
    }

    suspend fun setSpeechRate(rate: Float) {
        context.dataStore.edit { it[Keys.SPEECH_RATE] = rate }
    }

    suspend fun currentSettings(): PracticeSettings = settings.first()
    suspend fun currentLanguageCode(): String = languageCode.first()

    /** Index of the furthest-unlocked Learn stage for the language [code] (0 = only the first stage). */
    fun learnUnlockedIndex(code: String): Flow<Int> = context.dataStore.data.map { it[Keys.learnUnlocked(code)] ?: 0 }

    suspend fun setLearnUnlockedIndex(code: String, index: Int) {
        context.dataStore.edit { it[Keys.learnUnlocked(code)] = index }
    }

    suspend fun currentLearnUnlockedIndex(code: String): Int = learnUnlockedIndex(code).first()

    /**
     * The Listen-mode "confirm in" language — a global preference (the listener's own language,
     * independent of whichever language is currently being drilled), not tied to any one target
     * language. Null means confirmation is off.
     */
    val confirmLanguageCode: Flow<String?> = context.dataStore.data.map { it[Keys.CONFIRM_LANGUAGE] }

    suspend fun setConfirmLanguageCode(code: String?) {
        context.dataStore.edit {
            if (code == null) it.remove(Keys.CONFIRM_LANGUAGE) else it[Keys.CONFIRM_LANGUAGE] = code
        }
    }

    suspend fun currentConfirmLanguageCode(): String? = confirmLanguageCode.first()
}
