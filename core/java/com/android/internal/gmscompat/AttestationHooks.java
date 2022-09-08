/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.internal.gmscompat;

import android.app.Application;
import android.content.res.Resources;
import android.os.Build;
import android.os.SystemProperties;
import android.util.Log;

import com.android.internal.R;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** @hide */
public final class AttestationHooks {
    private static final String TAG = "GmsCompat/Attestation";

    private static final String PACKAGE_GMS = "com.google.android.gms";
    private static final String PACKAGE_GPHOTOS = "com.google.android.apps.photos";

    private static final String PROCESS_UNSTABLE = "com.google.android.gms.unstable";

    private static final String PRODUCT_GMS_SPOOFING_FINGERPRINT =
            SystemProperties.get("ro.build.gms_fingerprint");

    private static final Map<String, String> sP1Props = new HashMap<>();
    static {
        sP1Props.put("BRAND", "google");
        sP1Props.put("MANUFACTURER", "Google");
        sP1Props.put("DEVICE", "marlin");
        sP1Props.put("PRODUCT", "marlin");
        sP1Props.put("MODEL", "Pixel XL");
        sP1Props.put("FINGERPRINT", "google/marlin/marlin:10/QP1A.191005.007.A3/5972272:user/release-keys");
    }

    private static final String[] sFeaturesBlacklist = {
        "PIXEL_2017_EXPERIENCE",
        "PIXEL_2017_PRELOAD",
        "PIXEL_2018_PRELOAD",
        "PIXEL_2019_EXPERIENCE",
        "PIXEL_2019_MIDYEAR_EXPERIENCE",
        "PIXEL_2019_MIDYEAR_PRELOAD",
        "PIXEL_2019_PRELOAD",
        "PIXEL_2020_EXPERIENCE",
        "PIXEL_2020_MIDYEAR_EXPERIENCE",
        "PIXEL_2021_EXPERIENCE",
        "PIXEL_2021_MIDYEAR_EXPERIENCE"
    };

    private static volatile boolean sIsPhotos = false;

    private static final boolean sSpoofPhotos =
            Resources.getSystem().getBoolean(R.bool.config_spoofGooglePhotos);

    private AttestationHooks() { }

    private static void setBuildField(String key, String value) {
        try {
            // Unlock
            Field field = Build.class.getDeclaredField(key);
            field.setAccessible(true);

            // Edit
            field.set(null, value);

            // Lock
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to spoof Build." + key, e);
        }
    }

    private static void spoofBuildGms() {
        // Set fingerprint for SafetyNet CTS profile
        if (PRODUCT_GMS_SPOOFING_FINGERPRINT.length() > 0) {
            setBuildField("FINGERPRINT", PRODUCT_GMS_SPOOFING_FINGERPRINT);
        }

        // Alter model name to avoid hardware attestation enforcement
        setBuildField("MODEL", Build.MODEL + " ");
    }

    public static void initApplicationBeforeOnCreate(Application app) {
        if (PACKAGE_GMS.equals(app.getPackageName()) &&
                PROCESS_UNSTABLE.equals(Application.getProcessName())) {
            spoofBuildGms();
        } else if (sSpoofPhotos && PACKAGE_GPHOTOS.equals(app.getPackageName())) {
            sIsPhotos = true;
            sP1Props.forEach((k, v) -> setBuildField(k, v));
        }
    }

    public static boolean hasSystemFeature(String name, boolean def) {
        if (sIsPhotos && def &&
                Arrays.stream(sFeaturesBlacklist).anyMatch(name::contains)) {
            return false;
        }
        return def;
    }
}
