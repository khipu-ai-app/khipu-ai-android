package pe.khipuai.app.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import pe.khipuai.app.ui.theme.ThemeMode
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistencia local de las preferencias de UI. T-03 (modo oscuro).
 *
 * Hoy solo guarda el [ThemeMode], pero el patrón es extensible a otras
 * preferencias locales que no necesitan sincronizarse con el backend
 * (recordar que `SessionDataStore` ya guarda el JWT).
 */
private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "theme_prefs"
)

@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    }

    val themeModeFlow: Flow<ThemeMode> = context.themeDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val raw = preferences[THEME_MODE_KEY]
            // Si no hay valor guardado, default = seguir el sistema
            raw?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.name
        }
    }
}
