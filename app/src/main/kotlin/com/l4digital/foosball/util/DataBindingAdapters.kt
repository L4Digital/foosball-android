/*
 * Copyright 2018 L4 Digital. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.l4digital.foosball.util

import android.annotation.SuppressLint
import android.databinding.BindingAdapter
import android.graphics.drawable.Drawable
import android.net.Uri
import android.support.annotation.ColorRes
import android.support.annotation.FontRes
import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView

object DataBindingAdapters {

    @JvmStatic
    @BindingAdapter("android:visibility")
    fun setVisibility(view: View, visible: Boolean) {
        view.visibility = if (visible) View.VISIBLE else View.GONE
    }

    @JvmStatic
    @BindingAdapter("android:src")
    fun setImageSrc(view: CircleImageView, src: Any?) {
        when (src) {
            is String -> Glide.with(view.context).load(src).into(view)
            is Uri -> Glide.with(view.context).load(src).into(view)
            is Drawable -> view.setImageDrawable(src)
            is Int -> view.setImageResource(src)
            else -> view.setImageDrawable(null)
        }
    }

    @JvmStatic
    @BindingAdapter("android:text")
    fun setTextViewText(view: TextView, @StringRes res: Int) {
        if (res != 0) view.text = view.context.getString(res)
    }

    @JvmStatic
    @BindingAdapter("android:fontFamily")
    fun setTextFont(view: TextView, @FontRes res: Int) {
        if (res != 0) view.typeface = ResourcesCompat.getFont(view.context, res)
    }

    @JvmStatic
    @BindingAdapter("android:textColor")
    fun setTextColor(view: TextView, @ColorRes res: Int) {
        if (res != 0) view.setTextColor(ContextCompat.getColor(view.context, res))
    }

    @JvmStatic
    @BindingAdapter("civ_border_color")
    fun setBorderColor(view: CircleImageView, @ColorRes res: Int) {
        if (res != 0) view.borderColor = ContextCompat.getColor(view.context, res)
    }

    @JvmStatic
    @BindingAdapter("android:tint")
    fun setImageViewText(view: ImageView, @ColorRes res: Int) {
        if (res != 0) view.setTint(res)
    }

    @JvmStatic
    @BindingAdapter("background_tint")
    fun setTextViewBackgroundTint(view: TextView, @ColorRes res: Int) {
        if (res != 0) view.background = DrawableCompat.wrap(view.background).mutate().apply {
            DrawableCompat.setTint(this, ContextCompat.getColor(view.context, res))
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @JvmStatic
    @BindingAdapter("touchTint")
    fun setImageTouchTint(view: ImageView, @ColorRes res: Int) {
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> view.setTint(res)
                MotionEvent.ACTION_UP -> view.setTint(android.R.color.transparent)
            }
            false
        }
    }

    @JvmStatic
    @BindingAdapter("roundsWon", "roundsColor")
    fun setRoundsWon(view: LinearLayout, roundsWon: Int, @ColorRes teamColor: Int) {
        view.forChildIndexed { i, childView: View ->
            if (childView is ImageView) {
                childView.setTint(if (roundsWon > i) teamColor else android.R.color.transparent)
            }
        }
    }
}