package com.example.mamoontaskapp.adapters

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

class ExpandCollapseAnimator(private val view: View) {
    var isExpanded: Boolean = false

    // Lazy initialization of the expandAnimator using the by lazy delegate
    private val expandAnimator: Animator by lazy {
        ObjectAnimator.ofFloat(view, View.SCALE_Y, 0f, 1f).apply {
            duration = ANIMATION_DURATION
            interpolator = ACCELERATE_DECELERATE_INTERPOLATOR
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.scaleY = 1f
                }
            })
        }
    }

    // Lazy initialization of the collapseAnimator using the by lazy delegate
    private val collapseAnimator: Animator by lazy {
        ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 0f).apply {
            duration = ANIMATION_DURATION
            interpolator = ACCELERATE_DECELERATE_INTERPOLATOR
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.scaleY = 0f
                    view.visibility = View.GONE
                }
            })
        }
    }

    /**
     * Expands the view with animation.
     */
    fun expand() {
        cancelAnimations()
        view.visibility = View.VISIBLE
        view.pivotY = 0f

        expandAnimator.start()
    }

    /**
     * Collapses the view with animation.
     */
    fun collapse() {
        cancelAnimations()
        collapseAnimator.start()
    }

    /**
     * Cancels any ongoing animations.
     */
    private fun cancelAnimations() {
        expandAnimator.cancel()
        collapseAnimator.cancel()
    }

    companion object {
        private const val ANIMATION_DURATION = 300L
        private val ACCELERATE_DECELERATE_INTERPOLATOR = AccelerateDecelerateInterpolator()
    }
}