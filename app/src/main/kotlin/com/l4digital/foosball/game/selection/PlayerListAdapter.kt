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

import android.databinding.ViewDataBinding
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.l4digital.foosball.databinding.ItemPlayerNoNameBinding
import com.l4digital.foosball.databinding.ItemPlayerWithNameBinding
import com.l4digital.foosball.game.TableSide
import com.l4digital.foosball.game.model.Player

class PlayerListAdapter(
        private val viewModel: PlayerSelectionViewModel,
        private val showOnlyIcons: Boolean = false,
        private var players: MutableList<Player> = mutableListOf()
) : RecyclerView.Adapter<PlayerListAdapter.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        return LayoutInflater.from(viewGroup.context).let {
            val tableSide = viewGroup.tag as TableSide
            if (showOnlyIcons) ViewHolder(ItemPlayerNoNameBinding.inflate(it, viewGroup, false), tableSide)
            else ViewHolder(ItemPlayerWithNameBinding.inflate(it, viewGroup, false), tableSide)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            holder.bind(players[position], viewModel)

    override fun getItemCount() = players.size

    override fun getItemId(position: Int) = players[position].id.hashCode().toLong()

    fun setPlayers(players: List<Player>) {
        DiffUtil.calculateDiff(DiffCallback(this.players, players)).let {
            this.players.clear()
            this.players.addAll(players)
            it.dispatchUpdatesTo(this)
        }
    }

    class ViewHolder(private val binding: ViewDataBinding, private val side: TableSide) : RecyclerView.ViewHolder(binding.root) {
        fun bind(player: Player, viewModel: PlayerSelectionViewModel) {
            when (binding) {
                is ItemPlayerNoNameBinding -> binding.apply {
                    this.player = player
                    this.tableSide = side
                    this.viewModel = viewModel
                }
                is ItemPlayerWithNameBinding -> binding.apply {
                    this.player = player
                    this.tableSide = side
                    this.viewModel = viewModel
                }
            }
            binding.executePendingBindings()
        }
    }

    class DiffCallback(private val oldItems: List<Player>, private val newItems: List<Player>) : DiffUtil.Callback() {
        override fun getOldListSize() = oldItems.size

        override fun getNewListSize() = newItems.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                oldItems.getOrNull(oldItemPosition)?.id == newItems.getOrNull(newItemPosition)?.id

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                oldItems.getOrNull(oldItemPosition) == newItems.getOrNull(newItemPosition)
    }
}
