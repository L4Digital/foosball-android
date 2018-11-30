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
import com.l4digital.foosball.game.FoosballApi
import com.l4digital.foosball.game.model.Player
import com.l4digital.foosball.util.ViewModelFactory
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class PlayerListViewModel(private val api: FoosballApi) : ViewModel() {

    class State(val players: List<Player>, val recentPlayers: List<Player>)

    private val playerMap = mutableMapOf<String, Player>()
    private val triggerSubject = PublishSubject.create<Boolean>()

    fun loadPlayers(reloadAll: Boolean) = triggerSubject.onNext(reloadAll)

    fun state(): Observable<State> = triggerSubject
            .flatMapSingle { getPlayers(it) }
            .flatMapSingle { players -> getRecentPlayers().map { State(players, it) } }

    private fun getPlayers(reloadAllPlayers: Boolean = false): Single<List<Player>> {
        val cachedPlayers = Single.fromCallable { playerMap.values.toList() }
        return if (reloadAllPlayers || playerMap.isEmpty()) api.getActivePlayers().doOnSuccess {
            it.takeUnless { it.isEmpty() }?.associateBy({ it.id }, { it })?.apply {
                playerMap.clear()
                playerMap.putAll(this)
            }
        }.onErrorResumeNext(cachedPlayers)
        else cachedPlayers
    }

    private fun getRecentPlayers(): Single<List<Player>> =
            api.getRecentPlayers().map { it.mapNotNull { playerMap[it.playerId] } }
                    .let { players ->
                        if (playerMap.isEmpty()) getPlayers().flatMap { players } else players
                    }
                    .onErrorReturnItem(listOf())


    class Factory @Inject constructor(private val api: FoosballApi)
        : ViewModelFactory<PlayerListViewModel>() {

        override val modelClass: Class<PlayerListViewModel> get() = PlayerListViewModel::class.java

        @Suppress("UNCHECKED_CAST")
        @Throws(IllegalArgumentException::class)
        override fun <T : ViewModel> create(modelClass: Class<T>) =
                if (modelClass.isAssignableFrom(this.modelClass)) {
                    PlayerListViewModel(api) as T
                } else throw IllegalArgumentException(illegalArgumentError)
    }
}