package com.cinetrack.ui.components.comments

import android.content.Context
import android.graphics.Outline
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.giphy.sdk.ui.views.GifView
import com.giphy.sdk.ui.views.GiphyDialogFragment

object GiphyDialogCustomizer {

    private const val GIF_CORNER_RADIUS_DP = 14f

    /**
     * Updates Giphy's static corner radius once before views are created or bound.
     */
    fun prepareStaticRadius(context: Context) {
        val density = context.resources.displayMetrics.density
        val targetRadiusPx = GIF_CORNER_RADIUS_DP * density
        try {
            val field = GifView::class.java.getDeclaredField("CORNER_RADIUS")
            field.isAccessible = true
            field.setFloat(null, targetRadiusPx)
        } catch (_: Throwable) {
            // Handled dynamically on view attach
        }
    }

    /**
     * Attaches lifecycle hooks to style the GiphyDialogFragment once its views are inflated.
     */
    fun customizeDialog(dialog: GiphyDialogFragment, fragmentManager: FragmentManager) {
        val callbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
                if (f === dialog) {
                    applyCustomizations(v)
                }
            }

            override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
                if (f === dialog) {
                    fm.unregisterFragmentLifecycleCallbacks(this)
                }
            }
        }
        fragmentManager.registerFragmentLifecycleCallbacks(callbacks, false)
    }

    private fun applyCustomizations(root: View) {
        val density = root.resources.displayMetrics.density
        val gifRadiusPx = GIF_CORNER_RADIUS_DP * density

        // 1. Style search bar as a pill using native OutlineProvider and GradientDrawable (zero reflection overhead)
        val searchInput = root.findViewById<View>(com.giphy.sdk.ui.R.id.searchInput)
        val searchBar = searchInput?.parent as? ViewGroup
        if (searchBar != null) {
            makeSearchBarPill(searchBar)
        }

        // 2. Style GIFs in RecyclerView to have rounded corners
        findViewsByClass(root, RecyclerView::class.java).forEach { recyclerView ->
            recyclerView.addOnChildAttachStateChangeListener(object : RecyclerView.OnChildAttachStateChangeListener {
                override fun onChildViewAttachedToWindow(view: View) {
                    val gifView = view as? GifView ?: (view as? ViewGroup)?.let { findFirstGifView(it) }
                    if (gifView != null) {
                        gifView.cornerRadius = gifRadiusPx
                        gifView.outlineProvider = object : ViewOutlineProvider() {
                            override fun getOutline(v: View, outline: Outline) {
                                outline.setRoundRect(0, 0, v.width, v.height, gifRadiusPx)
                            }
                        }
                        gifView.clipToOutline = true
                    }
                }

                override fun onChildViewDetachedFromWindow(view: View) {}
            })
        }
    }

    private fun makeSearchBarPill(searchBar: ViewGroup) {
        val density = searchBar.resources.displayMetrics.density
        val defaultPillRadius = 24f * density

        // Hardware-accelerated pill outline
        searchBar.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val r = if (view.height > 0) view.height / 2f else defaultPillRadius
                outline.setRoundRect(0, 0, view.width, view.height, r)
            }
        }
        searchBar.clipToOutline = true

        // Keep background drawable pill-shaped
        val currentBgColor = (searchBar.background as? ColorDrawable)?.color ?: 0xFF2E2E2E.toInt()
        val pillBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 1000f
            setColor(currentBgColor)
        }
        searchBar.background = pillBg
    }

    private fun findFirstGifView(group: ViewGroup): GifView? {
        for (i in 0 until group.childCount) {
            val child = group.getChildAt(i)
            if (child is GifView) return child
            if (child is ViewGroup) {
                val found = findFirstGifView(child)
                if (found != null) return found
            }
        }
        return null
    }

    private fun <T : View> findViewsByClass(root: View, clazz: Class<T>): List<T> {
        val results = mutableListOf<T>()
        if (clazz.isInstance(root)) {
            results.add(clazz.cast(root)!!)
        }
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                results.addAll(findViewsByClass(root.getChildAt(i), clazz))
            }
        }
        return results
    }
}
