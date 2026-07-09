package com.tera.visualizer

import android.media.audiofx.Visualizer
import android.media.audiofx.Visualizer.OnDataCaptureListener

class VisualizerManager {

    private var visualizer: Visualizer? = null

    private var mView: VisualizerView? = null

    fun init(audioSession: Int, view: VisualizerView) {
        mView = view

        visualizer = Visualizer(audioSession)
        visualizer!!.enabled = false
        visualizer!!.captureSize = 512

        // Регистрируем слушатель и указываем, какие данные нам нужны
        visualizer!!.setDataCaptureListener(object : OnDataCaptureListener {

            // Обрабатываем частотный спектр
            override fun onFftDataCapture(visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int) {

                mView!!.setFft(fft)
            }

            // Обрабатываем волновую форму
            override fun onWaveFormDataCapture(
                visualizer: Visualizer?,
                waveform: ByteArray?,
                samplingRate: Int
            ) {
                // Ничего не делать
            }
        }, Visualizer.getMaxCaptureRate(), false, true) // true для waveform, true для fft
    }

    fun start() {
        visualizer!!.enabled = true
    }

    fun stop() {
        visualizer!!.enabled = false
    }

    fun release() {
        visualizer?.enabled = false
        visualizer?.release()
    }

    // Свойства
    var volume: Float = 1f
        set(value) {
            mView!!.volume = value

        }

    var style: Int = 0
        set(value) {
            mView!!.style = value
        }

    var barColor: Int = 0
        set(value) {
            mView!!.barColor = value
        }

    var axisColor: Int = 0
        set(value) {
            mView!!.axisColor = value
        }

    var groundColor: Int = 0
        set(value) {
            mView!!.groundColor = value
        }

    var topColor: Int = 0
        set(value) {
            mView!!.topColor = value
        }

    var columnsNum: Int = 0
        set(value) {
            mView!!.columnsNum = 30
        }

    var segmentHeight: Float = 10f
        set(value) {
            mView!!.segmentHeight = value
        }

}