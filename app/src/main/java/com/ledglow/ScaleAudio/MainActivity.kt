package com.ledglow.ScaleAudio

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView
import kotlinx.coroutines.*
import java.net.Socket
import java.io.OutputStreamWriter

class MainActivity : ComponentActivity() {

    var exoPlayer: ExoPlayer? = null // Public property

    private val serverIP = "192.168.0.238" // Replace with your Pico W IP
    private val serverPort = 80

    private var stopRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        setContent {
            MaterialTheme {
                var showIntro by remember { mutableStateOf(true) }
                var currentImage by remember { mutableStateOf(R.drawable.alpine7909joff) }
                var showVideo by remember { mutableStateOf(false) }

                if (showIntro) {
                    IntroScreen(onStartButtonClick = {
                        showIntro = false
                        currentImage = R.drawable.alpine7909jon
                        showVideo = true
                    })
                } else {
                    ImageWithVideoScreen(
                        currentImage = currentImage,
                        showVideo = showVideo,
                        videoUri = "android.resource://${packageName}/" + R.raw.video,
                        onPlayButtonClick = {
                            playVideo() // Start the video when Play button is pressed
                            CoroutineScope(Dispatchers.IO).launch {
                                resetForNextRun()
                                startLEDCommands("led_commands.txt") // Start LED commands when Play button is pressed
                            }
                        },
                        onStopButtonClick = {
                            stopEverything() // Stop video and LEDs
                            showIntro = true
                        }
                    )
                }
            }
        }
    }

    private fun stopEverything() {
        stopRequested = true
        stopExoPlayer() // Stop the ExoPlayer
        sendCommandToPico("col 0 0 0") // Stop LED by sending command
    }

    private fun stopExoPlayer() {
        exoPlayer?.let {
            it.stop()
            it.release()
        }
        exoPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopExoPlayer() // Ensure the player is stopped and released
    }

    private fun resetForNextRun() {
        stopRequested = false
    }

    private suspend fun startLEDCommands(filePath: String) {
        val commands = assets.open(filePath).bufferedReader().readLines()
        val startTime = System.currentTimeMillis()

        for (line in commands) {
            if (stopRequested) break

            val parts = line.split(" ")
            val timeMs = parts[0].toInt()
            val command = parts.drop(1).joinToString(" ")

            while (System.currentTimeMillis() - startTime < timeMs) {
                if (stopRequested) break
                delay(10)
            }

            if (stopRequested) break

            sendCommandToPico(command)
        }

        if (stopRequested) {
            sendCommandToPico("col 0 0 0")
        }
    }

    private fun sendCommandToPico(command: String) {
        try {
            Socket(serverIP, serverPort).use { socket ->
                val writer = OutputStreamWriter(socket.getOutputStream())
                writer.write(command + "\n")
                writer.flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playVideo() {
        exoPlayer?.let {
            it.playWhenReady = true
        }
    }

    @Composable
    fun IntroScreen(onStartButtonClick: () -> Unit) {
        val scrollState = rememberScrollState()

        Box(modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
        ) {
            Image(
                painter = painterResource(id = R.drawable.alpine7909joff),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(0.dp),
                contentScale = ContentScale.FillWidth
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 34.dp, y = 146.dp)
            ) {
                Button(
                    onClick = onStartButtonClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .size(75.dp, 75.dp)
                ) {
                    Text("", fontSize = 20.sp, color = Color.Transparent)
                }
            }
        }
    }

    @Composable
    fun ImageWithVideoScreen(
        currentImage: Int,
        showVideo: Boolean,
        videoUri: String,
        onPlayButtonClick: () -> Unit,
        onStopButtonClick: () -> Unit
    ) {
        val context = LocalContext.current as MainActivity
        var showThumbnail by remember { mutableStateOf(true) }
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 13.dp)
            ) {
                // Top image
                Image(
                    painter = painterResource(id = currentImage),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2400f / 650f),
                    contentScale = ContentScale.FillWidth
                )

                // Play button
                Button(
                    onClick = onPlayButtonClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = 566.dp, y = 112.dp)
                        .size(38.dp)
                ) {
                    Text("Play", fontSize = 20.sp)
                }

                // Stop button
                Button(
                    onClick = onStopButtonClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-710).dp, y = 128.dp)
                        .size(75.dp)
                ) {
                    Text("Stop", fontSize = 20.sp)
                }
            }

            // Video and thumbnail area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2400f / 650f)
                    .padding(top = 13.dp)
            ) {
                if (showVideo) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                val exoPlayer = ExoPlayer.Builder(ctx).build()
                                context.exoPlayer = exoPlayer // Assign the player to the MainActivity context
                                player = exoPlayer
                                val mediaItem = MediaItem.fromUri(videoUri)
                                exoPlayer.setMediaItem(mediaItem)
                                exoPlayer.prepare()
                                layoutParams = FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    )
                }

                if (showThumbnail) {
                    Image(
                        painter = painterResource(id = R.drawable.thumbnail_image),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}
