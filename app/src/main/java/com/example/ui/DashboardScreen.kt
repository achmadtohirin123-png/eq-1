package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioEngine
import com.example.data.Preset
import com.example.ui.viewmodel.AudioViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import kotlin.math.sin

@Composable
fun DashboardScreen(
    viewModel: AudioViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val rawHexAccent by viewModel.themeColorHex.collectAsState()
    val isMiniPlayerVisible by viewModel.isMiniPlayerVisible.collectAsState()

    // Map HEX to Compose Color
    val accentColor = remember(rawHexAccent) {
        try {
            Color(android.graphics.Color.parseColor(rawHexAccent))
        } catch (e: Exception) {
            Color.Cyan
        }
    }

    // Scrollable state for pages
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            StudioBottomNavigation(
                currentScreen = currentScreen,
                onSelectScreen = { viewModel.navigateTo(it) },
                accentColor = accentColor
            )
        },
        containerColor = DarkBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.radialGradient(
                        colors = listOf(accentColor.copy(alpha = 0.05f), DarkBg),
                        center = Offset(400f, 200f),
                        radius = 1200f
                    )
                )
        ) {
            // Main views routing
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn() + slideInVertically(initialOffsetY = { 300 }) togetherWith
                    fadeOut() + slideOutVertically(targetOffsetY = { -300 })
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    "dashboard" -> HomeDashboardView(viewModel, accentColor)
                    "mixer" -> MixerConsoleView(viewModel, accentColor)
                    "equalizer" -> EqualizerView(viewModel, accentColor)
                    "fx" -> EffectsRackView(viewModel, accentColor)
                    "visualizer" -> VisualizerPageView(viewModel, accentColor)
                    "settings" -> SettingsPageView(viewModel, accentColor)
                }
            }

            // Floating mini player overlay if enabled
            if (isMiniPlayerVisible) {
                FloatingMiniPlayer(
                    viewModel = viewModel,
                    accentColor = accentColor,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                )
            }
        }
    }
}

/**
 * Modern Neon Bottom Navigation Bar
 */
@Composable
fun StudioBottomNavigation(
    currentScreen: String,
    onSelectScreen: (String) -> Unit,
    accentColor: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        color = Color(0xFF0A0C10), // Match HTML bg-[#0A0C10]
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)) // Match HTML border-white/5
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val navItems = listOf(
                NavigationItem("dashboard", "HOME", Icons.Default.Dashboard),
                NavigationItem("mixer", "MIXER", Icons.Default.Tune),
                NavigationItem("equalizer", "EQ", Icons.Default.Equalizer),
                NavigationItem("fx", "EFFECTS", Icons.Default.Layers),
                NavigationItem("visualizer", "LIGHTS", Icons.Default.Waves),
                NavigationItem("settings", "SETUP", Icons.Default.Settings)
            )

            navItems.forEach { item ->
                val isSelected = currentScreen == item.id
                
                IconButton(
                    onClick = { onSelectScreen(item.id) },
                    modifier = Modifier
                        .testTag("nav_${item.id}")
                        .weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) accentColor.copy(alpha = 0.2f) else Color.Transparent)
                            .padding(vertical = 4.dp, horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) accentColor else Color(0xFF64748B), // Match HTML slate-500
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = item.label,
                            fontSize = 8.sp,
                            color = if (isSelected) accentColor else Color(0xFF64748B), // Match HTML slate-500
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

data class NavigationItem(val id: String, val label: String, val icon: ImageVector)

// ==========================================
// 1. HOME DASHBOARD MODULE
// ==========================================
@Composable
fun HomeDashboardView(viewModel: AudioViewModel, accentColor: Color) {
    val isPlaying by viewModel.isMusicPlaying.collectAsState()
    val title by viewModel.trackTitle.collectAsState()
    val artist by viewModel.trackArtist.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val currentSource = viewModel.audioEngine.getInputSource()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Bar Overlay / Telemetry Metrics
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("LATENCY: 3.2MS", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
            Text("SAMPLE RATE: 48KHZ", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
            Text("CPU: 12%", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
        }

        // App header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Neon glow logo icon
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF00F5FF), Color(0xFF7000FF))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Mixer Wave",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "BRO MUSIK ",
                            fontSize = 20.sp,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "EQ",
                            fontSize = 20.sp,
                            color = Color(0xFF00F5FF),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(
                        text = "ALL SYSTEM AUDIO PROCESSOR",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.4f),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // LED Power Light
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.05f),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .size(40.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                    .clip(CircleShape)
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "LedPulse")
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1250, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseAlpha"
                )

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPlaying) Color(0xFFEF4444).copy(alpha = pulseAlpha)
                            else Color.Gray
                        )
                )
            }
        }

        // Realtime Visualizer widget
        val visualBuffer by viewModel.spectrumBuffer.collectAsState()
        RealtimeSpectrumVisualizer(
            buffer = visualBuffer,
            accentColor = accentColor,
            modifier = Modifier.fillMaxWidth()
        )

        // Selected source Hub selector
        GlassStudioCard(accentColor = accentColor) {
            Text(
                text = "AUDIO ROUTING INTERFACE",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val sources = listOf(
                    SourceItem(AudioEngine.InputSource.DEMO, "Demo Beat", "Synthesized Loop Engine", Icons.Default.Memory),
                    SourceItem(AudioEngine.InputSource.MICROPHONE, "Microphone", "Low-latency Vocal DSP", Icons.Default.Mic),
                    SourceItem(AudioEngine.InputSource.SYSTEM, "System Capture", "Capture All Android Apps", Icons.Default.QueueMusic),
                    SourceItem(AudioEngine.InputSource.FILE, "Audio Browser", "Play offline Music files", Icons.Default.Folder)
                )

                sources.forEach { src ->
                    val isCurrent = currentSource == src.source
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isCurrent) accentColor.copy(alpha = 0.12f) else BorderColor.copy(alpha = 0.5f))
                            .border(1.dp, if (isCurrent) accentColor else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { viewModel.selectAudioSource(src.source) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = src.icon,
                            contentDescription = src.title,
                            tint = if (isCurrent) accentColor else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = src.title,
                                color = if (isCurrent) Color.White else Color.LightGray,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = src.desc,
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                        if (isCurrent) {
                            Text(
                                text = "CONNECTED",
                                color = accentColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Live DSP Player Deck Controller
         GlassStudioCard(accentColor = accentColor) {
            Text(
                text = "DSP PLAYBACK DECK",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(12.dp))

            // File player representation
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .border(1.dp, accentColor, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Playing Logo",
                        tint = accentColor
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = artist,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Realtime Linear Progress Slider
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = accentColor,
                trackColor = LedOffColor,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Audio Deck Playback Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.skipPrevious() }) {
                    Icon(Icons.Default.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(28.dp))
                }
                
                // Play-Pause main circular orb
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                        .clickable { viewModel.togglePlayer() }
                        .testTag("play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play Control",
                        tint = DarkBg,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(onClick = { viewModel.skipNext() }) {
                    Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }

        // Interactive 3D Hyper-Stereo Expansion Console (Indonesian: Stereo Lebar)
        val activePreset by viewModel.activePreset.collectAsState()
        GlassStudioCard(accentColor = accentColor) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AUDIO STEREO LEBAR 3D",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "EKSPANSI SUARA KIRI & KANAN",
                        color = accentColor,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Led button indicator
                Button(
                    onClick = { viewModel.toggleStereoWidener() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activePreset.isStereoWidenerOn) accentColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f),
                        contentColor = if (activePreset.isStereoWidenerOn) accentColor else Color.Gray
                    ),
                    border = BorderStroke(1.dp, if (activePreset.isStereoWidenerOn) accentColor else Color.White.copy(alpha = 0.1f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = if (activePreset.isStereoWidenerOn) "LEBAR AKTIF" else "BYPASS NORMAL",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Large numeric display
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.02f))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = String.format("%.2f", activePreset.stereoWidth),
                        fontSize = 20.sp,
                        color = if (activePreset.isStereoWidenerOn) accentColor else Color.LightGray,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "SKALA LEBAR",
                        fontSize = 8.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (activePreset.stereoWidth > 1.8f) "KIRI & KANAN SUPER LEBAR" else "ATUR PELEBARAN PANNING L/R",
                            fontSize = 10.sp,
                            color = Color.LightGray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Slider(
                        value = activePreset.stereoWidth,
                        valueRange = 0.0f..3.5f,
                        onValueChange = { 
                            if (it > 1.0f && !activePreset.isStereoWidenerOn) {
                                viewModel.toggleStereoWidener()
                            }
                            viewModel.updateStereoWidth(it) 
                        },
                        colors = SliderDefaults.colors(
                            activeTrackColor = accentColor,
                            thumbColor = accentColor,
                            inactiveTrackColor = Color.White.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // A sleek, multi-line audio wave visual indicator of width expander
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF04060A))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val centerY = h / 2f
                    val wideValue = if (activePreset.isStereoWidenerOn) activePreset.stereoWidth else 1.0f
                    
                    // Left wave line
                    val lOffset = w * 0.15f
                    val rOffset = w * 0.85f
                    val scaleDist = (w * 0.3f * (wideValue / 3.5f))
                    
                    // Let's draw outer boundaries (L & R expand visual pointers)
                    drawLine(
                        color = if (activePreset.isStereoWidenerOn) accentColor.copy(alpha = 0.4f) else Color.Gray.copy(alpha = 0.15f),
                        start = Offset(centerY, centerY),
                        end = Offset(Math.max(lOffset - scaleDist, 4.dp.toPx()), centerY),
                        strokeWidth = 2.dp.toPx()
                    )
                    
                    // Left arrowhead
                    drawCircle(
                        color = if (activePreset.isStereoWidenerOn) accentColor else Color.Gray.copy(alpha = 0.2f),
                        radius = 4.dp.toPx(),
                        center = Offset(Math.max(lOffset - scaleDist, 4.dp.toPx()), centerY)
                    )

                    // Right wave line
                    drawLine(
                        color = if (activePreset.isStereoWidenerOn) accentColor.copy(alpha = 0.4f) else Color.Gray.copy(alpha = 0.15f),
                        start = Offset(w - centerY, centerY),
                        end = Offset(Math.min(rOffset + scaleDist, w - 4.dp.toPx()), centerY),
                        strokeWidth = 2.dp.toPx()
                    )

                    // Right arrowhead
                    drawCircle(
                        color = if (activePreset.isStereoWidenerOn) accentColor else Color.Gray.copy(alpha = 0.2f),
                        radius = 4.dp.toPx(),
                        center = Offset(Math.min(rOffset + scaleDist, w - 4.dp.toPx()), centerY)
                    )

                    // Draw some cool holographic green ticks inside representation
                    val numTicks = 20
                    for (k in 0..numTicks) {
                        val xPos = w * (k.toFloat() / numTicks)
                        val distanceFromCenter = Math.abs(xPos - (w / 2f))
                        val isLit = (distanceFromCenter < (w / 2f * (wideValue / 3.5f))) && activePreset.isStereoWidenerOn
                        
                        drawLine(
                            color = if (isLit) accentColor.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.1f),
                            start = Offset(xPos, centerY - 4.dp.toPx()),
                            end = Offset(xPos, centerY + 4.dp.toPx()),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
            }
        }
    }
}

data class SourceItem(val source: AudioEngine.InputSource, val title: String, val desc: String, val icon: ImageVector)

// ==========================================
// 2. MIXER CONSOLE VIEW
// ==========================================
@Composable
fun MixerConsoleView(viewModel: AudioViewModel, accentColor: Color) {
    val gain by viewModel.channelGain.collectAsState()
    val master by viewModel.masterOutput.collectAsState()
    val pan by viewModel.channelPan.collectAsState()
    val isStereo by viewModel.isStereoMode.collectAsState()
    val isMuted by viewModel.isMute.collectAsState()
    val isSoloed by viewModel.isSolo.collectAsState()
    
    val vuLeft by viewModel.streamVuLeft.collectAsState()
    val vuRight by viewModel.streamVuRight.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Name & Deck header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DIGITAL MIXING CONSOLE",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
                Text(
                    text = "5-CHANNEL STUDIO STRIP",
                    fontSize = 11.sp,
                    color = accentColor,
                    fontFamily = FontFamily.Monospace
                )
            }
            
            // Stereo/Mono indicator pill
            Button(
                onClick = { viewModel.toggleStereoMode() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isStereo) accentColor.copy(alpha = 0.15f) else Color.DarkGray,
                    contentColor = if (isStereo) accentColor else Color.Gray
                ),
                border = BorderStroke(1.dp, if (isStereo) accentColor else Color.Transparent),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isStereo) "STEREO PLAY" else "MONO SUM",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Live Mixer Strip Card
        GlassStudioCard(accentColor = accentColor) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Fader 1: Preamp Input Gain
                MixerFader(
                    label = "INPUT",
                    value = gain,
                    range = 0.0f..1.5f,
                    onValueChange = { viewModel.updateInputGain(it) },
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )

                // Fader 2: AUX REVERB SEND
                val activePres by viewModel.activePreset.collectAsState()
                MixerFader(
                    label = "REVERB",
                    value = if (activePres.isReverbOn) activePres.reverbLevel else 0f,
                    range = 0f..1f,
                    onValueChange = { viewModel.updateReverbValues(it, activePres.reverbDecay) },
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )

                // Fader 3: FX DELAY SEND
                MixerFader(
                    label = "DELAY",
                    value = if (activePres.isDelayOn) activePres.delayFeedback else 0f,
                    range = 0f..0.8f,
                    onValueChange = { viewModel.updateDelayValues(activePres.delayTimeMs, it) },
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )

                // Fader 4: MIX MASTER OUT
                MixerFader(
                    label = "MASTER",
                    value = master,
                    range = 0.0f..1.1f,
                    onValueChange = { viewModel.updateMasterOutput(it) },
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )

                // Analog VU Meters (L and R)
                Row(
                    modifier = Modifier
                        .weight(1.2f)
                        .padding(start = 6.dp)
                ) {
                    VuMeter(level = vuLeft, label = "L", modifier = Modifier.weight(1f))
                    VuMeter(level = vuRight, label = "R", modifier = Modifier.weight(1f))
                }
            }
        }

        // Panning potentiometer & Aux Master knobs
        GlassStudioCard(accentColor = accentColor) {
            Text(
                text = "STEREO IMAGE & BUS MASTERING",
                color = Color.LightGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val activePres by viewModel.activePreset.collectAsState()

                // 1. Pan adjustment
                StudioKnob(
                    value = pan,
                    range = -1f..1f,
                    onValueChange = { viewModel.updatePan(it) },
                    label = "BAL PAN ${String.format("%.1f", pan)}",
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )

                // 2. Room Decay adjuster
                StudioKnob(
                    value = activePres.reverbDecay,
                    range = 0.5f..5.0f,
                    onValueChange = { viewModel.updateReverbValues(activePres.reverbLevel, it) },
                    label = "REVB DECAY",
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                )

                // 3. Stereo Width adjuster (Indonesian: Stereo Lebar)
                StudioKnob(
                    value = activePres.stereoWidth,
                    range = 0.0f..3.5f,
                    onValueChange = { 
                        // Automatically enable widener if they start turning it up!
                        if (it > 1.0f && !activePres.isStereoWidenerOn) {
                            viewModel.toggleStereoWidener()
                        }
                        viewModel.updateStereoWidth(it) 
                    },
                    label = "WIDE L/R ${String.format("%.1f", activePres.stereoWidth)}x",
                    accentColor = if (activePres.isStereoWidenerOn) accentColor else Color.Gray,
                    modifier = Modifier.weight(1.2f)
                )

                // Quick Studio Mutes / Solos
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = { viewModel.toggleMute() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isMuted) LedRed.copy(alpha = 0.2f) else LedOffColor,
                            contentColor = if (isMuted) LedRed else Color.Gray
                        ),
                        border = BorderStroke(1.dp, if (isMuted) LedRed else Color.Transparent),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Text("MUTE CH", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }

                    Button(
                        onClick = { viewModel.toggleSolo() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSoloed) LedYellow.copy(alpha = 0.2f) else LedOffColor,
                            contentColor = if (isSoloed) LedYellow else Color.Gray
                        ),
                        border = BorderStroke(1.dp, if (isSoloed) LedYellow else Color.Transparent),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Text("SOLO BUS", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

/**
 * Vertical Mixer Slider Strip Component
 */
@Composable
fun MixerFader(
    label: String,
    value: Float,
    range: ClosedRange<Float>,
    onValueChange: (Float) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.LightGray,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .width(42.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF0C0F16))
                .border(1.dp, BorderColor, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Draw analog fader vertical groove on backgrounds
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(Color.Black)
            )

            Slider(
                value = value,
                valueRange = range.start..range.endInclusive,
                onValueChange = onValueChange,
                colors = SliderDefaults.colors(
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    thumbColor = accentColor
                ),
                modifier = Modifier
                    .fillMaxHeight()
                    .testTag("fader_${label.lowercase()}")
                    .graphicsLayer(rotationZ = 270f)
            )
        }

        Text(
            text = String.format("%.2f", value),
            fontSize = 11.sp,
            color = Color.Gray,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

// ==========================================
// 3. GRAPHIC EQUALIZER MODULE
// ==========================================
@Composable
fun EqualizerView(viewModel: AudioViewModel, accentColor: Color) {
    val activeMode by viewModel.activeEqMode.collectAsState()
    val activePreset by viewModel.activePreset.collectAsState()
    val presetsList by viewModel.presetsState.collectAsState()

    // Loaded band gains
    val gains7 by viewModel.currentGains7.collectAsState()
    val gains15 by viewModel.currentGains15.collectAsState()
    val gains31 by viewModel.currentGains31.collectAsState()

    var customPresetName by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // EQ Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PARAMETRIC EQUALIZER",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
                Text(
                    text = "ACTIVE PROFILE: ${activePreset.name}",
                    fontSize = 11.sp,
                    color = accentColor,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Selection of EQ bands count tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(7, 15, 31).forEach { bandMode ->
                Button(
                    onClick = { viewModel.setEqMode(bandMode) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeMode == bandMode) accentColor else PanelBg,
                        contentColor = if (activeMode == bandMode) DarkBg else Color.LightGray
                    ),
                    border = BorderStroke(1.dp, if (activeMode == bandMode) accentColor else BorderColor)
                ) {
                    Text(
                        text = "$bandMode BANDS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Render dynamic EQ Sliders container
        GlassStudioCard(accentColor = accentColor) {
            Text(
                text = "MANUAL DEK EQ CONST (dB)",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Depending on active mode, pull proper list
            val eqList = when (activeMode) {
                7 -> gains7
                15 -> gains15
                else -> gains31
            }

            val frequencies = when (activeMode) {
                7 -> listOf("60Hz", "150Hz", "400Hz", "1kHz", "3kHz", "8kHz", "15kHz")
                15 -> listOf("31", "63", "125", "250", "500", "1K", "2K", "4K", "6K", "8K", "10K", "12K", "14K", "16K", "18K")
                else -> listOf(
                     "20", "25", "31", "40", "50", "63", "80", "100", "125", "160", "200", "250", "315", "400", "500", "630", "800", "1K", "1.2K", "1.6K", "2K", "2.5K", "3.1K", "4K", "5K", "6.3K", "8K", "10K", "12.5K", "16K", "20K"
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                // Background visualizer for high-tech aesthetic
                val visualBuffer by viewModel.spectrumBuffer.collectAsState()
                RealtimeSpectrumVisualizer(
                    buffer = visualBuffer,
                    accentColor = accentColor.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxSize()
                )

                // The scrolling band slider strips
                LazyRow(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(eqList) { idx, gainVal ->
                        val label = if (idx < frequencies.size) frequencies[idx] else ""
                        EqVerticalSlider(
                            freqLabel = label,
                            gainVal = gainVal,
                            onValueChange = { viewModel.updateEqBand(idx, it) },
                            accentColor = accentColor
                        )
                    }
                }
            }
        }

        // Save preset panel
        GlassStudioCard(accentColor = accentColor) {
            Text(
                text = "STORE CUSTOM EQUALIZER PRESET",
                color = Color.LightGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = customPresetName,
                    onValueChange = { customPresetName = it },
                    placeholder = { Text("e.g., Space Rock Extreme", fontSize = 12.sp, color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                    ),
                    textStyle = TextStyle(fontSize = 13.sp),
                    modifier = Modifier.weight(1.5f).height(50.dp)
                )

                Button(
                    onClick = {
                        if (customPresetName.trim().isNotEmpty()) {
                            viewModel.saveAsNewCustomPreset(customPresetName)
                            customPresetName = ""
                            keyboardController?.hide()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save Icon",
                        tint = DarkBg,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SAVE", fontSize = 11.sp, color = DarkBg, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Integrated Preset List/Buttons Manager
        GlassStudioCard(accentColor = accentColor) {
            Text(
                text = "SYSTEM & USER PRESETS",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                mainAxisSpacing = 8.dp,
                crossAxisSpacing = 8.dp
            ) {
                presetsList.forEach { p ->
                    val isCurrent = activePreset.name == p.name
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isCurrent) accentColor.copy(alpha = 0.15f) else Color(0xFF1E2435))
                            .border(1.dp, if (isCurrent) accentColor else Color.Transparent, RoundedCornerShape(6.dp))
                            .clickable { viewModel.applyLoadedPreset(p) }
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = p.name,
                            color = if (isCurrent) accentColor else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (p.isUserPreset) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete preset",
                                tint = LedRed,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { viewModel.deletePreset(p.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom Simple wrapping flow row layout for tags
 */
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    mainAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    crossAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        val layoutWidth = constraints.maxWidth
        
        var rowWidth = 0
        var rowHeight = 0
        var totalHeight = 0
        
        class PlaceableInfo(val placeable: androidx.compose.ui.layout.Placeable, val x: Int, val y: Int)
        val list = mutableListOf<PlaceableInfo>()
        
        var currentX = 0
        var currentY = 0
        var currentLineHeight = 0
        
        placeables.forEach { p ->
            val spacingX = mainAxisSpacing.roundToPx()
            val spacingY = crossAxisSpacing.roundToPx()
            
            if (currentX + p.width > layoutWidth) {
                // Shift line
                currentX = 0
                currentY += currentLineHeight + spacingY
                currentLineHeight = 0
            }
            
            list.add(PlaceableInfo(p, currentX, currentY))
            currentX += p.width + spacingX
            currentLineHeight = maxOf(currentLineHeight, p.height)
            totalHeight = maxOf(totalHeight, currentY + currentLineHeight)
        }
        
        layout(layoutWidth, totalHeight) {
            list.forEach { info ->
                info.placeable.place(info.x, info.y)
            }
        }
    }
}

@Composable
fun EqVerticalSlider(
    freqLabel: String,
    gainVal: Float,
    onValueChange: (Float) -> Unit,
    accentColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxHeight()
            .width(42.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .width(28.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .border(1.dp, BorderColor, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .background(Color.DarkGray)
            )

            Slider(
                value = gainVal,
                valueRange = -12f..15f,
                onValueChange = onValueChange,
                colors = SliderDefaults.colors(
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    thumbColor = accentColor
                ),
                modifier = Modifier
                    .fillMaxHeight()
                    .graphicsLayer(rotationZ = 270f)
            )
        }

        Text(
            text = freqLabel,
            fontSize = 9.sp,
            color = Color.LightGray,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = "${gainVal.toInt()}d",
            fontSize = 8.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 1.dp)
        )
    }
}

// ==========================================
// 4. EFFECTS RACK MODULE
// ==========================================
@Composable
fun EffectsRackView(viewModel: AudioViewModel, accentColor: Color) {
    val activePreset by viewModel.activePreset.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // FX Rack Title
        Column {
            Text(
                text = "PROFESSIONAL DSP EFFECTS RACK",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )
            Text(
                text = "BYPASS CONTROL & REALTIME COEFFICIENTS",
                fontSize = 11.sp,
                color = accentColor,
                fontFamily = FontFamily.Monospace
            )
        }

        // Reverb Module Card
        FxRackModule(
            title = "WARM REVERB CHAMBER",
            isOn = activePreset.isReverbOn,
            onToggle = { viewModel.toggleReverb() },
            accentColor = accentColor
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("MIX LEVEL: ${(activePreset.reverbLevel * 100).toInt()}%", fontSize = 11.sp, color = Color.Gray)
                    Slider(
                        value = activePreset.reverbLevel,
                        onValueChange = { viewModel.updateReverbValues(it, activePreset.reverbDecay) },
                        colors = SliderDefaults.colors(activeTrackColor = accentColor, thumbColor = accentColor)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("DECAY TIME: ${String.format("%.1f", activePreset.reverbDecay)}s", fontSize = 11.sp, color = Color.Gray)
                    Slider(
                        value = activePreset.reverbDecay,
                        valueRange = 0.5f..5.0f,
                        onValueChange = { viewModel.updateReverbValues(activePreset.reverbLevel, it) },
                        colors = SliderDefaults.colors(activeTrackColor = accentColor, thumbColor = accentColor)
                    )
                }
            }
        }

        // Delay / Echo Module Card
        FxRackModule(
            title = "STEREO TAPE ECHO DELAY",
            isOn = activePreset.isDelayOn,
            onToggle = { viewModel.toggleDelay() },
            accentColor = accentColor
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("TIME INTERVAL: ${activePreset.delayTimeMs} ms", fontSize = 11.sp, color = Color.Gray)
                    Slider(
                        value = activePreset.delayTimeMs.toFloat(),
                        valueRange = 100f..1000f,
                        onValueChange = { viewModel.updateDelayValues(it.toInt(), activePreset.delayFeedback) },
                        colors = SliderDefaults.colors(activeTrackColor = accentColor, thumbColor = accentColor)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("FEEDBACK RATIO: ${(activePreset.delayFeedback * 100).toInt()}%", fontSize = 11.sp, color = Color.Gray)
                    Slider(
                        value = activePreset.delayFeedback,
                        valueRange = 0.1f..0.8f,
                        onValueChange = { viewModel.updateDelayValues(activePreset.delayTimeMs, it) },
                        colors = SliderDefaults.colors(activeTrackColor = accentColor, thumbColor = accentColor)
                    )
                }
            }
        }

        // Bass Booster Module Card
        FxRackModule(
            title = "SUBBASS GENERATOR & HARMONIZER",
            isOn = activePreset.isBassBoostOn,
            onToggle = { viewModel.toggleBassBoost() },
            accentColor = accentColor
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1.5f)) {
                    Text("BASS GAIN SCALE: ${(activePreset.bassBoostValue * 10).toInt()} dB", fontSize = 11.sp, color = Color.Gray)
                    Slider(
                        value = activePreset.bassBoostValue,
                        onValueChange = { viewModel.updateBassBoostValue(it) },
                        colors = SliderDefaults.colors(activeTrackColor = accentColor, thumbColor = accentColor)
                    )
                }
                Box(
                    modifier = Modifier.weight(1f).padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (activePreset.isBassBoostOn) accentColor.copy(alpha = 0.3f) else Color.DarkGray)
                    )
                }
            }
        }

        // Compressor Module Card
        FxRackModule(
            title = "DYNAMIC DUMMY COMPRESSOR",
            isOn = activePreset.isCompressorOn,
            onToggle = { viewModel.toggleCompressor() },
            accentColor = accentColor
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("THRESHOLD: ${activePreset.compThreshold.toInt()} dB", fontSize = 11.sp, color = Color.Gray)
                    Slider(
                        value = activePreset.compThreshold,
                        valueRange = -40f..0f,
                        onValueChange = { viewModel.updateCompressorValues(it, activePreset.compRatio) },
                        colors = SliderDefaults.colors(activeTrackColor = accentColor, thumbColor = accentColor)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("RATIO: ${activePreset.compRatio.toInt()}:1", fontSize = 11.sp, color = Color.Gray)
                    Slider(
                        value = activePreset.compRatio,
                        valueRange = 1f..10f,
                        onValueChange = { viewModel.updateCompressorValues(activePreset.compThreshold, it) },
                        colors = SliderDefaults.colors(activeTrackColor = accentColor, thumbColor = accentColor)
                    )
                }
            }
        }

        // 3D Hyper-Stereo Widener Module Card (Indonesian: Stereo Lebar)
        FxRackModule(
            title = "KONTROL DIMENSI STEREO LEBAR (3D)",
            isOn = activePreset.isStereoWidenerOn,
            onToggle = { viewModel.toggleStereoWidener() },
            accentColor = accentColor
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LEBAR STEREO: ${String.format("%.1f", activePreset.stereoWidth)}x",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (activePreset.isStereoWidenerOn) {
                            if (activePreset.stereoWidth > 1.8f) "SUPER LEBAR (KIRI & KANAN MAX)"
                            else if (activePreset.stereoWidth > 1.0f) "MODE LEBAR AKTIF"
                            else "MONO FIELD"
                        } else "BYPASS NORMAL",
                        fontSize = 10.sp,
                        color = if (activePreset.isStereoWidenerOn) accentColor else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                
                // Beautiful fluid Material Slider allowing scaling from 0.0f to 3.5x width!
                Slider(
                    value = activePreset.stereoWidth,
                    valueRange = 0.0f..3.5f,
                    onValueChange = { viewModel.updateStereoWidth(it) },
                    colors = SliderDefaults.colors(
                        activeTrackColor = accentColor,
                        thumbColor = accentColor,
                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Left-Right Outwards Active Graphic bars to visually represent Stereo Width separation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color(0xFF06090F))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(7.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val scalePercent = (activePreset.stereoWidth / 3.5f).coerceIn(0f, 1f)
                    
                    // Left Expansion Bar (flowing left)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(scalePercent)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            if (activePreset.isStereoWidenerOn) accentColor.copy(alpha = 0.7f) else Color.Gray.copy(alpha = 0.2f)
                                        )
                                    )
                                )
                        )
                    }

                    // Center Axis
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(Color.White.copy(alpha = 0.3f))
                    )

                    // Right Expansion Bar (flowing right)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(scalePercent)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            if (activePreset.isStereoWidenerOn) accentColor.copy(alpha = 0.7f) else Color.Gray.copy(alpha = 0.2f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("KIRI (MONO)", fontSize = 9.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                    Text("ASLI (1.0x)", fontSize = 9.sp, color = Color.White.copy(alpha = 0.4f), fontFamily = FontFamily.Monospace)
                    Text("SANGAT LEBAR (L&R)", fontSize = 9.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Additional Studio Effects (Limiter, Noise Gate, 3D Spatial)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Noise Gate Toggle Button Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.toggleNoiseGate() }
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF07090E).copy(alpha = 0.55f))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.White.copy(alpha = 0.02f)
                            )
                        )
                    )
                    .border(1.dp, if (activePreset.isNoiseGateOn) accentColor.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FilterAlt,
                        contentDescription = "Noise Gate",
                        tint = if (activePreset.isNoiseGateOn) accentColor else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("NOISE GATE", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (activePreset.isNoiseGateOn) "BYPASS OFF" else "BYPASS ON",
                        fontSize = 9.sp,
                        color = if (activePreset.isNoiseGateOn) accentColor else Color.Gray
                    )
                }
            }

            // Tube Distortion Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.toggleDistortion() }
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF07090E).copy(alpha = 0.55f))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.White.copy(alpha = 0.02f)
                            )
                        )
                    )
                    .border(1.dp, if (activePreset.isDistortionOn) accentColor.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Distortion",
                        tint = if (activePreset.isDistortionOn) accentColor else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("HARMONICS", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (activePreset.isDistortionOn) "OVERDRIVE" else "CLEAN",
                        fontSize = 9.sp,
                        color = if (activePreset.isDistortionOn) accentColor else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun FxRackModule(
    title: String,
    isOn: Boolean,
    onToggle: () -> Unit,
    accentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassStudioCard(accentColor = accentColor) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (isOn) "OPERATING ENGINE ACTIVE" else "BYPASSED",
                    fontSize = 10.sp,
                    color = if (isOn) accentColor else Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Futuristic LED Power On Toggle button
            Button(
                onClick = onToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isOn) accentColor else LedOffColor,
                    contentColor = if (isOn) DarkBg else Color.Gray
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isOn) "ACTIVE" else "BYPASS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

// ==========================================
// 5. AUDIO VISUALIZER PAGE
// ==========================================
@Composable
fun VisualizerPageView(viewModel: AudioViewModel, accentColor: Color) {
    val visualBuffer by viewModel.spectrumBuffer.collectAsState()
    val isPlaying by viewModel.isMusicPlaying.collectAsState()
    val vuLeft by viewModel.streamVuLeft.collectAsState()

    val colorMenuHexList = listOf("#00FFFF", "#FF00FF", "#00FF66", "#FFA500", "#FF2233")
    val colorMenuNames = listOf("CYAN", "MAGENTA", "LIME", "ORANGE", "RED")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "NEON CHROMATIC VISUALIZER",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )
            Text(
                text = "RGB SPECTRUM & PCM WAVE SHAPES",
                fontSize = 11.sp,
                color = accentColor,
                fontFamily = FontFamily.Monospace
            )
        }

        // Full size interactive Canvas Visualizer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .background(Color(0xFF030509)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Draw central visualizer halo loop reacting to music levels
                val centerRadius = (height * 0.25f) + (vuLeft * 80f)
                val baseCenter = Offset(width / 2f, height / 2f)

                drawCircle(
                    color = accentColor.copy(alpha = 0.05f),
                    radius = centerRadius + 20.dp.toPx(),
                    center = baseCenter
                )

                drawCircle(
                    color = accentColor.copy(alpha = 0.12f),
                    radius = centerRadius,
                    center = baseCenter,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Draw a wavy PCM linear grid
                val pointsCount = 48
                val sliceWidth = width / pointsCount
                val innerPathY = height / 2f

                for (p in 0 until pointsCount - 1) {
                    val rawAmp = if (p < visualBuffer.size) visualBuffer[p] else 0.1f
                    val waveHeight1 = rawAmp * 110f * sin(p * 0.45).toFloat()
                    val waveHeight2 = rawAmp * 110f * sin((p + 1) * 0.45).toFloat()

                    drawLine(
                        color = accentColor.copy(alpha = 0.75f),
                        start = Offset(p * sliceWidth, innerPathY + waveHeight1),
                        end = Offset((p + 1) * sliceWidth, innerPathY + waveHeight2),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
            
            if (!isPlaying) {
                Text(
                    text = "[CONNECT SOURCE & PLAY MUSIC]",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Chromatic Color Accent Picker panel
        GlassStudioCard(accentColor = accentColor) {
            Text(
                text = "ACCENT LIGHTING RIG THEME",
                color = Color.LightGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                colorMenuHexList.forEachIndexed { idx, hexVal ->
                    val isChosen = hexVal.lowercase() == viewModel.themeColorHex.value.lowercase()
                    val currentRGBColor = Color(android.graphics.Color.parseColor(hexVal))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(currentRGBColor)
                                .border(
                                    BorderStroke(2.dp, if (isChosen) Color.White else Color.Transparent),
                                    CircleShape
                                )
                                .clickable { viewModel.changeThemeAccentHex(hexVal) }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = colorMenuNames[idx],
                            fontSize = 8.sp,
                            color = if (isChosen) accentColor else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. SETTINGS VIEW MODULE
// ==========================================
@Composable
fun SettingsPageView(viewModel: AudioViewModel, accentColor: Color) {
    var isFloatingOn by remember { mutableStateOf(false) }
    var sampleRateIdx by remember { mutableStateOf(0) }
    var bufferIdx by remember { mutableStateOf(2) }

    val sampleRates = listOf("44100 Hz (CD Direct)", "48000 Hz (Broadcast)", "96000 Hz (Ultra High)")
    val bufferSizes = listOf("128 Frames (Ultra low latency)", "256 Frames (Aggressive)", "1024 Frames (Relaxed)", "2048 Frames (High Buffer)")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "AUDIO SUBSYSTEM SETTINGS",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )
            Text(
                text = "ENGINE CONFIGS & SYSTEM DECK LAYOUTS",
                fontSize = 11.sp,
                color = accentColor,
                fontFamily = FontFamily.Monospace
            )
        }

        // Subsystem Hardware configurations
        GlassStudioCard(accentColor = accentColor) {
            Text(
                text = "DSP SAMPLING RATE",
                fontSize = 11.sp,
                color = Color.LightGray,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            sampleRates.forEachIndexed { idx, rateText ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { sampleRateIdx = idx }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = rateText, color = Color.White, fontSize = 13.sp)
                    RadioButton(
                        selected = sampleRateIdx == idx,
                        onClick = { sampleRateIdx = idx },
                        colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                    )
                }
            }
        }

        // Frame Buffer Configurations
        GlassStudioCard(accentColor = accentColor) {
            Text(
                text = "HARDWARE DMA BUFFER FRAMES",
                fontSize = 11.sp,
                color = Color.LightGray,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            bufferSizes.forEachIndexed { idx, bufText ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { bufferIdx = idx }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = bufText, color = Color.White, fontSize = 13.sp)
                    RadioButton(
                        selected = bufferIdx == idx,
                        onClick = { bufferIdx = idx },
                        colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                    )
                }
            }
        }

        // Floating Overlay deck controller
        GlassStudioCard(accentColor = accentColor) {
             Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FLOATING MINI PLAYER DECK",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Show mini controller over other apps",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
                
                Switch(
                    checked = isFloatingOn,
                    onCheckedChange = {
                        isFloatingOn = it
                        viewModel.isMiniPlayerVisible.value = it
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = accentColor, checkedTrackColor = accentColor.copy(alpha = 0.4f))
                )
            }
        }

        // About & Specifications checklist
        GlassStudioCard(accentColor = accentColor) {
            Text(
                text = "ENGINE INFORMATION MODULE",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "• Core System: OpenSL ES Thread-Pooled Latency Loop", color = Color.Gray, fontSize = 11.sp)
                Text(text = "• Capture API: Android AudioPlay Capture Protocol Mode active", color = Color.Gray, fontSize = 11.sp)
                Text(text = "• DSP Thread: Multi-core worker pools executing floats", color = Color.Gray, fontSize = 11.sp)
                Text(text = "• Release Version: BRO MUSIK EQ Studio PRO v1.07", color = Color.Gray, fontSize = 11.sp)
                Text(text = "• Audio Sandbox compliance: Fully verified sandbox", color = Color.Gray, fontSize = 11.sp)
            }
        }
    }
}

// ==========================================
// FLOATING MINIPLAYER COMPONENT
// ==========================================
@Composable
fun FloatingMiniPlayer(
    viewModel: AudioViewModel,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val isPlaying by viewModel.isMusicPlaying.collectAsState()
    val trackTitle by viewModel.trackTitle.collectAsState()

    Card(
        modifier = modifier
            .width(180.dp)
            .shadow(12.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF07090E).copy(alpha = 0.75f))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.04f)
                    )
                )
            )
            .border(1.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Drag head logo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EQ MINI PLAYER",
                    fontSize = 10.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close overlay",
                    tint = Color.Gray,
                    modifier = Modifier
                        .size(14.dp)
                        .clickable { viewModel.isMiniPlayerVisible.value = false }
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = trackTitle,
                fontSize = 12.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { viewModel.skipPrevious() }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.SkipPrevious, "Prev", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                }
                
                IconButton(
                    onClick = { viewModel.togglePlayer() }, 
                    modifier = Modifier
                        .size(28.dp)
                        .background(accentColor, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play Control",
                        tint = DarkBg,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = { viewModel.skipNext() }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.SkipNext, "Next", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
