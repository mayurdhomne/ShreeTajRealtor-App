package com.app.str.utils

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Yeh extension function kisi bhi View par apply kar sakte ho
 * Sirf TOP padding set karega system bars ke liye
 * Baki left, right, bottom padding original rahegi
 */
fun View.applyTopPadding() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view: View, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        // Sirf TOP padding set kar rahe hain, baki purani padding use kar rahe hain
        view.setPadding(view.paddingLeft, systemBars.top, view.paddingRight, view.paddingBottom)
        insets
    }
}

/**
 * Agar tumhe saari sides par padding chahiye (left, top, right, bottom)
 */
fun View.applyFullPadding() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view: View, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        // Saari sides par system bars apply kar rahe hain
        view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
        insets
    }
}
