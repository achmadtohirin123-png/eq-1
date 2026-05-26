package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY timestamp DESC")
    fun getAllPresets(): Flow<List<Preset>>

    @Query("SELECT * FROM presets WHERE id = :id LIMIT 1")
    suspend fun getPresetById(id: Int): Preset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: Preset): Long

    @Delete
    suspend fun deletePreset(preset: Preset)
    
    @Query("DELETE FROM presets WHERE id = :id")
    suspend fun deletePresetById(id: Int)
}
