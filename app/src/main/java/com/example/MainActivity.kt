package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.AudioEngine
import com.example.ui.DashboardScreen
import com.example.ui.DarkBg
import com.example.ui.theme.MyApplicationTheme
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: com.example.ui.viewmodel.AudioViewModel = viewModel()
                var showSplash by remember { mutableStateOf(true) }

                // Check and handle microphone permissions
                val context = LocalContext.current
                val micPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        viewModel.selectAudioSource(AudioEngine.InputSource.MICROPHONE)
                    } else {
                        Toast.makeText(context, "Microphone permission is required for vocal monitoring loopback!", Toast.LENGTH_LONG).show()
                        viewModel.selectAudioSource(AudioEngine.InputSource.DEMO)
                    }
                }

                // Check Permission helper
                LaunchedEffect(Unit) {
                    // Start checking if we choose mic but redirect if not allowed
                    snapshotFlow { viewModel.audioEngine.getInputSource() }.collect { source ->
                        if (source == AudioEngine.InputSource.MICROPHONE) {
                            val permissionCheck = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            )
                            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    }
                }

                // Splash Timer Animation
                if (showSplash) {
                    StudioSplashScreen(
                        onSplashFinished = { showSplash = false }
                    )
                } else {
                    DashboardScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * High-fidelity Futuristic Splash Screen for BRO MUSIK EQ
 */
@Composable
fun StudioSplashScreen(onSplashFinished: () -> Unit) {
    var loadingStep by remember { mutableStateOf("Readying Audio HAL Layer...") }
    val transition = rememberInfiniteTransition(label = "SplashPulse")
    
    // Scale pulse for the central synthesizer logo orb
    val orbScale by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbScale"
    )

    // Sequence of high-tech initialization logs
    LaunchedEffect(Unit) {
        delay(600)
        loadingStep = "Mounting 31-Band Parametric EQ Filters..."
        delay(600)
        loadingStep = "Synthesizing Studio Drum & Synth Oscillators..."
        delay(550)
        loadingStep = "Loading Professional Reverb Chambers & Clamping Limiters..."
        delay(500)
        loadingStep = "System Ready! Launching BRO MIXER Console..."
        delay(350)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        // Futuristic radar scanning circles
        Box(
            modifier = Modifier
                .size(310.dp)
                .border(2.dp, Color.Cyan.copy(alpha = 0.08f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(220.dp)
                .border(2.dp, Color.Cyan.copy(alpha = 0.12f * orbScale), CircleShape)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Neon logo
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF00FFCC), Color(0xFF001144)),
                            radius = 180f
                        )
                    )
                    .border(3.dp, Color.Cyan, CircleShape)
                    .testTag("splash_logo"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Studio Mic Equalization Wave",
                    tint = Color.White,
                    modifier = Modifier.size(54.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            Text(
                text = "BRO MUSIK EQ",
                fontSize = 28.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 2.sp
            )
            
            Text(
                text = "VIRTUAL MIXING DIGITAL CONSOLE",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color.Cyan,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Pulse loading log text
            Text(
                text = loadingStep.uppercase(),
                fontSize = 10.sp,
                color = Color.Gray,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                color = Color.Cyan,
                trackColor = Color(0xFF141822),
                modifier = Modifier
                    .width(160.dp)
                    .height(4.dp)
                    .clip(CircleShape)
            )
        }
    }
}
