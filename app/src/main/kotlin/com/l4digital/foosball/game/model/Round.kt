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

@Suppress("unused")
class Round(
        val teamA: Team,
        val teamB: Team,
        val roundNumber: Int,
        val points: MutableList<Point> = mutableListOf()
)

fun Round.getTeamScore(team: Team) =
        points.filter { it.team == team && !it.isGoat }.size +
                points.filter { it.team != team && it.isGoat }.size

val Round.isSkunk get() = getTeamScore(teamA) == 0 || getTeamScore(teamB) == 0

val MutableList<Round>.lastRoundWithPoints
    get () = last().takeIf { it.points.isNotEmpty() } ?: getOrNull(size - 2)
