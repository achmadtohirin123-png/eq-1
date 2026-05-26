package com.example.ui.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioEngine
import com.example.data.Preset
import com.example.data.PresetDatabase
import com.example.data.PresetRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AudioViewModel(application: Application) : AndroidViewModel(application) {

    // Master Engine Instance
    val audioEngine = AudioEngine()

    // Database & Repository references
    private val database = PresetDatabase.getDatabase(application)
    private val repository = PresetRepository(database.presetDao())

    // List of all presets (Default system + custom user saved)
    val presetsState: StateFlow<List<Preset>> = repository.allPresets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PresetRepository.getStaticPresets()
        )

    // Current Active Preset
    private val _activePreset = MutableStateFlow(PresetRepository.getStaticPresets().first())
    val activePreset: StateFlow<Preset> = _activePreset.asStateFlow()

    // Floating Player State & Minimizer
    val isMiniPlayerVisible = MutableStateFlow(false)

    // Real-time Visual Theme Accents (Neon glow)
    private val _themeColorHex = MutableStateFlow("#00FFFF") // Init Cyan Neon
    val themeColorHex: StateFlow<String> = _themeColorHex.asStateFlow()

    // Navigation State
    private val _currentScreen = MutableStateFlow("dashboard")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Active Slider Index For Manual EQ Customization
    val activeEqMode = MutableStateFlow(7) // 7, 15, 31 band state

    // Live mutable copies of current EQ gains
    val currentGains7 = MutableStateFlow(listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f))
    val currentGains15 = MutableStateFlow(List(15) { 0f })
    val currentGains31 = MutableStateFlow(List(31) { 0f })

    // Active Channel parameters
    val channelGain = MutableStateFlow(1.0f)
    val masterOutput = MutableStateFlow(0.8f)
    val channelPan = MutableStateFlow(0.0f)
    val isStereoMode = MutableStateFlow(true)
    val isMute = MutableStateFlow(false)
    val isSolo = MutableStateFlow(false)

    // VU Levels from the engine
    val streamVuLeft: StateFlow<Float> = audioEngine.vuLeft
    val streamVuRight: StateFlow<Float> = audioEngine.vuRight
    val spectrumBuffer: StateFlow<FloatArray> = audioEngine.visualizerBuffer

    // Live playback state
    val isMusicPlaying = MutableStateFlow(false)
    val progress: StateFlow<Float> = audioEngine.playbackProgress
    val trackTitle = audioEngine.currentSongTitle
    val trackArtist = audioEngine.currentSongArtist

    // File selection metadata
    val selectedMediaFilePath = MutableStateFlow("")

    init {
        // Load default values initially
        applyLoadedPreset(PresetRepository.getStaticPresets().first())
    }

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    fun changeThemeAccentHex(hexCode: String) {
        _themeColorHex.value = hexCode
        val updated = _activePreset.value.copy(themeAccentHex = hexCode)
        _activePreset.value = updated
        audioEngine.setPreset(updated)
    }

    fun togglePlayer() {
        if (isMusicPlaying.value) {
            audioEngine.stop()
            isMusicPlaying.value = false
        } else {
            audioEngine.start()
            isMusicPlaying.value = true
        }
    }

    fun skipNext() {
        // Mock shifting sources
        val nextSourceNum = (audioEngine.getInputSource().ordinal + 1) % AudioEngine.InputSource.values().size
        val nextSource = AudioEngine.InputSource.values()[nextSourceNum]
        audioEngine.setInputSource(nextSource)
        Toast.makeText(getApplication(), "Input rerouted: ${nextSource.name}", Toast.LENGTH_SHORT).show()
    }

    fun skipPrevious() {
        var prevSourceNum = (audioEngine.getInputSource().ordinal - 1)
        if (prevSourceNum < 0) prevSourceNum = AudioEngine.InputSource.values().size - 1
        val prevSource = AudioEngine.InputSource.values()[prevSourceNum]
        audioEngine.setInputSource(prevSource)
        Toast.makeText(getApplication(), "Input rerouted: ${prevSource.name}", Toast.LENGTH_SHORT).show()
    }

    fun selectAudioSource(source: AudioEngine.InputSource) {
        audioEngine.setInputSource(source)
        Toast.makeText(getApplication(), "Source: ${source.name} Connected", Toast.LENGTH_SHORT).show()
    }

    fun applyLoadedPreset(preset: Preset) {
        _activePreset.value = preset
        _themeColorHex.value = preset.themeAccentHex
        activeEqMode.value = preset.eqMode

        // Parse EQ Bands
        currentGains7.value = parseGains(preset.eqGains7, 7)
        currentGains15.value = parseGains(preset.eqGains15, 15)
        currentGains31.value = parseGains(preset.eqGains31, 31)

        // Set Mixer
        channelGain.value = preset.inputGain
        masterOutput.value = preset.masterOutput
        channelPan.value = preset.balancePan
        isStereoMode.value = preset.isStereo

        audioEngine.setPreset(preset)
        audioEngine.inputGain = preset.inputGain
        audioEngine.masterOutput = preset.masterOutput
        audioEngine.balancePan = preset.balancePan
        audioEngine.isStereo = preset.isStereo
    }

    // Individual FX toggles
    fun toggleReverb() {
        val curr = _activePreset.value
        val updated = curr.copy(isReverbOn = !curr.isReverbOn)
        updateActivePresetConfig(updated)
    }

    fun updateReverbValues(level: Float, decay: Float) {
        val curr = _activePreset.value
        val updated = curr.copy(reverbLevel = level, reverbDecay = decay)
        updateActivePresetConfig(updated)
    }

    fun toggleDelay() {
        val curr = _activePreset.value
        val updated = curr.copy(isDelayOn = !curr.isDelayOn)
        updateActivePresetConfig(updated)
    }

    fun updateDelayValues(timeMs: Int, feedback: Float) {
        val curr = _activePreset.value
        val updated = curr.copy(delayTimeMs = timeMs, delayFeedback = feedback)
        updateActivePresetConfig(updated)
    }

    fun toggleBassBoost() {
        val curr = _activePreset.value
        val updated = curr.copy(isBassBoostOn = !curr.isBassBoostOn)
        updateActivePresetConfig(updated)
    }

    fun updateBassBoostValue(value: Float) {
        val curr = _activePreset.value
        val updated = curr.copy(bassBoostValue = value)
        updateActivePresetConfig(updated)
    }

    fun toggleCompressor() {
        val curr = _activePreset.value
        val updated = curr.copy(isCompressorOn = !curr.isCompressorOn)
        updateActivePresetConfig(updated)
    }

    fun updateCompressorValues(threshold: Float, ratio: Float) {
        val curr = _activePreset.value
        val updated = curr.copy(compThreshold = threshold, compRatio = ratio)
        updateActivePresetConfig(updated)
    }

    fun toggleLimiter() {
        val curr = _activePreset.value
        val updated = curr.copy(isLimiterOn = !curr.isLimiterOn)
        updateActivePresetConfig(updated)
    }

    fun toggleNoiseGate() {
        val curr = _activePreset.value
        val updated = curr.copy(isNoiseGateOn = !curr.isNoiseGateOn)
        updateActivePresetConfig(updated)
    }

    fun updateNoiseGateThreshold(threshold: Float) {
        val curr = _activePreset.value
        val updated = curr.copy(gateThreshold = threshold)
        updateActivePresetConfig(updated)
    }

    fun toggleStereoWidener() {
        val curr = _activePreset.value
        val updated = curr.copy(isStereoWidenerOn = !curr.isStereoWidenerOn)
        updateActivePresetConfig(updated)
    }

    fun updateStereoWidth(width: Float) {
        val curr = _activePreset.value
        val updated = curr.copy(stereoWidth = width)
        updateActivePresetConfig(updated)
    }

    fun toggleSpatial3d() {
        val curr = _activePreset.value
        val updated = curr.copy(is3dAudioOn = !curr.is3dAudioOn)
        updateActivePresetConfig(updated)
    }

    fun updateSpatialDepth(depth: Float) {
        val curr = _activePreset.value
        val updated = curr.copy(spatialDepth = depth)
        updateActivePresetConfig(updated)
    }

    fun togglePitchCorrection() {
        val curr = _activePreset.value
        val updated = curr.copy(isPitchCorrectionOn = !curr.isPitchCorrectionOn)
        updateActivePresetConfig(updated)
    }

    fun updatePitchKey(key: Int) {
        val curr = _activePreset.value
        val updated = curr.copy(pitchKey = key)
        updateActivePresetConfig(updated)
    }

    fun toggleDistortion() {
        val curr = _activePreset.value
        val updated = curr.copy(isDistortionOn = !curr.isDistortionOn)
        updateActivePresetConfig(updated)
    }

    fun updateDistortionAmount(amount: Float) {
        val curr = _activePreset.value
        val updated = curr.copy(distortionAmount = amount)
        updateActivePresetConfig(updated)
    }

    // Mixer fader controls
    fun updateInputGain(gain: Float) {
        channelGain.value = gain
        audioEngine.inputGain = gain
        val updated = _activePreset.value.copy(inputGain = gain)
        updateActivePresetConfig(updated)
    }

    fun updateMasterOutput(master: Float) {
        if (isMute.value) return
        masterOutput.value = master
        audioEngine.masterOutput = master
        val updated = _activePreset.value.copy(masterOutput = master)
        updateActivePresetConfig(updated)
    }

    fun updatePan(pan: Float) {
        channelPan.value = pan
        audioEngine.balancePan = pan
        val updated = _activePreset.value.copy(balancePan = pan)
        updateActivePresetConfig(updated)
    }

    fun toggleStereoMode() {
        isStereoMode.value = !isStereoMode.value
        audioEngine.isStereo = isStereoMode.value
        val updated = _activePreset.value.copy(isStereo = isStereoMode.value)
        updateActivePresetConfig(updated)
    }

    fun toggleMute() {
        isMute.value = !isMute.value
        audioEngine.isMuteEnabled = isMute.value
        if (isMute.value) {
            audioEngine.masterOutput = 0f
        } else {
            audioEngine.masterOutput = masterOutput.value
        }
    }

    fun toggleSolo() {
        isSolo.value = !isSolo.value
        audioEngine.isSoloEnabled = isSolo.value
    }

    // EQ sliders real-time updating
    fun updateEqBand(index: Int, dbGains: Float) {
        when (activeEqMode.value) {
            7 -> {
                val list = currentGains7.value.toMutableList()
                if (index in list.indices) {
                    list[index] = dbGains
                    currentGains7.value = list
                    val gainsStr = list.joinToString(",") { String.format("%.2f", it) }
                    val updated = _activePreset.value.copy(eqGains7 = gainsStr)
                    updateActivePresetConfig(updated)
                }
            }
            15 -> {
                val list = currentGains15.value.toMutableList()
                if (index in list.indices) {
                    list[index] = dbGains
                    currentGains15.value = list
                    val gainsStr = list.joinToString(",") { String.format("%.2f", it) }
                    val updated = _activePreset.value.copy(eqGains15 = gainsStr)
                    updateActivePresetConfig(updated)
                }
            }
            31 -> {
                val list = currentGains31.value.toMutableList()
                if (index in list.indices) {
                    list[index] = dbGains
                    currentGains31.value = list
                    val gainsStr = list.joinToString(",") { String.format("%.2f", it) }
                    val updated = _activePreset.value.copy(eqGains31 = gainsStr)
                    updateActivePresetConfig(updated)
                }
            }
        }
    }

    fun setEqMode(mode: Int) {
        activeEqMode.value = mode
        val updated = _activePreset.value.copy(eqMode = mode)
        updateActivePresetConfig(updated)
    }

    private fun updateActivePresetConfig(preset: Preset) {
        _activePreset.value = preset
        audioEngine.setPreset(preset)
    }

    // Actions on repository database
    fun saveAsNewCustomPreset(presetName: String) {
        viewModelScope.launch {
            if (presetName.trim().isEmpty()) return@launch
            val newPreset = _activePreset.value.copy(
                id = 0, // auto-increment
                name = presetName,
                isUserPreset = true,
                timestamp = System.currentTimeMillis()
            )
            val newId = repository.insertPreset(newPreset)
            applyLoadedPreset(newPreset.copy(id = newId.toInt()))
            Toast.makeText(getApplication(), "Preset '$presetName' Saved Successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    fun deletePreset(id: Int) {
        viewModelScope.launch {
            repository.deletePresetById(id)
            Toast.makeText(getApplication(), "Preset Deleted", Toast.LENGTH_SHORT).show()
            // Reset to FLAT default if currently active is deleted
            if (_activePreset.value.id == id) {
                applyLoadedPreset(PresetRepository.getStaticPresets().first())
            }
        }
    }

    private fun parseGains(str: String, bandsCount: Int): List<Float> {
        return try {
            val list = str.split(",").map { it.trim().toFloatOrNull() ?: 0.0f }
            if (list.size != bandsCount) {
                List(bandsCount) { 0f }
            } else {
                list
            }
        } catch (e: Exception) {
            List(bandsCount) { 0f }
        }
    }

    override fun onCleared() {
        audioEngine.stop()
        super.onCleared()
    }
}
