package com.app.str.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import android.widget.TextView
import com.app.str.R
import com.airbnb.lottie.LottieAnimationView

class LoadingDialog(private val context: Context) {
    
    private var dialog: Dialog? = null
    private var loadingLottie: LottieAnimationView? = null
    private var tvLoadingMessage: TextView? = null
    
    init {
        setupDialog()
    }
    
    private fun setupDialog() {
        dialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            val view = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null)
            setContentView(view)
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            
            window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setDimAmount(0.5f)
            }
            
            loadingLottie = view.findViewById(R.id.loadingLottie)
            tvLoadingMessage = view.findViewById(R.id.tvLoadingMessage)
        }
    }
    
    /**
     * Show loading dialog with default message
     */
    fun show() {
        show("Please wait...")
    }
    
    /**
     * Show loading dialog with custom message
     * @param message Custom message to display
     */
    fun show(message: String) {
        try {
            if (dialog?.isShowing == false) {
                tvLoadingMessage?.text = message
                dialog?.show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Update the loading message while dialog is showing
     * @param message New message to display
     */
    fun updateMessage(message: String) {
        tvLoadingMessage?.text = message
    }
    
    /**
     * Dismiss the loading dialog
     */
    fun dismiss() {
        try {
            if (dialog?.isShowing == true) {
                dialog?.dismiss()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Check if dialog is currently showing
     */
    fun isShowing(): Boolean {
        return dialog?.isShowing == true
    }
    
    /**
     * Release resources
     */
    fun destroy() {
        dismiss()
        dialog = null
        loadingLottie = null
        tvLoadingMessage = null
    }
}
