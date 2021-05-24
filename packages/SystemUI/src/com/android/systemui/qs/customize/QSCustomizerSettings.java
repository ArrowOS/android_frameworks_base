/*
 * Copyright (C) 2020 The OmniROM Project
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
import android.content.res.Resources;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.SeekBar;
import android.widget.Switch;

import com.android.systemui.R;

public class QSCustomizerSettings extends LinearLayout {
    private static final String TAG = "QSCustomizer::QSCustomizerSettings";
    private static final boolean DEBUG = false;
    private static final String PREFS = "qscustomizer_prefs";
    private static final String COLUMNS_TOOLTIP_SHOWN = "columns_tooltip_shown";

    private static Button showBrightnessSlider;

    private static final int brightnessSliderNull = R.drawable.ic_qs_brightness_slider_null;
    private static final int brightnessSliderSingle = R.drawable.ic_qs_brightness_slider_single;
    private static final int brightnessSliderDouble = R.drawable.ic_qs_brightness_slider_double;

    public QSCustomizerSettings(Context context, AttributeSet attrs) {
        super(new ContextThemeWrapper(context, R.style.edit_theme), attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        Switch showLabels = findViewById(R.id.qs_customize_settings_show_labels);
        boolean showLabelsValue = Settings.System.getIntForUser(
                mContext.getContentResolver(), Settings.System.QS_TILE_TITLE_VISIBILITY, 1,
                UserHandle.USER_CURRENT) == 1;
        showLabels.setChecked(showLabelsValue);
        showLabels.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Settings.System.putIntForUser(mContext.getContentResolver(),
                    Settings.System.QS_TILE_TITLE_VISIBILITY, isChecked ? 1 : 0,
                    UserHandle.USER_CURRENT);
        });

        Switch showBrightnessIcon = findViewById(R.id.qs_customize_settings_brightness_icon);
        boolean showBrightnessIconValue = Settings.Secure.getIntForUser(
                mContext.getContentResolver(), Settings.Secure.QS_SHOW_AUTO_BRIGHTNESS, 1,
                UserHandle.USER_CURRENT) == 1;
        showBrightnessIcon.setChecked(showBrightnessIconValue);
        showBrightnessIcon.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Settings.Secure.putIntForUser(mContext.getContentResolver(),
                    Settings.Secure.QS_SHOW_AUTO_BRIGHTNESS, isChecked ? 1 : 0,
                    UserHandle.USER_CURRENT);
        });

        showBrightnessSlider = findViewById(R.id.qs_customize_settings_brightness_slider);
        int currentBrightnessSliderValue = Settings.Secure.getIntForUser(
                mContext.getContentResolver(), Settings.Secure.QS_SHOW_BRIGHTNESS_SLIDER, 1,
                UserHandle.USER_CURRENT);

        setBrightnessSliderDrawable(currentBrightnessSliderValue);

        showBrightnessSlider.setOnClickListener(new View.OnClickListener() {
            int showBrightnessSliderValue = Settings.Secure.getIntForUser(
                mContext.getContentResolver(), Settings.Secure.QS_SHOW_BRIGHTNESS_SLIDER, 1,
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

        Switch useLessRows = findViewById(R.id.qs_less_rows_switch);
        if (useLessRows != null) {
            useLessRows.setOnCheckedChangeListener((v, checked) -> {
                Settings.System.putInt(mContext.getContentResolver(), "qs_less_rows", !checked ? 1 : 0);
            });
            useLessRows.setChecked((Settings.System.getInt(mContext.getContentResolver(), "qs_less_rows", 0)) != 1);
        }

        int defaultMaxTiles = mContext.getResources().getInteger(R.integer.quick_qs_panel_max_columns);
        int quickColumns = Settings.System.getIntForUser(
                mContext.getContentResolver(), Settings.System.QS_QUICKBAR_COLUMNS,
                defaultMaxTiles, UserHandle.USER_CURRENT);
        final SeekBar quickColumnsSlider = findViewById(R.id.qs_customize_settings_quickbar);
        Switch quickFollow = findViewById(R.id.qs_customize_settings_quickbar_follow);
        quickFollow.setChecked(quickColumns == -1);
        quickFollow.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Settings.System.putIntForUser(mContext.getContentResolver(),
                    Settings.System.QS_QUICKBAR_COLUMNS, isChecked ? -1 : defaultMaxTiles,
                    UserHandle.USER_CURRENT);
            quickColumnsSlider.setEnabled(!isChecked);
        });
        quickColumnsSlider.setProgress(quickColumns != -1 ? quickColumns : defaultMaxTiles);
        quickColumnsSlider.setEnabled(quickColumns != -1);
        quickColumnsSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    Settings.System.putIntForUser(mContext.getContentResolver(),
                            Settings.System.QS_QUICKBAR_COLUMNS, progress,
                            UserHandle.USER_CURRENT);
                }
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        int resourceColumns = Math.max(1, mContext.getResources().getInteger(R.integer.quick_settings_num_columns));
        int columnsPort = Settings.System.getIntForUser(
                mContext.getContentResolver(), Settings.System.QS_LAYOUT_COLUMNS,
                resourceColumns, UserHandle.USER_CURRENT);
        SeekBar columnsSliderPort = findViewById(R.id.qs_customize_settings_columns_port);
        columnsSliderPort.setProgress(columnsPort);
        columnsSliderPort.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    Settings.System.putIntForUser(mContext.getContentResolver(),
                            Settings.System.QS_LAYOUT_COLUMNS, progress,
                            UserHandle.USER_CURRENT);
                }
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        int columnsLand = Settings.System.getIntForUser(
                mContext.getContentResolver(), Settings.System.QS_LAYOUT_COLUMNS_LANDSCAPE,
                resourceColumns, UserHandle.USER_CURRENT);
        SeekBar columnsSliderLand = findViewById(R.id.qs_customize_settings_columns_land);
        columnsSliderLand.setProgress(columnsLand);
        columnsSliderLand.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    Settings.System.putIntForUser(mContext.getContentResolver(),
                            Settings.System.QS_LAYOUT_COLUMNS_LANDSCAPE, progress,
                            UserHandle.USER_CURRENT);
                }
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        showInfoTooltip();
    }

    private void showInfoTooltip() {
        if (!mContext.getSharedPreferences(PREFS, 0).getBoolean(COLUMNS_TOOLTIP_SHOWN, false)) {
            final View info = findViewById(R.id.qs_customize_settings_info);
            info.setVisibility(View.VISIBLE);
            View dismiss = findViewById(R.id.qs_customize_settings_info_dismiss);
            dismiss.setOnClickListener(v -> {
                mContext.getSharedPreferences(PREFS, 0).edit().putBoolean(
                        COLUMNS_TOOLTIP_SHOWN, true).apply();
                info.setVisibility(View.GONE);
            });
        }
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
