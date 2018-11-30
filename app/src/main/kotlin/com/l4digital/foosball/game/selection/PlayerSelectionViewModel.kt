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

package com.l4digital.foosball.game.selection

import android.arch.lifecycle.ViewModel
import android.databinding.ObservableBoolean
import com.l4digital.foosball.R
import com.l4digital.foosball.game.FoosballApi
import com.l4digital.foosball.game.TableSide
import com.l4digital.foosball.game.model.Player
import com.l4digital.foosball.util.ViewModelFactory
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class PlayerSelectionViewModel(private val api: FoosballApi) : ViewModel() {

    class State(val selectedPlayers: List<Player?>, val summonedPlayer: Player?)

    private sealed class Intent {
        object ClearIntent : Intent()
        object RandomizeIntent : Intent()
        class SelectPlayerIntent(val player: Player, val tableSide: TableSide) : Intent()
        class RemovePlayerIntent(val isOffense: Boolean, val tableSide: TableSide) : Intent()
        class SummonIntent(val player: Player) : Intent()
    }

    val selectedPlayers = mutableListOf<Player?>(null, null, null, null)
    val readyToStart = ObservableBoolean(false)
    val randomizeInProgress = ObservableBoolean(false)

    private val intentSubject = PublishSubject.create<Intent>()

    fun state(): Observable<State> = intentSubject.flatMap {
        when (it) {
            is Intent.SelectPlayerIntent -> Observable.fromCallable {
                if (!selectedPlayers.contains(it.player)) {
                    val index = when (it.tableSide) {
                        TableSide.LEFT -> if (selectedPlayers[0] == null) 0 else 1
                        TableSide.RIGHT -> if (selectedPlayers[2] == null) 2 else 3
                    }
                    if (selectedPlayers[index] == null) {
                        selectedPlayers[index] = it.player
                    }
                }
                State(selectedPlayers, null)
            }
            is Intent.ClearIntent -> Observable.fromCallable {
                for (i in selectedPlayers.indices) {
                    selectedPlayers[i] = null
                }
                State(selectedPlayers, null)
            }
            is Intent.RemovePlayerIntent -> Observable.fromCallable {
                val index = when (it.tableSide) {
                    TableSide.LEFT -> if (it.isOffense) 0 else 1
                    TableSide.RIGHT -> if (it.isOffense) 2 else 3
                }
                selectedPlayers[index] = null
                State(selectedPlayers, null)
            }
            is Intent.SummonIntent -> api.summon(it.player.name)
                    .toSingleDefault(State(selectedPlayers, it.player))
                    .onErrorReturnItem(PlayerSelectionViewModel.State(selectedPlayers, null))
                    .toObservable()
            is Intent.RandomizeIntent -> Observable.interval(0, 100, TimeUnit.MILLISECONDS)
                    .doOnNext { if (it == 0L) randomizeInProgress.set(true) }
                    .take(10)
                    .map { State(selectedPlayers.apply { shuffle() }, null) }
                    .doOnComplete { randomizeInProgress.set(false) }
        }
    }.doOnNext { readyToStart.set(it.selectedPlayers.filterNotNull().size == 4) }

    fun removePlayer(isOffense: Boolean, tableSide: TableSide) =
            intentSubject.onNext(Intent.RemovePlayerIntent(isOffense, tableSide))

    fun selectPlayer(player: Player, tableSide: TableSide) =
            intentSubject.onNext(Intent.SelectPlayerIntent(player, tableSide))

    fun clear() = intentSubject.onNext(Intent.ClearIntent)

    fun randomize() = intentSubject.onNext(Intent.RandomizeIntent)

    fun summonPlayer(player: Player?) = player?.let {
        intentSubject.onNext(Intent.SummonIntent(player))
    }

    fun getSelectionLabel(offensivePlayer: Player?, defensivePlayer: Player?) = when {
        offensivePlayer == null -> R.string.who_is_offense
        defensivePlayer == null -> R.string.who_is_defense
        else -> R.string.ready
    }

    fun getPlayerIcon(player: Player?) = player?.profile?.smallIconUrl ?: R.drawable.empty_player

    class Factory @Inject constructor(private val api: FoosballApi)
        : ViewModelFactory<PlayerSelectionViewModel>() {

        override val modelClass: Class<PlayerSelectionViewModel> get() = PlayerSelectionViewModel::class.java

        @Suppress("UNCHECKED_CAST")
        @Throws(IllegalArgumentException::class)
        override fun <T : ViewModel> create(modelClass: Class<T>) =
                if (modelClass.isAssignableFrom(this.modelClass)) {
                    PlayerSelectionViewModel(api) as T
                } else throw IllegalArgumentException(illegalArgumentError)
    }
}
