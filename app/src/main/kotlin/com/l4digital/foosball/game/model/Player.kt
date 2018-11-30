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

import com.squareup.moshi.Json

class PlayerProfile(
        @Json(name = "image_72") val smallIconUrl: String,
        @Json(name = "image_192") val largeIconUrl: String
)

class Player(
        @Json(name = "_id") val id: String,
        val name: String,
        val profile: PlayerProfile
)

class RecentPlayer(@Json(name = "player") val playerId: String)

