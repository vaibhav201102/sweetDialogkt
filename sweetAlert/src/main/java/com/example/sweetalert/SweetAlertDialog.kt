package com.example.sweetalert

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.Transformation
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView

class SweetAlertDialog @JvmOverloads constructor(context: Context?, alertType: Int = NORMAL_TYPE) : Dialog(context!!, R.style.alert_dialog), View.OnClickListener {

    private var mDialogView: View? = null
    private val mModalInAnim: AnimationSet?
    private val mModalOutAnim: AnimationSet?
    private val mOverlayOutAnim: Animation
    private val mErrorInAnim: Animation?
    private val mErrorXInAnim: AnimationSet?
    private val mSuccessLayoutAnimSet: AnimationSet?
    private val mSuccessBowAnim: Animation?
    private var mTitleTextView: TextView? = null
    private var mContentTextView: TextView? = null
    private var titleText: String? = null
    private var contentText: String? = null
    private var isShowCancelButton: Boolean = false
    private var isShowContentText: Boolean = false
    private var cancelText: String? = null
    private var confirmText: String? = null
    private var alertType: Int
    private var mErrorFrame: FrameLayout? = null
    private var mSuccessFrame: FrameLayout? = null
    private var mProgressFrame: FrameLayout? = null
    private var mSuccessTick: SuccessTickView? = null
    private var mErrorX: ImageView? = null
    private var mSuccessLeftMask: View? = null
    private var mSuccessRightMask: View? = null
    private var mCustomImgDrawable: Drawable? = null
    private var mCustomImage: ImageView? = null
    private var mConfirmButton: Button? = null
    private var mCancelButton: Button? = null
    private val progressHelper: ProgressHelper
    private var mWarningFrame: FrameLayout? = null
    private var mCancelClickListener: OnSweetClickListener? = null
    private var mConfirmClickListener: OnSweetClickListener? = null
    private var mCloseFromCancel = false


    init {
        setCancelable(true)
        setCanceledOnTouchOutside(false)
        progressHelper = ProgressHelper(context!!)
        this.alertType = alertType
        mErrorInAnim = OptAnimationLoader.loadAnimation(getContext(), R.anim.error_frame_in)
        mErrorXInAnim = OptAnimationLoader.loadAnimation(getContext(), R.anim.error_x_in) as AnimationSet?
        // 2.3.x system don't support alpha-animation on layer-list drawable
        // remove it from animation set
        mSuccessBowAnim = OptAnimationLoader.loadAnimation(getContext(), R.anim.success_bow_roate)
        mSuccessLayoutAnimSet = OptAnimationLoader.loadAnimation(getContext(),R.anim.success_mask_layout) as AnimationSet?
        mModalInAnim = OptAnimationLoader.loadAnimation(getContext(), R.anim.modal_in) as AnimationSet?
        mModalOutAnim = OptAnimationLoader.loadAnimation(getContext(), R.anim.modal_out) as AnimationSet?
        mModalOutAnim!!.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
            }

            override fun onAnimationEnd(animation: Animation) {
                mDialogView!!.visibility = View.GONE
                mDialogView!!.post {
                    if (mCloseFromCancel) {
                        super@SweetAlertDialog.cancel()
                    } else {
                        super@SweetAlertDialog.dismiss()
                    }
                }
            }

            override fun onAnimationRepeat(animation: Animation) {
            }
        })

        // dialog overlay fade out
        mOverlayOutAnim = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                val wlp = window!!.attributes
                wlp.alpha = 1 - interpolatedTime
                window!!.attributes = wlp
            }
        }
        mOverlayOutAnim.setDuration(120)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.alert_dialog)

        mDialogView = window!!.decorView.findViewById(android.R.id.content)
        mTitleTextView = findViewById<View>(R.id.title_text) as TextView
        mContentTextView = findViewById<View>(R.id.content_text) as TextView
        mErrorFrame = findViewById<View>(R.id.error_frame) as FrameLayout
        mErrorX = mErrorFrame!!.findViewById<View>(R.id.error_x) as ImageView
        mSuccessFrame = findViewById<View>(R.id.success_frame) as FrameLayout
        mProgressFrame = findViewById<View>(R.id.progress_dialog) as FrameLayout
        mSuccessTick = mSuccessFrame!!.findViewById<View>(R.id.success_tick) as SuccessTickView
        mSuccessLeftMask = mSuccessFrame!!.findViewById(R.id.mask_left)
        mSuccessRightMask = mSuccessFrame!!.findViewById(R.id.mask_right)
        mCustomImage = findViewById<View>(R.id.custom_image) as ImageView
        mWarningFrame = findViewById<View>(R.id.warning_frame) as FrameLayout
        mConfirmButton = findViewById<View>(R.id.confirm_button) as Button
        mCancelButton = findViewById<View>(R.id.cancel_button) as Button
        progressHelper.setProgressWheel(findViewById<View>(R.id.progressWheel) as ProgressWheel)
        mConfirmButton!!.setOnClickListener(this)
        mCancelButton!!.setOnClickListener(this)

        setTitleText(titleText)
        setContentText(contentText)
        setCancelText(cancelText)
        setConfirmText(confirmText)
        changeAlertType(alertType, true)
    }

    private fun restore() {
        mCustomImage!!.visibility = View.GONE
        mErrorFrame!!.visibility = View.GONE
        mSuccessFrame!!.visibility = View.GONE
        mWarningFrame!!.visibility = View.GONE
        mProgressFrame!!.visibility = View.GONE
        mConfirmButton!!.visibility = View.VISIBLE

        mConfirmButton!!.setBackgroundResource(R.drawable.blue_button_background)
        mErrorFrame!!.clearAnimation()
        mErrorX!!.clearAnimation()
        mSuccessTick!!.clearAnimation()
        mSuccessLeftMask!!.clearAnimation()
        mSuccessRightMask!!.clearAnimation()
    }

    private fun playAnimation() {
        if (alertType == ERROR_TYPE) {
            mErrorFrame!!.startAnimation(mErrorInAnim)
            mErrorX!!.startAnimation(mErrorXInAnim)
        } else if (alertType == SUCCESS_TYPE) {
            mSuccessTick!!.startTickAnim()
            mSuccessRightMask!!.startAnimation(mSuccessBowAnim)
        }
    }

    private fun changeAlertType(alertType: Int, fromCreate: Boolean) {
        this.alertType = alertType
        // call after created views
        if (mDialogView != null) {
            if (!fromCreate) {
                // restore all of views state before switching alert type
                restore()
            }
            when (this.alertType) {
                ERROR_TYPE -> mErrorFrame!!.visibility =
                    View.VISIBLE

                SUCCESS_TYPE -> {
                    mSuccessFrame!!.visibility = View.VISIBLE
                    // initial rotate layout of success mask
                    mSuccessLeftMask!!.startAnimation(mSuccessLayoutAnimSet!!.animations[0])
                    mSuccessRightMask!!.startAnimation(mSuccessLayoutAnimSet.animations[1])
                }

                WARNING_TYPE -> {
                    mConfirmButton!!.setBackgroundResource(R.drawable.red_button_background)
                    mWarningFrame!!.visibility = View.VISIBLE
                }

                CUSTOM_IMAGE_TYPE -> setCustomImage(mCustomImgDrawable)
                PROGRESS_TYPE -> {
                    mProgressFrame!!.visibility = View.VISIBLE
                    mConfirmButton!!.visibility = View.GONE
                }
            }
            if (!fromCreate) {
                playAnimation()
            }
        }
    }

    fun changeAlertType(alertType: Int) {
        changeAlertType(alertType, false)
    }


    fun setTitleText(text: String?): SweetAlertDialog {
        titleText = text
        if (mTitleTextView != null && titleText != null) {
            mTitleTextView!!.text = titleText
        }
        return this
    }

    private fun setCustomImage(drawable: Drawable?): SweetAlertDialog {
        mCustomImgDrawable = drawable
        if (mCustomImage != null && mCustomImgDrawable != null) {
            mCustomImage!!.visibility = View.VISIBLE
            mCustomImage!!.setImageDrawable(mCustomImgDrawable)
        }
        return this
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun setCustomImage(resourceId: Int): SweetAlertDialog {
        return setCustomImage(context.resources.getDrawable(resourceId,context.theme))
    }

    fun setContentText(text: String?): SweetAlertDialog {
        contentText = text
        if (mContentTextView != null && contentText != null) {
            showContentText()
            mContentTextView!!.text = contentText
        }
        return this
    }

    fun showCancelButton(isShow: Boolean): SweetAlertDialog {
        isShowCancelButton = isShow
        if (mCancelButton != null) {
            mCancelButton!!.visibility = if (isShowCancelButton) View.VISIBLE else View.GONE
        }
        return this
    }

    private fun showContentText(isShow: Boolean = true): SweetAlertDialog {
        isShowContentText = isShow
        if (mContentTextView != null) {
            mContentTextView!!.visibility = if (isShowContentText) View.VISIBLE else View.GONE
        }
        return this
    }

    fun setCancelText(text: String?): SweetAlertDialog {
        cancelText = text
        if (mCancelButton != null && cancelText != null) {
            showCancelButton(true)
            mCancelButton!!.text = cancelText
        }
        return this
    }

    fun setConfirmText(text: String?): SweetAlertDialog {
        confirmText = text
        if (mConfirmButton != null && confirmText != null) {
            mConfirmButton!!.text = confirmText
        }
        return this
    }

    fun setCancelClickListener(listener: OnSweetClickListener?): SweetAlertDialog {
        mCancelClickListener = listener
        return this
    }

    fun setConfirmClickListener(listener: OnSweetClickListener?): SweetAlertDialog {
        mConfirmClickListener = listener
        return this
    }

    override fun onStart() {
        mDialogView!!.startAnimation(mModalInAnim)
        playAnimation()
    }

    /**
     * The real Dialog.cancel() will be invoked async-ly after the animation finishes.
     */
    override fun cancel() {
        dismissWithAnimation(true)
    }

    /**
     * The real Dialog.dismiss() will be invoked async-ly after the animation finishes.
     */
    private fun dismissWithAnimation() {
        dismissWithAnimation(false)
    }

    private fun dismissWithAnimation(fromCancel: Boolean) {
        mCloseFromCancel = fromCancel
        mConfirmButton!!.startAnimation(mOverlayOutAnim)
        mDialogView!!.startAnimation(mModalOutAnim)
    }

    override fun onClick(v: View) {
        if (v.id == R.id.cancel_button) {
            if (mCancelClickListener != null) {
                mCancelClickListener!!.onClick(this@SweetAlertDialog)
            } else {
                dismissWithAnimation()
            }
        } else if (v.id == R.id.confirm_button) {
            if (mConfirmClickListener != null) {
                mConfirmClickListener!!.onClick(this@SweetAlertDialog)
            } else {
                dismissWithAnimation()
            }
        }
    }

    fun getProgressHelper(): ProgressHelper {
        return progressHelper
    }

    companion object {
        const val NORMAL_TYPE: Int = 0
        const val ERROR_TYPE: Int = 1
        const val SUCCESS_TYPE: Int = 2
        const val WARNING_TYPE: Int = 3
        const val CUSTOM_IMAGE_TYPE: Int = 4
        const val PROGRESS_TYPE: Int = 5
    }
}