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

class GameParameters(val pointsInRound: Int = 5, val winBy: Int = 2, val roundsNeededToWin: Int = 2)

class Game(
        private val params: GameParameters,
        val teamA: Team,
        val teamB: Team,
        val rounds: MutableList<Round> = mutableListOf()
) {
    val teams: Array<Team> = arrayOf(teamA, teamB)

    val Round.winners
        get() = params.let { p ->
            getTeamScore(teamA).let { scoreA ->
                getTeamScore(teamB).let { scoreB ->
                    teamA.takeIf { scoreA >= p.pointsInRound && scoreA >= scoreB + p.winBy } ?:
                            teamB.takeIf { scoreB >= p.pointsInRound && scoreB >= scoreA + p.winBy }
                }
            }
        }

    val Team.roundsWon get() = rounds.filter { round -> round.winners == this }.size

    val Game.winners get() = teams.find { it.roundsWon >= params.roundsNeededToWin }

    val Game.losers get() = when (winners) { teamA -> teamB; teamB -> teamA; else -> null }
}

fun Game.newRound() = rounds.add(Round(teamA, teamB, rounds.size + 1))

fun Game.getTeam(player: Player) = teams.find { it.hasPlayer(player) }

fun Game.getPlayerScore(player: Player) = rounds.sumBy {
    it.points.filter { !it.isGoat && it.player.id == player.id }.size
}


