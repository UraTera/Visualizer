package com.tera.visualizer

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.tera.visualizer.databinding.ActivityMainBinding
import kotlin.math.atan
import kotlin.math.sin


class MainActivity : AppCompatActivity() {

    companion object {
        const val VALUE_FREQ = "value_freq"
        const val STYLE = "style"
        const val MIN = 200f
        const val MAX = 5000f
    }

    private lateinit var binding: ActivityMainBinding

    private var player: MediaPlayer? = null
    private lateinit var audioManager: AudioManager
    private var visualizer: VisualizerManager? = null
    private var track: AudioTrack? = null
    private val sampleRate: Int = 44100
    private var buffLength = 0
    private var channelConfig = AudioFormat.CHANNEL_OUT_MONO
    private var audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private var frequency = 1000
    private var maxVolume = 0
    private var currVolume = 0
    private var posVolume = 0f
    private var isGenerator = false
    private var valueFreq = 0f
    private var style = 0

    private lateinit var sp: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sp = getSharedPreferences("settings", MODE_PRIVATE)
        valueFreq = sp.getFloat(VALUE_FREQ, MIN)
        style = sp.getInt(STYLE, 0)

        initButton()
        initVolume()

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        } else {
            initVisualizer()
        }
    }

    private fun initVisualizer(){
        visualizer = VisualizerManager()
        visualizer!!.init(0, binding.vsView)
        setParams()
    }

    // Кнопки и плеер
    private fun initButton() = with(binding) {
        player = MediaPlayer.create(this@MainActivity, R.raw.demo)

        imPlay.setOnClickListener {
            if (player!!.isPlaying) {
                imPlay.setImageResource(R.drawable.ic_play)
                player?.pause()
                visualizer?.stop()
            } else {
                if (!isGenerator) {
                    imPlay.setImageResource(R.drawable.ic_pause)
                    player?.start()
                    if (visualizer == null)
                        initVisualizer()
                    visualizer?.start()
                }
            }
        }

        rgStyle.setOnCheckedChangeListener { _, i ->
            when (i) {
                R.id.rbWave -> {
                    visualizer!!.style = 0
                    style = 0
                }
                R.id.rbBar -> {
                    visualizer!!.style = 1
                    style = 1
                }
                R.id.rbSeg -> {
                    visualizer!!.style = 2
                    style = 2
                }
            }
        }

        // Кнопка Генератор
        imGenerator.setOnClickListener {
            // Включен
            if (isGenerator) {
                imGenerator.setImageResource(R.drawable.ic_play)
                isGenerator = false
                track!!.stop()
                track!!.release()
                visualizer!!.stop()
            } else {
                if (!player!!.isPlaying) {
                    imGenerator.setImageResource(R.drawable.ic_pause)
                    isGenerator = true
                    visualizer!!.start()
                    Thread {
                        initTrack()
                        track!!.play()
                        wave()
                    }.start()
                }
            }
        }
    }

    // AudioTrack
    private fun initTrack() {
        buffLength = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(buffLength)
            .build()
    }

    // Синусоидальный сигнал
    private fun wave() {
        val frameOut = ShortArray(buffLength)
        val amplitude = 32767
        val twopi = 8.0 * atan(1.0)
        var phase = 0.0

        while (isGenerator) {
            for (i in 0 until buffLength) {
                frameOut[i] = (amplitude * sin(phase)).toInt().toShort()
                phase += twopi * frequency / sampleRate
                if (phase > twopi) {
                    phase -= twopi
                }
            }
            if (track != null &&
                track!!.state == AudioTrack.STATE_INITIALIZED &&
                track!!.playState == AudioTrack.PLAYSTATE_PLAYING
            ) {
                track!!.write(frameOut, 0, buffLength)
            }

        }
    }

    // Громкость, Частота
    private fun initVolume() = with(binding) {
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        // Установите максимальную громкость
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        // Установите текущую громкость
        currVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        slVolume.valueMax = maxVolume.toFloat()
        slVolume.value = currVolume.toFloat()
        posVolume = slVolume.value

        // Громкость
        slVolume.setOnChangeListener {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, it.toInt(), 0)
            posVolume = it
            visualizer!!.volume = it
            setParams()
        }

        // Частота
        slFreq.valueMin = MIN
        slFreq.valueMax = MAX

        slFreq.setOnChangeListener {
            valueFreq = it
            val str = valueFreq.toInt().toString()
            tvFreq.text = str

            runOnUiThread {
                frequency = valueFreq.toInt()
            }
        }

    }

    private fun setParams() = with(binding) {
        val percent = posVolume * 100 / maxVolume
        val volume = percent.toInt().toString() + " %"
        tvVolume.text = volume

        slFreq.value = valueFreq
        val str = valueFreq.toInt().toString()
        tvFreq.text = str
        frequency = valueFreq.toInt()
        visualizer!!.style = style

        when (style) {
            0 -> rbWave.isChecked = true
            1 -> rbBar.isChecked = true
            2 -> rbSeg.isChecked = true
        }

    }

    override fun onStop() {
        super.onStop()
        sp.edit {
            putFloat(VALUE_FREQ, valueFreq)
            putInt(STYLE, style)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        visualizer?.release()
        track?.release()
    }
}