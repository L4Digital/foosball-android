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

package com.l4digital.foosball.game

import android.content.Context
import android.media.MediaPlayer
import android.support.annotation.RawRes
import com.l4digital.foosball.R
import io.reactivex.Completable

enum class SoundEffect(@RawRes val resId: Int) {
    SCORE(R.raw.score),
    SKUNK(R.raw.angry),
    GOAT(R.raw.goat),
    GAME_OVER(R.raw.fanfare);
}

fun SoundEffect.play(context: Context): Completable {
    return Completable.using(
            { MediaPlayer.create(context, resId) },
            { mediaPlayer ->
                Completable.create { emitter ->
                    mediaPlayer.apply { setOnCompletionListener { emitter.onComplete() } }.start()
                }
            }, MediaPlayer::release)
}