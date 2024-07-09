package com.example.sweetalert

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Parcel
import android.os.Parcelable
import android.os.SystemClock
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import kotlin.math.cos
import kotlin.math.min


/**
 * A Material style progress wheel, compatible up to 2.2.
 * Todd Davies' Progress Wheel https://github.com/Todd-Davies/ProgressWheel
 *
 * @author Nico Hormazábal
 *
 *
 * Licensed under the Apache License 2.0 license see:
 * http://www.apache.org/licenses/LICENSE-2.0
 */
class ProgressWheel : View {
    //Sizes (with defaults)
    private var circleRadius = 80
    private var fillRadius = false

    private val barLength = 40
    private val barMaxLength = 270
    private var timeStartGrowing = 0.0
    private var barSpinCycleTime = 1000.0
    private var barExtraLength = 0f
    private var barGrowingFromFront = true

    private var pausedTimeWithoutGrowing: Long = 0
    private val pauseGrowingTime: Long = 300
    private var barWidth = 5
    private var rimWidth = 5

    //Colors (with defaults)
    private var barColor = -0x56000000
    private var rimColor = 0x00FFFFFF

    //Paints
    private val barPaint = Paint()
    private val rimPaint = Paint()

    //Rectangles
    private var circleBounds = RectF()

    //Animation
    //The amount of degrees per second
    private var spinSpeed = 270.0f

    // The last time the spinner was animated
    private var lastTimeAnimated: Long = 0

    private var mProgress = 0.0f
    private var mTargetProgress = 0.0f

    /**
     * Check if the wheel is currently spinning
     */
    var isSpinning: Boolean = false
        private set

    /**
     * The constructor for the ProgressWheel
     *
     * @param context
     * @param attrs
     */
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        parseAttributes(
            context.obtainStyledAttributes(
                attrs,
                R.styleable.ProgressWheel
            )
        )
    }

    /**
     * The constructor for the ProgressWheel
     *
     * @param context
     */
    constructor(context: Context?) : super(context)

    //----------------------------------
    //Setting up stuff
    //----------------------------------
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val viewWidth = circleRadius + this.paddingLeft + this.paddingRight
        val viewHeight = circleRadius + this.paddingTop + this.paddingBottom

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        //Measure Width
        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> {
                //Must be this size
                widthSize
            }
            MeasureSpec.AT_MOST -> {
                //Can't be bigger than...
                min(viewWidth.toDouble(), widthSize.toDouble()).toInt()
            }
            else -> {
                //Be whatever you want
                viewWidth
            }
        }

        //Measure Height
        val height = if (heightMode == MeasureSpec.EXACTLY || widthMode == MeasureSpec.EXACTLY) {
            //Must be this size
            heightSize
        } else if (heightMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            min(viewHeight.toDouble(), heightSize.toDouble()).toInt()
        } else {
            //Be whatever you want
            viewHeight
        }

        setMeasuredDimension(width, height)
    }

    /**
     * Use onSizeChanged instead of onAttachedToWindow to get the dimensions of the view,
     * because this method is called after measuring the dimensions of MATCH_PARENT & WRAP_CONTENT.
     * Use this dimensions to setup the bounds and paints.
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        setupBounds(w, h)
        setupPaints()
        invalidate()
    }

    /**
     * Set the properties of the paints we're using to
     * draw the progress wheel
     */
    private fun setupPaints() {
        barPaint.color = barColor
        barPaint.isAntiAlias = true
        barPaint.style = Paint.Style.STROKE
        barPaint.strokeWidth = barWidth.toFloat()

        rimPaint.color = rimColor
        rimPaint.isAntiAlias = true
        rimPaint.style = Paint.Style.STROKE
        rimPaint.strokeWidth = rimWidth.toFloat()
    }

    /**
     * Set the bounds of the component
     */
    private fun setupBounds(layoutWidth: Int, layoutHeight: Int) {
        val paddingTop = paddingTop
        val paddingBottom = paddingBottom
        val paddingLeft = paddingLeft
        val paddingRight = paddingRight

        if (!fillRadius) {
            // Width should equal to Height, find the min value to setup the circle
            val minValue = min(
                (layoutWidth - paddingLeft - paddingRight).toDouble(),
                (layoutHeight - paddingBottom - paddingTop).toDouble()
            ).toInt()

            val circleDiameter =
                min(minValue.toDouble(), (circleRadius * 2 - barWidth * 2).toDouble()).toInt()

            // Calc the Offset if needed for centering the wheel in the available space
            val xOffset =
                (layoutWidth - paddingLeft - paddingRight - circleDiameter) / 2 + paddingLeft
            val yOffset =
                (layoutHeight - paddingTop - paddingBottom - circleDiameter) / 2 + paddingTop

            circleBounds = RectF(
                (xOffset + barWidth).toFloat(),
                (yOffset + barWidth).toFloat(),
                (xOffset + circleDiameter - barWidth).toFloat(),
                (yOffset + circleDiameter - barWidth).toFloat()
            )
        } else {
            circleBounds = RectF(
                (paddingLeft + barWidth).toFloat(),
                (paddingTop + barWidth).toFloat(),
                (layoutWidth - paddingRight - barWidth).toFloat(),
                (layoutHeight - paddingBottom - barWidth).toFloat()
            )
        }
    }

    /**
     * Parse the attributes passed to the view from the XML
     *
     * @param a the attributes to parse
     */
    private fun parseAttributes(a: TypedArray) {
        val metrics = context.resources.displayMetrics
        barWidth =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, barWidth.toFloat(), metrics)
                .toInt()
        rimWidth =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, rimWidth.toFloat(), metrics)
                .toInt()

        circleRadius =
            a.getDimension(R.styleable.ProgressWheel_matProg_circleRadius, circleRadius.toFloat()).toInt()

        fillRadius = a.getBoolean(R.styleable.ProgressWheel_matProg_fillRadius, false)

        barWidth = a.getDimension(R.styleable.ProgressWheel_matProg_barWidth, barWidth.toFloat()).toInt()

        rimWidth = a.getDimension(R.styleable.ProgressWheel_matProg_rimWidth, rimWidth.toFloat()).toInt()

        val baseSpinSpeed = a.getFloat(R.styleable.ProgressWheel_matProg_spinSpeed, spinSpeed / 360.0f)
        spinSpeed = baseSpinSpeed * 360

        barSpinCycleTime =
            a.getInt(R.styleable.ProgressWheel_matProg_barSpinCycleTime, barSpinCycleTime.toInt())
                .toDouble()

        barColor = a.getColor(R.styleable.ProgressWheel_matProg_barColor, barColor)

        rimColor = a.getColor(R.styleable.ProgressWheel_matProg_rimColor, rimColor)

        if (a.getBoolean(R.styleable.ProgressWheel_matProg_progressIndeterminate, false)) {
            spin()
        }

        // Recycle
        a.recycle()
    }

    //----------------------------------
    //Animation stuff
    //----------------------------------
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawArc(circleBounds, 360f, 360f, false, rimPaint)

        var mustInvalidate = false

        if (isSpinning) {
            //Draw the spinning bar
            mustInvalidate = true

            val deltaTime = (SystemClock.uptimeMillis() - lastTimeAnimated)
            val deltaNormalized = deltaTime * spinSpeed / 1000.0f

            updateBarLength(deltaTime)

            mProgress += deltaNormalized
            if (mProgress > 360) {
                mProgress -= 360f
            }
            lastTimeAnimated = SystemClock.uptimeMillis()

            val from = mProgress - 90
            val length = barLength + barExtraLength

            canvas.drawArc(
                circleBounds, from, length, false,
                barPaint
            )
        } else {
            if (mProgress != mTargetProgress) {
                //We smoothly increase the progress bar
                mustInvalidate = true

                val deltaTime = (SystemClock.uptimeMillis() - lastTimeAnimated).toFloat() / 1000
                val deltaNormalized = deltaTime * spinSpeed

                mProgress =
                    min((mProgress + deltaNormalized).toDouble(), mTargetProgress.toDouble())
                        .toFloat()
                lastTimeAnimated = SystemClock.uptimeMillis()
            }

            canvas.drawArc(circleBounds, -90f, mProgress, false, barPaint)
        }

        if (mustInvalidate) {
            invalidate()
        }
    }

    private fun updateBarLength(deltaTimeInMilliSeconds: Long) {
        if (pausedTimeWithoutGrowing >= pauseGrowingTime) {
            timeStartGrowing += deltaTimeInMilliSeconds.toDouble()

            if (timeStartGrowing > barSpinCycleTime) {
                // We completed a size change cycle
                // (growing or shrinking)
                timeStartGrowing -= barSpinCycleTime
                timeStartGrowing = 0.0
                if (!barGrowingFromFront) {
                    pausedTimeWithoutGrowing = 0
                }
                barGrowingFromFront = !barGrowingFromFront
            }

            val distance = cos((timeStartGrowing / barSpinCycleTime + 1) * Math.PI)
                .toFloat() / 2 + 0.5f
            val destLength = (barMaxLength - barLength).toFloat()

            if (barGrowingFromFront) {
                barExtraLength = distance * destLength
            } else {
                val newLength = destLength * (1 - distance)
                mProgress += (barExtraLength - newLength)
                barExtraLength = newLength
            }
        } else {
            pausedTimeWithoutGrowing += deltaTimeInMilliSeconds
        }
    }

    //----------------------------------
    //Getters + setters
    //----------------------------------
    fun getProgressBar(): Float {
        return if (isSpinning) -1f else mProgress / 360.0f
    }

    fun setProgressBar(progress: Float) {
        var progressBar = progress
        if (isSpinning) {
            mProgress = 0.0f
            isSpinning = false
        }

        if (progressBar > 1.0f) {
            progressBar -= 1.0f
        } else if (progressBar < 0) {
            progressBar = 0f
        }

        if (progressBar == mTargetProgress) {
            return
        }

        // If we are currently in the right position
        // we set again the last time animated so the
        // animation starts smooth from here
        if (mProgress == mTargetProgress) {
            lastTimeAnimated = SystemClock.uptimeMillis()
        }

        mTargetProgress = min((progressBar * 360.0f).toDouble(), 360.0).toFloat()

        invalidate()
    }
    /**
     * Reset the count (in increment mode)
     */
/*    fun resetCount() {
        mProgress = 0.0f
        mTargetProgress = 0.0f
        invalidate()
    }*/

    /**
     * Turn off spin mode
     */
    fun stopSpinning() {
        isSpinning = false
        mProgress = 0.0f
        mTargetProgress = 0.0f
        invalidate()
    }

    /**
     * Puts the view on spin mode
     */
    fun spin() {
        lastTimeAnimated = SystemClock.uptimeMillis()
        isSpinning = true
        invalidate()
    }

    /**
     * Set the progress to a specific value,
     * the bar will be set instantly to that value
     * @param progress the progress between 0 and 1
     */
    fun setInstantProgress(progress: Float) {
        var instantProgress = progress
        if (isSpinning) {
            mProgress = 0.0f
            isSpinning = false
        }

        if (instantProgress > 1.0f) {
            instantProgress -= 1.0f
        } else if (instantProgress < 0) {
            instantProgress = 0f
        }

        if (instantProgress == mTargetProgress) {
            return
        }

        mTargetProgress = min((instantProgress * 360.0f).toDouble(), 360.0).toFloat()
        mProgress = mTargetProgress
        lastTimeAnimated = SystemClock.uptimeMillis()
        invalidate()
    }

    // Great way to save a view's state http://stackoverflow.com/a/7089687/1991053
    public override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()

        val ss = WheelSavedState(superState)

        // We save everything that can be changed at runtime
        ss.mProgress = this.mProgress
        ss.mTargetProgress = this.mTargetProgress
        ss.isSpinning = this.isSpinning
        ss.spinSpeed = this.spinSpeed
        ss.barWidth = this.barWidth
        ss.barColor = this.barColor
        ss.rimWidth = this.rimWidth
        ss.rimColor = this.rimColor
        ss.circleRadius = this.circleRadius

        return ss
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is WheelSavedState) {
            super.onRestoreInstanceState(state)
            return
        }

        super.onRestoreInstanceState(state.superState)

        this.mProgress = state.mProgress
        this.mTargetProgress = state.mTargetProgress
        this.isSpinning = state.isSpinning
        this.spinSpeed = state.spinSpeed
        this.barWidth = state.barWidth
        this.barColor = state.barColor
        this.rimWidth = state.rimWidth
        this.rimColor = state.rimColor
        this.circleRadius = state.circleRadius
    }

    //----------------------------------
    //Getters + setters
    //----------------------------------
    var progress: Float
    /**
     * @return the current progress between 0.0 and 1.0,
     * if the wheel is indeterminate, then the result is -1
     */
    get() = if (isSpinning) -1f else mProgress / 360.0f
    /**
     * Set the progress to a specific value,
     * the bar will smoothly animate until that value
     * @param progress the progress between 0 and 1
     */
    set(progress) {
        var progress = progress
        if (isSpinning) {
            mProgress = 0.0f
            isSpinning = false
        }

        if (progress > 1.0f) {
            progress -= 1.0f
        } else if (progress < 0) {
            progress = 0f
        }

        if (progress == mTargetProgress) {
            return
        }

        // If we are currently in the right position
        // we set again the last time animated so the
        // animation starts smooth from here
        if (mProgress == mTargetProgress) {
            lastTimeAnimated = SystemClock.uptimeMillis()
        }

        mTargetProgress = min((progress * 360.0f).toDouble(), 360.0).toFloat()

        invalidate()
    }

    /**
     * @return the radius of the wheel in pixels
     */
    fun getCircleRadius(): Int {
        return circleRadius
    }

    /**
     * Sets the radius of the wheel
     * @param circleRadius the expected radius, in pixels
     */
    fun setCircleRadius(circleRadius: Int) {
        this.circleRadius = circleRadius
        if (!isSpinning) {
            invalidate()
        }
    }

    /**
     * @return the width of the spinning bar
     */
    fun getBarWidth(): Int {
        return barWidth
    }

    /**
     * Sets the width of the spinning bar
     * @param barWidth the spinning bar width in pixels
     */
    fun setBarWidth(barWidth: Int) {
        this.barWidth = barWidth
        if (!isSpinning) {
            invalidate()
        }
    }

    /**
     * @return the color of the spinning bar
     */
    fun getBarColor(): Int {
        return barColor
    }

    /**
     * Sets the color of the spinning bar
     * @param barColor The spinning bar color
     */
    fun setBarColor(barColor: Int) {
        this.barColor = barColor
        setupPaints()
        if (!isSpinning) {
            invalidate()
        }
    }

    /**
     * @return the color of the wheel's contour
     */
    fun getRimColor(): Int {
        return rimColor
    }

    /**
     * Sets the color of the wheel's contour
     * @param rimColor the color for the wheel
     */
    fun setRimColor(rimColor: Int) {
        this.rimColor = rimColor
        setupPaints()
        if (!isSpinning) {
            invalidate()
        }
    }

    /**
     * @return the base spinning speed, in full circle turns per second
     * (1.0 equals on full turn in one second), this value also is applied for
     * the smoothness when setting a progress
     */
    fun getSpinSpeed(): Float {
        return spinSpeed / 360.0f
    }

    /**
     * Sets the base spinning speed, in full circle turns per second
     * (1.0 equals on full turn in one second), this value also is applied for
     * the smoothness when setting a progress
     *
     * @param spinSpeed the desired base speed in full turns per second
     */
    fun setSpinSpeed(spinSpeed: Float) {
        this.spinSpeed = spinSpeed * 360.0f
    }

    /**
     * @return the width of the wheel's contour in pixels
     */
    fun getRimWidth(): Int {
        return rimWidth
    }

    /**
     * Sets the width of the wheel's contour
     * @param rimWidth the width in pixels
     */
    fun setRimWidth(rimWidth: Int) {
        this.rimWidth = rimWidth
        if (!isSpinning) {
            invalidate()
        }
    }

    internal class WheelSavedState : BaseSavedState {
        var mProgress: Float = 0f
        var mTargetProgress: Float = 0f
        var isSpinning: Boolean = false
        var spinSpeed: Float = 0f
        var barWidth: Int = 0
        var barColor: Int = 0
        var rimWidth: Int = 0
        var rimColor: Int = 0
        var circleRadius: Int = 0

        constructor(superState: Parcelable?) : super(superState)

        private constructor(`in`: Parcel) : super(`in`) {
            this.mProgress = `in`.readFloat()
            this.mTargetProgress = `in`.readFloat()
            this.isSpinning = `in`.readByte().toInt() != 0
            this.spinSpeed = `in`.readFloat()
            this.barWidth = `in`.readInt()
            this.barColor = `in`.readInt()
            this.rimWidth = `in`.readInt()
            this.rimColor = `in`.readInt()
            this.circleRadius = `in`.readInt()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeFloat(this.mProgress)
            out.writeFloat(this.mTargetProgress)
            out.writeByte((if (isSpinning) 1 else 0).toByte())
            out.writeFloat(this.spinSpeed)
            out.writeInt(this.barWidth)
            out.writeInt(this.barColor)
            out.writeInt(this.rimWidth)
            out.writeInt(this.rimColor)
            out.writeInt(this.circleRadius)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<WheelSavedState> {
            override fun createFromParcel(parcel: Parcel): WheelSavedState {
                return WheelSavedState(parcel)
            }

            override fun newArray(size: Int): Array<WheelSavedState?> {
                return arrayOfNulls(size)
            }
        }
    }

}
