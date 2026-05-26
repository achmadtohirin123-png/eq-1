package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "presets")
data class Preset(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isUserPreset: Boolean = true,
    
    // Theme configs
    val themeAccentHex: String = "#00FFFF", // LED Cyan Default
    val layoutStyle: Int = 0, // 0 = Studio, 1 = compact/performance
    
    // Equalizer bands - saved as comma separated strings
    // e.g., "0.0,0.0,2.0,-1.0,..."
    val eqMode: Int = 7, // 7, 15, or 31 band
    val eqGains7: String = "0.0,0.0,0.0,0.0,0.0,0.0,0.0",
    val eqGains15: String = "0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0",
    val eqGains31: String = "0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0",
    
    // Digital Mixer sliders
    val inputGain: Float = 1.0f,
    val masterOutput: Float = 0.8f,
    val balancePan: Float = 0.0f, // -1.0 to +1.0 for Left to Right
    val isStereo: Boolean = true,
    val audioQuality: Int = 1, // 0 = ECO, 1 = HIGH, 2 = Ultra low latency
    
    // Audio FX rack
    val isReverbOn: Boolean = false,
    val reverbLevel: Float = 0.4f, // 0.0 to 1.0
    val reverbDecay: Float = 2.5f, // Seconds
    
    val isDelayOn: Boolean = false,
    val delayTimeMs: Int = 250,
    val delayFeedback: Float = 0.3f,
    
    val isBassBoostOn: Boolean = false,
    val bassBoostValue: Float = 0.5f,
    
    val isCompressorOn: Boolean = false,
    val compThreshold: Float = -20f, // dB
    val compRatio: Float = 4.0f,
    
    val isLimiterOn: Boolean = true,
    val limiterThreshold: Float = -1.0f,
    
    val isNoiseGateOn: Boolean = false,
    val gateThreshold: Float = -60f,
    
    val isStereoWidenerOn: Boolean = false,
    val stereoWidth: Float = 1.2f, // 1.0 is original, up to 2.0
    
    val is3dAudioOn: Boolean = false,
    val spatialDepth: Float = 0.5f,
    
    val isPitchCorrectionOn: Boolean = false,
    val pitchKey: Int = 0, // C, C#, D, ...
    
    val isDistortionOn: Boolean = false,
    val distortionAmount: Float = 0.2f,
    
    val timestamp: Long = System.currentTimeMillis()
)
