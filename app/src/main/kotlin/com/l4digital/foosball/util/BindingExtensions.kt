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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.support.annotation.LayoutRes
import android.support.annotation.TransitionRes
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.transition.Transition
import android.transition.TransitionInflater
import android.transition.TransitionManager
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import com.l4digital.foosball.R
import com.l4digital.foosball.databinding.ActivityFoosballBinding
import com.l4digital.foosball.databinding.ViewTeamScoreBinding
import com.l4digital.foosball.game.GameViewModel
import com.l4digital.foosball.game.ScoreViewState

private const val SWAP_ANIMATION_DURATION = 600L

inline fun ConstraintLayout.transition(@LayoutRes id: Int,
                                       @TransitionRes transitionId: Int,
                                       crossinline preBlock: () -> Unit = {},
                                       crossinline postBlock: () -> Unit = {}) {
    TransitionInflater.from(context).inflateTransition(transitionId).addListener(
            object : Transition.TransitionListener {
                override fun onTransitionEnd(transition: Transition?) = postBlock()

                override fun onTransitionResume(transition: Transition?) = Unit

                override fun onTransitionPause(transition: Transition?) = Unit

                override fun onTransitionCancel(transition: Transition?) = Unit

                override fun onTransitionStart(transition: Transition?) = Unit
            }).let { TransitionManager.beginDelayedTransition(this, it) }
    preBlock()
    ConstraintSet().apply { clone(context, id) }.applyTo(this)
}

fun ActivityFoosballBinding.showScoreView(viewModel: GameViewModel, state: GameViewModel.State.GameState) {
    val updateCenterPanel = {
        centerPanel.apply {
            gameInProgress = true
            leftColor = state.leftScoreState.color
            rightColor = state.rightScoreState.color
            leftRoundsWon = state.leftScoreState.roundsWon
            rightRoundsWon = state.rightScoreState.roundsWon
        }
    }
    if (state.gameStateChanged) {
        foosballRootView.transition(R.layout.activity_foosball_in_game_state, R.transition.game_transition, {
            viewModel.isAnimationInProgress = true
            updateCenterPanel()
        }, {
            viewModel.isAnimationInProgress = false
        })
    } else updateCenterPanel()

    val needsSideSwap = leftGameScore.state?.topPlayer == state.rightScoreState.topPlayer
    if (needsSideSwap) swapSides(viewModel, state)
    else listOf(leftGameScore to state.leftScoreState, rightGameScore to state.rightScoreState)
            .forEach { (gameScore, state) ->
                val needsSwap = gameScore.state?.bottomPlayer == state.topPlayer
                if (needsSwap) gameScore.swapPlayers(viewModel, state)
                else gameScore.state = state
            }
}

fun ActivityFoosballBinding.showSelectionView(viewModel: GameViewModel) {
    foosballRootView.transition(R.layout.activity_foosball, R.transition.game_transition, {
        viewModel.isAnimationInProgress = true
        centerPanel.apply {
            gameInProgress = false
            leftColor = 0
            rightColor = 0
            leftRoundsWon = 0
            rightRoundsWon = 0
        }
    }, {
        viewModel.isAnimationInProgress = false
        leftGameScore.state = null
        rightGameScore.state = null
    })
}

fun ActivityFoosballBinding.showGameRecapView(viewModel: GameViewModel, state: GameViewModel.State.GameOverState) {
    if (state.failedToSubmit) {
        Toast.makeText(root.context, R.string.failed_submit, Toast.LENGTH_SHORT).show()
        return
    }
    foosballRootView.transition(R.layout.activity_foosball_game_recap_state, R.transition.game_transition, {
        viewModel.isAnimationInProgress = true
        gameRecap.apply {
            winners = state.winners
            losers = state.losers
        }
    }, {
        viewModel.isAnimationInProgress = false
    })
}

private fun ViewTeamScoreBinding.swapPlayers(gameViewModel: GameViewModel, newState: ScoreViewState) {
    val yDelta = bottomPlayerIcon.y - topPlayerIcon.y
    topPlayerIcon.clearAnimation()
    topPlayerName.clearAnimation()
    bottomPlayerIcon.clearAnimation()
    bottomPlayerName.clearAnimation()
    AnimatorSet().apply {
        interpolator = OvershootInterpolator()
        playTogether(
                ObjectAnimator.ofFloat(topPlayerIcon, "translationY", yDelta),
                ObjectAnimator.ofFloat(topPlayerName, "translationY", yDelta),
                ObjectAnimator.ofFloat(bottomPlayerIcon, "translationY", -yDelta),
                ObjectAnimator.ofFloat(bottomPlayerName, "translationY", -yDelta))

        duration = SWAP_ANIMATION_DURATION
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator?) {
                gameViewModel.isAnimationInProgress = true
            }

            override fun onAnimationEnd(a: Animator?) {
                gameViewModel.isAnimationInProgress = false
                root.post {
                    topPlayerIcon.translationY = 0f
                    topPlayerName.translationY = 0f
                    bottomPlayerIcon.translationY = 0f
                    bottomPlayerName.translationY = 0f
                    state = newState
                }
            }
        })
    }.start()
}

private fun ActivityFoosballBinding.swapSides(gameViewModel: GameViewModel, newState: GameViewModel.State.GameState) {
    val xDelta = rightGameScore.root.x - leftGameScore.root.x
    AnimatorSet().apply {
        interpolator = OvershootInterpolator()
        playTogether(
                ObjectAnimator.ofFloat(leftGameScore.root, "translationX", xDelta),
                ObjectAnimator.ofFloat(rightGameScore.root, "translationX", -xDelta))

        duration = SWAP_ANIMATION_DURATION
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator?) {
                gameViewModel.isAnimationInProgress = true
            }

            override fun onAnimationEnd(a: Animator?) {
                gameViewModel.isAnimationInProgress = false
                root.post {
                    leftGameScore.root.translationX = 0f
                    rightGameScore.root.translationX = 0f
                    leftGameScore.state = newState.leftScoreState
                    rightGameScore.state = newState.rightScoreState
                }
            }
        })
    }.start()
}