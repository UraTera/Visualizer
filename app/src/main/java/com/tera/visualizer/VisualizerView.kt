package com.tera.visualizer

import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.media.AudioManager
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.core.content.withStyledAttributes
import kotlin.math.sqrt


class VisualizerView(
    context: Context,
    attrs: AttributeSet?,
    defStyleRes: Int
) : View(context, attrs, defStyleRes) {

    constructor(context: Context, attributesSet: AttributeSet?) :
            this(context, attributesSet, 0)

    constructor(context: Context) : this(context, null)

    companion object {

        const val VIEW_HEIGHT = 300
        const val BAR_COLOR = -16744448
        const val AXIS_COLOR = -16761088
        const val GROUND_COLOR = -4522056
        const val SEG_H = 5f      // Высота Сегмента
        const val SEG_SPACE = 2f  // Пробел между сегментами
        const val COLUMNS = 40    // Число колонок
        const val MAX_BAR = 124
    }

    private val mPaintSegm = Paint()
    private val mPaintBar = Paint()
    private val mPaintWave = Paint()
    private val mPaintAxis = Paint()
    private val mPaintGround = Paint()

    private var mData: ByteArray // Массив столбцов громкости
    private var mWBlock = 0f
    private var mHBlock = 0f // Высота блока
    private var mWSpace = 0f
    private var mVolume = 0f // 0.0 - 1.0
    private lateinit var mAudioManager: AudioManager
    private var mMaxVolume = 0
    private var mCurrVolume = 0
    private var mX0Bar = 0f
    private var mX0Axis = 0f
    private var mX0Ground = 0f
    private var mY0Bar = 0f
    private var mY0Seg = 0f
    private var mY0Wave = 0f
    private var mScaleBar = 0f // Масштаб сплошной
    private var mScaleWave = 0f // Масштаб волны
    private var mScaleSeg = 0f // Масштаб сегментов
    private lateinit var shader: LinearGradient
    private var mWSeg = 0f // Ширина сегменты

    // Атрибуты
    private var mAxisColor = 0
    private var mBarColor = 0
    private var mColumns = 0 // Число колонок
    private var mGroundColor = 0
    private var mHSeg = 0f // Высота сегмента
    private var mHSpace = 0f
    private var mStyle = 0
    private var mTopColor = 0


    init {
        context.withStyledAttributes(attrs, R.styleable.VisualizerView) {

            mAxisColor = getColor(R.styleable.VisualizerView_vs_axisColor, AXIS_COLOR)
            mColumns = getInt(R.styleable.VisualizerView_vs_columnsNum, COLUMNS)
            mBarColor = getColor(R.styleable.VisualizerView_vs_barColor, BAR_COLOR)
            mGroundColor = getColor(R.styleable.VisualizerView_vs_groundColor, GROUND_COLOR)
            mHSeg = getDimension(R.styleable.VisualizerView_vs_segmentHeight, dpToPx(SEG_H))
            mHSpace = getDimension(R.styleable.VisualizerView_vs_segmentSpace, dpToPx(SEG_SPACE))
            mStyle = getInt(R.styleable.VisualizerView_vs_styleColumn, 0)
            mTopColor = getColor(R.styleable.VisualizerView_vs_topColor, 0)

        }

        mData = ByteArray(mColumns)
        initPaints()
        initVolume()
        if (isInEditMode) { // Режим отладки
            mData = DataEditMode.data
        }
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
    }

    private fun initPaints() {
        mPaintSegm.color = mBarColor
        mPaintSegm.strokeWidth = 2f
        mPaintSegm.style = Paint.Style.STROKE

        mPaintBar.color = mBarColor
        mPaintBar.strokeWidth = 40f
        mPaintBar.style = Paint.Style.STROKE

        mPaintWave.color = mBarColor
        mPaintWave.style = Paint.Style.STROKE
        mPaintWave.strokeCap = Paint.Cap.ROUND

        mPaintAxis.color = mAxisColor
        mPaintAxis.style = Paint.Style.STROKE
        mPaintAxis.strokeWidth = 5f

        mPaintGround.color = mGroundColor
        mPaintGround.style = Paint.Style.STROKE
    }

    private fun initVolume() {
        mAudioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
        mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        mCurrVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        mVolume = mCurrVolume.toFloat() / mMaxVolume
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (layoutParams.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), VIEW_HEIGHT)
        }
    }

    // Изменеие размеров
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setParams()
    }

    private fun setParams() {
        // Ширина
        if (mStyle == 0) { // Wave
            mWBlock = width.toFloat() / mColumns
            mWSeg = mWBlock * 0.6f
            mWSpace = mWBlock - mWSeg
            mPaintWave.strokeWidth = mWSeg // Ширина столба
        } else {
            mWSpace = 5f
            mWBlock = width.toFloat() / mColumns - mWSpace / mColumns
            mWSeg = mWBlock - mWSpace
            mPaintBar.strokeWidth = mWSeg
        }

        // Высота
        val k0 = 130
        mScaleBar = (height - 15f) / k0     // Масштаб сплошной
        mHBlock = mHSeg + mHSpace

        val k2 = 280
        mScaleWave = (height - 15f) / k2 // Масштаб волны

        val k1 = 2250
        mScaleSeg = (height - 15f) / k1
        mPaintSegm.strokeWidth = mHSeg    // Ширина столба

        // Координаты
        mX0Bar = mWSeg / 2 + mWSpace
        mX0Axis = mWSpace
        mY0Bar = height - 15f
        mY0Seg = mY0Bar - mHSeg / 2
        mX0Ground = width / 2f
        mY0Wave = height / 2f

        mPaintGround.strokeWidth = width.toFloat()
        shader = LinearGradient(
            0f, height.toFloat(), 0f, 0f,
            mGroundColor, mTopColor, Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mTopColor != 0)
            mPaintGround.shader = shader
        canvas.drawLine(mX0Ground, height.toFloat(), mX0Ground, 0f, mPaintGround)

        when (mStyle) {
            0 -> drawWare(canvas)
            1 -> drawBar(canvas)
            2 -> drawSegments(canvas)
        }

    }

    private fun drawWare(canvas: Canvas) {
        for (i in 0 until mColumns) {
            val x = mX0Bar + mWBlock * i
            val d = mData[i] * mScaleWave
            val y1 = mY0Wave - d
            val y2 = mY0Wave + d
            if (mData[i].toInt() != 0)
                canvas.drawLine(x, y1, x, y2, mPaintWave)
        }
    }

    private fun drawBar(canvas: Canvas) {
        for (i in 0 until mColumns) {
            val x = mX0Bar + mWBlock * i
            var d = mData[i]
            d = minOf(d, MAX_BAR.toByte())
            val y2 = mY0Bar - d * mScaleBar
            val xA1 = mX0Axis + mWBlock * i
            val xA2 = xA1 + mWBlock - mWSpace

            canvas.drawLine(x, mY0Bar, x, y2, mPaintBar)
            // Ось
            canvas.drawLine(xA1, mY0Bar, xA2, mY0Bar, mPaintAxis)
        }
    }

    // Сегменты
    private fun drawSegments(canvas: Canvas) {
        for (i in 0 until mColumns) {
            val x = mWSpace + mWBlock * i
            var data = mData[i]
            data = minOf(data, MAX_BAR.toByte())
            drawColumns(canvas, x, data)
        }
    }

    // Рисовать столбцы
    private fun drawColumns(canvas: Canvas, x: Float, fValue: Byte) {
        val value = (fValue * mScaleSeg).toInt()
        val x2 = x + mWSeg

        for (i in 0 until value) {
            val y = mY0Seg - mHBlock * i
            canvas.drawLine(x, y, x2, y, mPaintSegm)
        }
        // Ось
        canvas.drawLine(x, mY0Bar, x2, mY0Bar, mPaintAxis)
    }


    fun setFft(fft: ByteArray?) {
        if (fft!!.isEmpty()) return
        val size = mColumns
        for (i in 0..<size) {
            val real = fft[i]
            val imag = fft[i + 1]
            val d = sqrt((real * real + imag * imag).toFloat())
            mData[i] = (d * 0.7f * mVolume).toInt().toByte()
        }
        invalidate()
    }

    var volume: Float = 1f
        set(value) {
            mVolume = value / mMaxVolume
        }

    var style: Int = 0
        set(value) {
            mStyle = value
            if (value > 2) mStyle = 2
            if (value < 0) mStyle = 0
            setParams()
            invalidate()

        }

    var barColor: Int = 0
        set(value) {
            mBarColor = value
            mPaintSegm.color = mBarColor
            mPaintBar.color = mBarColor
            mPaintWave.color = mBarColor
            invalidate()
        }

    var axisColor: Int = 0
        set(value) {
            mAxisColor = value
            mPaintAxis.color = mAxisColor
            invalidate()
        }

    var groundColor: Int = 0
        set(value) {
            mGroundColor = value
            mPaintGround.color = mGroundColor
            invalidate()
        }

    var topColor: Int = 0
        set(value) {
            mTopColor = value
            setParams()
            invalidate()
        }

    var columnsNum: Int = 0
        set(value) {
            mColumns = value
            if (value < 10) mColumns = 10
            if (value > 50) mColumns = 50
            mData = ByteArray(mColumns)
            setParams()
            invalidate()
        }

    var segmentHeight: Float = mHSeg
        set(value) {
            mHSeg = value
            setParams()
            invalidate()
        }

}