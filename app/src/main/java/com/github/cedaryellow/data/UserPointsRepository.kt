package com.github.cedaryellow.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

interface UserPointsRepository {
    val totalPoints: Flow<Int>
    suspend fun addPoints(points: Int)
    suspend fun usePoints(points: Int): Boolean
}

@Singleton
class DefaultUserPointsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : UserPointsRepository {

    private val TOTAL_POINTS_KEY = intPreferencesKey("total_points")

    override val totalPoints: Flow<Int> = context.dataStore.data
        .map { preferences -> 
            preferences[TOTAL_POINTS_KEY] ?: 0 
        }

    override suspend fun addPoints(points: Int) {
        context.dataStore.edit { preferences ->
            val currentPoints = preferences[TOTAL_POINTS_KEY] ?: 0
            preferences[TOTAL_POINTS_KEY] = currentPoints + points
        }
    }

    override suspend fun usePoints(points: Int): Boolean {
        var success = false
        context.dataStore.edit { preferences ->
            val currentPoints = preferences[TOTAL_POINTS_KEY] ?: 0
            if (currentPoints >= points) {
                preferences[TOTAL_POINTS_KEY] = currentPoints - points
                success = true
            }
        }
        return success
    }
} 