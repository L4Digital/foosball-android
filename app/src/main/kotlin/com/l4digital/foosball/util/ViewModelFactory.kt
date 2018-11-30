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

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.support.v4.app.Fragment

/**
 * This abstract class is used by ViewModels to create
 * their own Factory class for dependency injection.
 */
abstract class ViewModelFactory<VM : ViewModel> : ViewModelProvider.Factory {

    protected abstract val modelClass: Class<VM>

    protected val illegalArgumentError = "Unknown ViewModel class"

    fun get(fragment: Fragment): VM = ViewModelProviders.of(fragment, this).get(modelClass)
}
