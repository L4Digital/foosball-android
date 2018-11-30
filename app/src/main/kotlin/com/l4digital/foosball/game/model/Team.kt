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

package com.l4digital.foosball.game.model

import android.support.annotation.ColorRes

@Suppress("unused")
class Team(
        val playerA: Player,
        val playerB: Player,
        @ColorRes val teamColor: Int
)

fun Team.hasPlayer(player: Player) = playerA.id == player.id || playerB.id == player.id
