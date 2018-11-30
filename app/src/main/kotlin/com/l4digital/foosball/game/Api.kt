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

import com.l4digital.foosball.ApplicationScope
import com.l4digital.foosball.BuildConfig
import com.l4digital.foosball.game.model.*
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import toothpick.ProvidesSingletonInScope
import java.util.concurrent.TimeUnit
import javax.inject.Provider

interface FoosballApi {
    @GET("users/active")
    fun getActivePlayers(): Single<List<Player>>

    @GET("users/recent")
    fun getRecentPlayers(): Single<List<RecentPlayer>>

    @GET("slack/summon")
    fun summon(@Query("name") playerName: String): Completable

    @Headers("Content-Type: application/json; charset=utf-8")
    @POST("games")
    fun submitGame(@Body game: GameApiModel): Completable
}

@ApplicationScope @ProvidesSingletonInScope
class ApiProvider: Provider<FoosballApi> {
    override fun get(): FoosballApi {
        val okHttpClient = OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS).apply {
                    if(BuildConfig.DEBUG) {
                        addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                    }
                }
                .build()

        val retrofit = Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().add(KotlinJsonAdapterFactory()).build()))
                .client(okHttpClient)
                .build()

        return retrofit.create(FoosballApi::class.java)
    }
}