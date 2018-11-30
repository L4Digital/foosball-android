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

@file:Suppress("unused")

package com.l4digital.foosball.game.model

import com.squareup.moshi.Json

class TeamApiModel(
        @Json(name = "playerA") val playerAId: String,
        @Json(name = "playerB") val playerBId: String
)

class PointApiModel(
        @Json(name = "isSelfGoal") val isGoat: Boolean,
        @Json(name = "user_id") val playerId: String
)

class RoundApiModel(
        @Json(name = "round") val roundNumber: Int,
        @Json(name = "goals") val points: List<PointApiModel>
)

class GameApiModel(
        val rounds: List<RoundApiModel>,
        val teams: List<TeamApiModel>
)

fun Game.toApiModel() = GameApiModel(
        rounds.map {
            RoundApiModel(
                    it.roundNumber,
                    it.points.map { PointApiModel(it.isGoat, it.playerId) })
        },
        teams.map { TeamApiModel(it.playerA.id, it.playerB.id) }
)