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

import android.support.v4.content.ContextCompat
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

inline fun ViewGroup.forChild(block: (View) -> Unit) =
        (0 until childCount).forEach { block(getChildAt(it)) }

inline fun ViewGroup.forChildIndexed(block: (Int, View) -> Unit) =
        (0 until childCount).forEach { block(it, getChildAt(it)) }

fun ImageView.setTint(color: Int) = setColorFilter(ContextCompat.getColor(context, color))

fun <T> MutableList<T>.removeLast() = if (isNotEmpty()) removeAt(size - 1) else null
