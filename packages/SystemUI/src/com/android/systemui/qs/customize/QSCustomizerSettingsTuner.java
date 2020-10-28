/*
 * Copyright (C) 2020 The OmniROM Project
 * Copyright (C) 2020 ArrowOS
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
package com.android.systemui.qs.customize;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.android.systemui.R;

public class QSCustomizerSettingsTuner extends LinearLayout {

    private static Button showBrightnessSlider;

    private static final int brightnessSliderNull = R.drawable.ic_qs_brightness_slider_null;
    private static final int brightnessSliderSingle = R.drawable.ic_qs_brightness_slider_single;
    private static final int brightnessSliderDouble = R.drawable.ic_qs_brightness_slider_double;

    public QSCustomizerSettingsTuner(Context context, AttributeSet attrs) {
        super(new ContextThemeWrapper(context, R.style.edit_theme), attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        showBrightnessSlider = findViewById(R.id.qs_customize_settings_brightness_slider);
        int currentBrightnessSliderValue = Settings.Secure.getIntForUser(
                mContext.getContentResolver(), Settings.Secure.QS_SHOW_BRIGHTNESS_SLIDER, 0,
                UserHandle.USER_CURRENT);

        setBrightnessSliderDrawable(currentBrightnessSliderValue);

        showBrightnessSlider.setOnClickListener(new View.OnClickListener() {
            int showBrightnessSliderValue = Settings.Secure.getIntForUser(
                mContext.getContentResolver(), Settings.Secure.QS_SHOW_BRIGHTNESS_SLIDER, 0,
                UserHandle.USER_CURRENT);

            public void onClick(View v) {
                if (showBrightnessSliderValue >= 2) showBrightnessSliderValue = 0;
                else showBrightnessSliderValue += 1;

                setBrightnessSliderDrawable(showBrightnessSliderValue);

                Settings.Secure.putIntForUser(mContext.getContentResolver(),
                    Settings.Secure.QS_SHOW_BRIGHTNESS_SLIDER, showBrightnessSliderValue,
                    UserHandle.USER_CURRENT);
            }
        });
    }

    private void setBrightnessSliderDrawable(int currentBrightnessSliderValue) {
        if (currentBrightnessSliderValue == 0) {
            showBrightnessSlider.setCompoundDrawablesWithIntrinsicBounds(0, brightnessSliderNull, 0, 0);
            showBrightnessSlider.setText(R.string.customize_brightness_slider_hide);
        } else if (currentBrightnessSliderValue == 1) {
            showBrightnessSlider.setCompoundDrawablesWithIntrinsicBounds(0, brightnessSliderSingle, 0, 0);
            showBrightnessSlider.setText(R.string.customize_brightness_slider_expanded);
        } else if (currentBrightnessSliderValue == 2) {
            showBrightnessSlider.setCompoundDrawablesWithIntrinsicBounds(0, brightnessSliderDouble, 0, 0);
            showBrightnessSlider.setText(R.string.customize_brightness_slider_quick);
        }
    }
}
