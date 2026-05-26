package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PresetRepository(private val presetDao: PresetDao) {

    // Get Flow of only user-saved custom presets
    val userPresets: Flow<List<Preset>> = presetDao.getAllPresets()

    // Retrieve entire list including default preloaded system presets
    val allPresets: Flow<List<Preset>> = presetDao.getAllPresets().map { userList ->
        getStaticPresets() + userList
    }

    suspend fun insertPreset(preset: Preset): Long {
        return presetDao.insertPreset(preset)
    }

    suspend fun deletePresetById(id: Int) {
        presetDao.deletePresetById(id)
    }

    suspend fun getPresetById(id: Int): Preset? {
        val staticPreset = getStaticPresets().find { it.id == id }
        if (staticPreset != null) return staticPreset
        return presetDao.getPresetById(id)
    }

    companion object {
        fun getStaticPresets(): List<Preset> {
            return listOf(
                Preset(
                    id = -1,
                    name = "FLAT",
                    isUserPreset = false,
                    themeAccentHex = "#00FFFF", // Cyan
                    eqGains7 = "0.0,0.0,0.0,0.0,0.0,0.0,0.0",
                    eqGains15 = "0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0",
                    isLimiterOn = true,
                    masterOutput = 0.8f
                ),
                Preset(
                    id = -2,
                    name = "BASS BOOST",
                    isUserPreset = false,
                    themeAccentHex = "#FF00FF", // Magenta
                    eqGains7 = "6.5,5.0,3.2,0.0,0.0,-1.0,-1.5",
                    eqGains15 = "8.0,7.0,5.5,4.0,2.5,1.0,0.0,0.0,-0.5,-1.0,-1.0,-1.0,-1.5,-1.5,-2.0",
                    isBassBoostOn = true,
                    bassBoostValue = 0.8f,
                    isStereoWidenerOn = true,
                    stereoWidth = 1.3f
                ),
                Preset(
                    id = -3,
                    name = "ROCK",
                    isUserPreset = false,
                    themeAccentHex = "#FFA500", // Neon Orange
                    eqGains7 = "4.0,2.5,-1.0,-2.0,1.5,3.0,4.5",
                    eqGains15 = "5.0,4.0,3.0,1.5,-1.0,-2.0,-2.5,-1.5,1.0,2.0,3.0,3.5,4.0,4.5,5.0",
                    isReverbOn = true,
                    reverbLevel = 0.3f,
                    reverbDecay = 1.8f
                ),
                Preset(
                    id = -4,
                    name = "VOCAL ENHANCER",
                    isUserPreset = false,
                    themeAccentHex = "#00FF00", // Lime green
                    eqGains7 = "-2.0,-1.0,1.5,3.5,4.0,2.5,1.0",
                    eqGains15 = "-3.0,-2.5,-2.0,-1.0,1.0,2.0,3.0,4.0,3.5,3.0,2.5,2.0,1.5,1.0,0.5",
                    isCompressorOn = true,
                    compThreshold = -18f,
                    compRatio = 3.5f,
                    masterOutput = 0.85f
                ),
                Preset(
                    id = -5,
                    name = "DANGDUT",
                    isUserPreset = false,
                    themeAccentHex = "#FFFF00", // Yellow-Gold
                    eqGains7 = "5.5,2.0,-1.5,-1.0,2.5,4.5,3.0",
                    eqGains15 = "7.0,5.0,3.5,1.0,-1.0,-2.0,-1.5,0.5,1.5,2.5,3.5,4.5,4.0,3.0,2.5",
                    isDelayOn = true,
                    delayTimeMs = 180,
                    delayFeedback = 0.25f,
                    isReverbOn = true,
                    reverbLevel = 0.25f
                ),
                Preset(
                    id = -6,
                    name = "EDM",
                    isUserPreset = false,
                    themeAccentHex = "#9D4EDD", // Electric Purple
                    eqGains7 = "6.0,4.0,0.0,2.0,1.5,4.0,5.5",
                    eqGains15 = "7.5,6.5,5.0,2.5,0.0,1.5,2.0,1.5,1.0,2.5,3.5,4.5,5.0,5.5,6.0",
                    isBassBoostOn = true,
                    bassBoostValue = 0.6f,
                    isStereoWidenerOn = true,
                    stereoWidth = 1.4f,
                    is3dAudioOn = true,
                    spatialDepth = 0.6f
                ),
                Preset(
                    id = -7,
                    name = "POP",
                    isUserPreset = false,
                    themeAccentHex = "#00BFFF", // Deep Sky Blue
                    eqGains7 = "2.0,1.5,0.0,1.0,2.0,1.5,2.5",
                    eqGains15 = "3.0,2.5,1.5,0.5,0.0,0.5,1.0,1.5,2.0,2.0,1.5,1.0,1.5,2.0,2.5"
                ),
                Preset(
                    id = -8,
                    name = "JAZZ STUDIO",
                    isUserPreset = false,
                    themeAccentHex = "#FF4500", // Neon Red-Orange
                    eqGains7 = "3.0,2.0,1.0,2.0,-1.0,1.5,2.0",
                    eqGains15 = "4.0,3.0,2.0,1.5,1.0,2.0,1.5,0.0,-1.0,0.5,1.0,1.5,2.0,2.0,2.5",
                    isReverbOn = true,
                    reverbLevel = 0.35f,
                    reverbDecay = 2.2f,
                    isStereoWidenerOn = true,
                    stereoWidth = 1.25f
                ),
                Preset(
                    id = -9,
                    name = "CLASSICAL",
                    isUserPreset = false,
                    themeAccentHex = "#E0AA3E", // Gold Champagne
                    eqGains7 = "2.5,1.5,-0.5,-1.0,0.0,1.5,2.0",
                    eqGains15 = "3.5,2.5,1.5,0.5,-0.5,-1.0,-1.0,-0.5,0.0,0.5,1.0,1.5,2.0,2.0,2.5",
                    is3dAudioOn = true,
                    spatialDepth = 0.7f
                ),
                Preset(
                    id = -10,
                    name = "STEREO LEBAR MAX",
                    isUserPreset = false,
                    themeAccentHex = "#00FFFF", // Bright Cyan
                    eqGains7 = "2.0,1.0,-0.5,0.0,1.5,3.0,4.5",
                    eqGains15 = "3.0,2.0,1.0,0.5,0.0,0.5,1.0,1.5,2.0,2.5,3.0,3.5,4.0,4.5,5.0",
                    isStereoWidenerOn = true,
                    stereoWidth = 2.8f, // Ultra-wide L & R Stage
                    is3dAudioOn = true,
                    spatialDepth = 0.75f
                )
            )
        }
    }
}
