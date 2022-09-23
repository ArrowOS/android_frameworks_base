/*
 * Copyright (C) 2022 The LineageOS Project
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

package com.android.systemui.biometrics

import android.content.Context
import android.os.FileUtils
import android.view.Surface

import java.io.IOException

class NtUdfpsHbmProvider constructor(
    private val context: Context
): UdfpsHbmProvider {

    override fun enableHbm(hbmType: Int, surface: Surface?, onHbmEnabled: Runnable?) {
        try {
            FileUtils.stringToFile(NT_HBM_MODE, "1")
        } catch (e: IOException) {
            // Do nothing...
        }
        onHbmEnabled?.run()
    }

    override fun disableHbm(onHbmDisabled: Runnable?) {
        try {
            FileUtils.stringToFile(NT_HBM_MODE, "0")
        } catch (e: IOException) {
            // Do nothing...
        }
        onHbmDisabled?.run()
    }

    companion object {
        private const val NT_HBM_MODE = "/sys/class/drm/sde-conn-1-DSI-1/hbm_mode"
    }
}
