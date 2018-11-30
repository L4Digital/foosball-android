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

package com.l4digital.foosball

import android.content.Context
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.widget.Toast
import com.l4digital.foosball.common.DividerItemDecoration
import com.l4digital.foosball.databinding.ActivityFoosballBinding
import com.l4digital.foosball.databinding.ViewPlayerSelectionBinding
import com.l4digital.foosball.game.GameViewModel
import com.l4digital.foosball.game.TableSide
import com.l4digital.foosball.game.play
import com.l4digital.foosball.game.selection.PlayerListAdapter
import com.l4digital.foosball.game.selection.PlayerListViewModel
import com.l4digital.foosball.game.selection.PlayerSelectionViewModel
import com.l4digital.foosball.util.showGameRecapView
import com.l4digital.foosball.util.showScoreView
import com.l4digital.foosball.util.showSelectionView
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import toothpick.Toothpick
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class FoosballActivity : AppCompatActivity() {

    @Inject lateinit var gameViewModelFactory: GameViewModel.Factory
    @Inject lateinit var listViewModelFactory: PlayerListViewModel.Factory
    @Inject lateinit var selectionViewModelFactory: PlayerSelectionViewModel.Factory

    private val gameViewModel by lazy { gameViewModelFactory.create(GameViewModel::class.java) }
    private val listViewModel by lazy { listViewModelFactory.create(PlayerListViewModel::class.java) }
    private val selectionViewModel by lazy { selectionViewModelFactory.create(PlayerSelectionViewModel::class.java) }
    private val allPlayersAdapter by lazy { PlayerListAdapter(selectionViewModel, false) }
    private val recentPlayersAdapter by lazy { PlayerListAdapter(selectionViewModel, true) }

    private lateinit var binding: ActivityFoosballBinding
    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Toothpick.openScopes(ApplicationScope::class.java, this).let {
            Toothpick.inject(this, it)
        }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_foosball)
        binding.gameViewModel = gameViewModel
        binding.selectionViewModel = selectionViewModel
        binding.leftPlayerSelection.initPlayerSelectionView(this, TableSide.LEFT)
        binding.rightPlayerSelection.initPlayerSelectionView(this, TableSide.RIGHT)
    }

    override fun onResume() {
        super.onResume()

        listViewModel.state()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { updatePlayerAdapterState(it) }
                .let { disposables.add(it) }

        selectionViewModel.state()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { updatePlayerSelectionState(it) }
                .let { disposables.add(it) }

        gameViewModel.state()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { updateGameState(it) }
                .observeOn(Schedulers.io())
                .flatMapCompletable { it.soundEffect?.play(this) ?: Completable.complete() }
                .subscribe()
                .let { disposables.add(it) }

        Observable.interval(0, 2, TimeUnit.MINUTES)
                .subscribeOn(Schedulers.io())
                .subscribe { listViewModel.loadPlayers(it % 15 == 0L) }
                .let { disposables.add(it) }
    }

    override fun onPause() {
        super.onPause()
        disposables.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        Toothpick.closeScope(this)
    }

    override fun onBackPressed() {
        if(!gameViewModel.hasGame) super.onBackPressed()
        else if(gameViewModel.isGameOver) gameViewModel.undo()
        else gameViewModel.endGame()
    }

    private fun ViewPlayerSelectionBinding.initPlayerSelectionView(context: Context, tableSide: TableSide) {
        allPlayersList.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        allPlayersList.addItemDecoration(DividerItemDecoration(context, resources.getDimension(R.dimen.standard_padding).toInt()))
        allPlayersList.tag = tableSide
        allPlayersList.adapter = allPlayersAdapter

        recentPlayersList.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recentPlayersList.tag = tableSide
        recentPlayersList.adapter = recentPlayersAdapter
    }

    private fun updatePlayerAdapterState(state: PlayerListViewModel.State) {
        allPlayersAdapter.setPlayers(state.players)
        recentPlayersAdapter.setPlayers(state.recentPlayers.take(10))
    }

    private fun updatePlayerSelectionState(state: PlayerSelectionViewModel.State) {
        state.summonedPlayer?.let {
            Toast.makeText(this, getString(R.string.summoned, it.name), Toast.LENGTH_SHORT).show()
        }
        listOf(binding.leftPlayerSelection to 0, binding.rightPlayerSelection to 2)
                .forEach { (selectionView, index) ->
                    selectionView.offensivePlayer = state.selectedPlayers[index]
                    selectionView.defensivePlayer = state.selectedPlayers[index + 1]
                }
    }

    private fun updateGameState(state: GameViewModel.State) = when (state) {
        is GameViewModel.State.GameState -> binding.showScoreView(gameViewModel, state)
        is GameViewModel.State.GameOverState -> binding.showGameRecapView(gameViewModel, state)
        is GameViewModel.State.NoGameState -> {
            if (state.gameWasSubmitted) {
                gameViewModel.endGame()
                selectionViewModel.clear()
            }
            binding.showSelectionView(gameViewModel)
        }
    }
}
