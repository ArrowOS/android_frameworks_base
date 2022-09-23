/*
 * Copyright (C) 2022 Project Kaleidoscope
 *
 * SPDX-License-Identifier: Apache-2.0
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
