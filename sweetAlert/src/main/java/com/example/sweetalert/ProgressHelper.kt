package com.example.sweetalert

import android.content.Context

class ProgressHelper(ctx: Context) {
    private var mProgressWheel: ProgressWheel? = null
    private var isSpinning: Boolean = true
    private var mSpinSpeed = 0.75f
    private var mBarWidth: Int = ctx.resources.getDimensionPixelSize(R.dimen.common_circle_width) + 1
    private var mBarColor: Int = ctx.resources.getColor(R.color.success_stroke_color,ctx.theme)
    private var mRimWidth = 0
    private var mRimColor = 0x00000000
    private var mIsInstantProgress = false
    private var mProgressVal: Float = -1f
    private var mCircleRadius: Int = ctx.resources.getDimensionPixelOffset(R.dimen.progress_circle_radius)

    /*    var progressWheel: ProgressWheel?
            get() = mProgressWheel
            set(value) {
                mProgressWheel = value
                updatePropsIfNeed()
            }*/


    fun setProgressWheel(progressWheel: ProgressWheel) {
        mProgressWheel = progressWheel
        updatePropsIfNeed()
    }

/*
    fun getBarColor(): Int {
        return mBarColor
    }
*/

    fun setBarColor(barColor: Int) {
        mBarColor = barColor
        updatePropsIfNeed()
    }
    private fun updatePropsIfNeed() {
        if (mProgressWheel != null) {
            if (!isSpinning && mProgressWheel!!.isSpinning) {
                mProgressWheel!!.stopSpinning()
            } else if (isSpinning && !mProgressWheel!!.isSpinning) {
                mProgressWheel!!.spin()
            }
            if (mSpinSpeed != mProgressWheel!!.getSpinSpeed()) {
                mProgressWheel!!.setSpinSpeed(mSpinSpeed)
            }
            if (mBarWidth != mProgressWheel!!.getBarWidth()) {
                mProgressWheel!!.setBarWidth(mBarWidth)
            }
            if (mBarColor != mProgressWheel!!.getBarColor()) {
                mProgressWheel!!.setBarColor(mBarColor)
            }
            if (mRimWidth != mProgressWheel!!.getRimWidth()) {
                mProgressWheel!!.setRimWidth(mRimWidth)
            }
            if (mRimColor != mProgressWheel!!.getRimColor()) {
                mProgressWheel!!.setRimColor(mRimColor)
            }
            if (mProgressVal != mProgressWheel!!.getProgressBar()) {
                if (mIsInstantProgress) {
                    mProgressWheel!!.setInstantProgress(mProgressVal)
                } else {
                    mProgressWheel!!.setProgressBar(mProgressVal)
                }
            }
            if (mCircleRadius != mProgressWheel!!.getCircleRadius()) {
                mProgressWheel!!.setCircleRadius(mCircleRadius)
            }
        }
    }

/*    fun resetCount() {
        if (mProgressWheel != null) {
            mProgressWheel!!.resetCount()
        }
    }

    fun spin() {
        isSpinning = true
        updatePropsIfNeed()
    }

    fun stopSpinning() {
        isSpinning = false
        updatePropsIfNeed()
    }

    var progress: Float
        get() = mProgressVal
        set(progress) {
            mIsInstantProgress = false
            mProgressVal = progress
            updatePropsIfNeed()
        }

    fun setInstantProgress(progress: Float) {
        mProgressVal = progress
        mIsInstantProgress = true
        updatePropsIfNeed()
    }

    var circleRadius: Int
        get() = mCircleRadius
        set(circleRadius) {
            mCircleRadius = circleRadius
            updatePropsIfNeed()
        }

    var barWidth: Int
        get() = mBarWidth
        set(barWidth) {
            mBarWidth = barWidth
            updatePropsIfNeed()
        }

    var barColor: Int
        get() = mBarColor
        set(barColor) {
            mBarColor = barColor
            updatePropsIfNeed()
        }

    var rimWidth: Int
        get() = mRimWidth
        set(rimWidth) {
            mRimWidth = rimWidth
            updatePropsIfNeed()
        }

    var rimColor: Int
        get() = mRimColor
        set(rimColor) {
            mRimColor = rimColor
            updatePropsIfNeed()
        }

    var spinSpeed: Float
        get() = mSpinSpeed
        set(spinSpeed) {
            mSpinSpeed = spinSpeed
            updatePropsIfNeed()
        }*/
}
