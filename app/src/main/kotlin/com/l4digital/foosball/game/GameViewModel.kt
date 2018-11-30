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

import android.arch.lifecycle.ViewModel
import android.databinding.ObservableBoolean
import com.l4digital.foosball.R
import com.l4digital.foosball.game.model.*
import com.l4digital.foosball.util.ViewModelFactory
import com.l4digital.foosball.util.removeLast
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

enum class TableSide { LEFT, RIGHT }

class ScoreViewState(
        val topPlayer: Player,
        val bottomPlayer: Player,
        val color: Int,
        val score: Int = 0,
        val roundsWon: Int = 0)

class GameViewModel(private val api: FoosballApi) : ViewModel() {

    sealed class State(val soundEffect: SoundEffect? = null) {
        class NoGameState(val gameWasSubmitted: Boolean = false) : State()
        class GameOverState(val winners: Team,
                            val losers: Team,
                            val failedToSubmit: Boolean = false) : State(if(failedToSubmit) null else SoundEffect.GAME_OVER)

        class GameState(val leftScoreState: ScoreViewState,
                        val rightScoreState: ScoreViewState,
                        val gameStateChanged: Boolean,
                        soundEffect: SoundEffect? = null) : State(soundEffect)
    }

    private sealed class Intent(val game: Game) {
        class NewGameIntent(game: Game, val leftTeam: Team, val rightTeam: Team) : Intent(game)
        class SubmitGameIntent(game: Game) : Intent(game)
        class EndGameIntent(game: Game) : Intent(game)
        class UndoPointIntent(game: Game, val wasGameOver: Boolean) : Intent(game)
        class SwapSidesIntent(game: Game) : Intent(game)
        class SwapPlayersIntent(game: Game, val tableSide: TableSide) : Intent(game)
        class ScorePointIntent(game: Game, val player: Player, val tableSide: TableSide, val isGoat: Boolean) : Intent(game)
    }

    private val intentSubject = PublishSubject.create<Intent>()
    private var game: Game? = null

    var isAnimationInProgress: Boolean = false
    val submitInProgress = ObservableBoolean(false)
    val hasGame get() = game != null
    val isGameOver get() = game?.run { winners != null } ?: false

    private var topLeftPlayer: Player? = null
    private var bottomLeftPlayer: Player? = null
    private var topRightPlayer: Player? = null
    private var bottomRightPlayer: Player? = null
    private var leftColor: Int = 0
    private var rightColor: Int = 0

    fun state(): Observable<State> = intentSubject
            .concatMap {
                if (isAnimationInProgress) Observable.empty<Intent>()
                else Observable.fromCallable {
                    when (it) {
                        is Intent.NewGameIntent -> {
                            game = it.game
                            bottomLeftPlayer = it.leftTeam.playerA
                            topLeftPlayer = it.leftTeam.playerB
                            leftColor = it.leftTeam.teamColor
                            topRightPlayer = it.rightTeam.playerA
                            bottomRightPlayer = it.rightTeam.playerB
                            rightColor = it.rightTeam.teamColor
                        }
                        is Intent.EndGameIntent -> game = null
                        is Intent.ScorePointIntent -> it.game.apply {
                            scorePoint(it.player, it.player.isOffense(it.tableSide), it.isGoat)
                            if (rounds.size > 1 && rounds.last().points.isEmpty()) {
                                swapSidesInternal()
                            }
                        }
                        is Intent.UndoPointIntent -> it.game.apply {
                            if (rounds.size > 1 && rounds.last().points.isEmpty()) {
                                swapSidesInternal()
                            }
                            undo()
                        }
                        is Intent.SwapSidesIntent -> swapSidesInternal()
                        is Intent.SwapPlayersIntent -> when (it.tableSide) {
                            TableSide.LEFT -> topLeftPlayer = bottomLeftPlayer.also { bottomLeftPlayer = topLeftPlayer }
                            TableSide.RIGHT -> topRightPlayer = bottomRightPlayer.also { bottomRightPlayer = topRightPlayer }
                        }
                    }
                    it
                }
            }
            .switchMap {
                when (it) {
                    is Intent.NewGameIntent,
                    is Intent.SwapPlayersIntent,
                    is Intent.SwapSidesIntent,
                    is Intent.UndoPointIntent -> Observable.just(it.game.state(it))
                    is Intent.ScorePointIntent -> Observable.just(it.game.run {
                        winnersAndLosers?.apply { return@run State.GameOverState(first, second) }
                        val round = rounds.lastRoundWithPoints
                        val isRoundOver = round?.winners != null
                        val isRoundSkunk = isRoundOver && (round?.isSkunk ?: false)
                        state(it, when {
                            isRoundSkunk -> SoundEffect.SKUNK
                            it.isGoat -> SoundEffect.GOAT
                            else -> SoundEffect.SCORE
                        })
                    })
                    is Intent.SubmitGameIntent -> submitInProgress.set(true).run {
                        val errorState = it.game.winnersAndLosers?.run { State.GameOverState(first, second, true) }
                                ?: State.NoGameState(true)
                        api.submitGame(it.game.toApiModel())
                                .toSingleDefault<State>(State.NoGameState(true))
                                .onErrorReturnItem(errorState)
                                .doOnEvent { _, _ -> submitInProgress.set(false) }
                                .toObservable()
                    }
                    is Intent.EndGameIntent -> Observable.just(State.NoGameState())
                }
            }

    fun startGame(selectedPlayers: List<Player>) {
        val leftTeam = Team(selectedPlayers[0], selectedPlayers[1], R.color.team_a)
        val rightTeam = Team(selectedPlayers[2], selectedPlayers[3], R.color.team_b)

        Game(GameParameters(), leftTeam, rightTeam).apply {
            newRound()
            intentSubject.onNext(Intent.NewGameIntent(this, leftTeam, rightTeam))
        }
    }

    fun endGame() = game?.run { intentSubject.onNext(Intent.EndGameIntent(this)) }

    fun scorePoint(player: Player, tableSide: TableSide, isGoat: Boolean = false) = game?.let {
        intentSubject.onNext(Intent.ScorePointIntent(it, player, tableSide, isGoat))
        true
    } ?: false

    fun undo() = game?.run { intentSubject.onNext(Intent.UndoPointIntent(this, winners != null)) }

    fun submit() = game?.run { intentSubject.onNext(Intent.SubmitGameIntent(this)) }

    fun swapPlayers(tableSide: TableSide) = game?.run { intentSubject.onNext(Intent.SwapPlayersIntent(this, tableSide)) }

    fun swapSides() = game?.run { intentSubject.onNext(Intent.SwapSidesIntent(this)) }

    fun getPlayerIcon(player: Player?): Any? = player?.icon

    fun getPlayerScore(player: Player?) = player?.let { game?.getPlayerScore(it) } ?: 0

    private fun swapSidesInternal() {
        topLeftPlayer = topRightPlayer.also { topRightPlayer = topLeftPlayer }
        bottomLeftPlayer = bottomRightPlayer.also { bottomRightPlayer = bottomLeftPlayer }
        leftColor = rightColor.also { rightColor = leftColor }
    }

    // Extension functions
    private fun Player.isOffense(tableSide: TableSide): Boolean = when (tableSide) {
        TableSide.LEFT -> this == bottomLeftPlayer
        TableSide.RIGHT -> this == topRightPlayer
    }

    private val Player.goats: Int
        get() {
            return game?.rounds
                    ?.flatMap { it.points }
                    ?.filter { it.player == this }
                    ?.map { it.isGoat }
                    ?.fold(0, { goats, isGoat ->
                        if (isGoat) goats + 1 else 0
                    }) ?: 0
        }

    private val Player.icon: Any
        get() = when (goats) {
            0 -> profile.largeIconUrl
            1 -> R.drawable.goat
            2 -> R.drawable.double_goat
            else -> R.drawable.triple_goat
        }

    private fun Game.scorePoint(player: Player, isOffense: Boolean, isGoat: Boolean = false) {
        val point = Point(player, getTeam(player) ?: return, isOffense, isGoat)
        val round = rounds.last()
        round.points.add(point)
        if (round.winners != null && winners == null) newRound()
    }

    private fun Game.undo() {
        if (rounds.size > 1 || rounds[0].points.isNotEmpty()) {
            val roundUndo = rounds.last().points.isEmpty()
            if (roundUndo) rounds.removeLast()
            rounds.last().points.removeLast()
        }
    }

    private fun Game.getScoreState(tableSide: TableSide): ScoreViewState? {
        val isLeftSide = tableSide == TableSide.LEFT
        val topPlayer = if (isLeftSide) topLeftPlayer else topRightPlayer
        val bottomPlayer = if (isLeftSide) bottomLeftPlayer else bottomRightPlayer
        val teamColor = if (isLeftSide) leftColor else rightColor
        return if (topPlayer != null && bottomPlayer != null) {
            val team = getTeam(topPlayer)
            val score = team?.let { rounds.last().getTeamScore(it) } ?: 0
            val roundsWon = team?.roundsWon ?: 0
            ScoreViewState(topPlayer, bottomPlayer, teamColor, score, roundsWon)
        } else null
    }

    private fun Game.state(intent: Intent, soundEffect: SoundEffect? = null): State {
        val leftScoreState = getScoreState(TableSide.LEFT) ?: return State.NoGameState()
        val rightScoreState = getScoreState(TableSide.RIGHT) ?: return State.NoGameState()
        val gameStateChanged = intent is Intent.NewGameIntent || (intent is Intent.UndoPointIntent && intent.wasGameOver)
        return State.GameState(leftScoreState, rightScoreState, gameStateChanged, soundEffect)
    }

    private val Game.winnersAndLosers: Pair<Team, Team>?
        get() = winners?.let { winners -> losers?.let { losers -> winners to losers } }

    class Factory @Inject constructor(private val api: FoosballApi)
        : ViewModelFactory<GameViewModel>() {

        override val modelClass: Class<GameViewModel> get() = GameViewModel::class.java

        @Suppress("UNCHECKED_CAST")
        @Throws(IllegalArgumentException::class)
        override fun <T : ViewModel> create(modelClass: Class<T>) =
                if (modelClass.isAssignableFrom(this.modelClass)) {
                    GameViewModel(api) as T
                } else throw IllegalArgumentException(illegalArgumentError)
    }
}