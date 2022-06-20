package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.settingslib.Utils;
import com.android.systemui.R;

import java.text.DecimalFormat;

/*
 *
 * Seeing how an Integer object in java requires at least 16 Bytes, it seemed awfully wasteful
 * to only use it for a single boolean. 32-bits is plenty of room for what we need it to do.
 *
 */
public class NetworkTraffic extends LinearLayout {

    private static final int INTERVAL = 1500; //ms
    private static final int KB = 1024;
    private static final int MB = KB * KB;
    private static final int GB = MB * KB;
    private static final String symbol = "/S";

    private final int mWidth;

    protected boolean mIsEnabled;
    private boolean mAttached;
    private long totalRxBytes;
    private long totalTxBytes;
    private long lastUpdateTime;
    private int mAutoHideThreshold;
    protected int mTintColor;

    private TextView mSpeedView;
    private TextView mUnitView;

    private boolean mScreenOn = true;
    protected boolean mVisible = true;
    private ConnectivityManager mConnectivityManager;

    private Handler mTrafficHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            long timeDelta = SystemClock.elapsedRealtime() - lastUpdateTime;

            if (timeDelta < INTERVAL * .95) {
                if (msg.what != 1) {
                    // we just updated the view, nothing further to do
                    return;
                }
                if (timeDelta < 1) {
                    // Can't div by 0 so make sure the value displayed is minimal
                    timeDelta = Long.MAX_VALUE;
                }
            }
            lastUpdateTime = SystemClock.elapsedRealtime();

            // Calculate the data rate from the change in total bytes and time
            long newTotalRxBytes = TrafficStats.getTotalRxBytes();
            long newTotalTxBytes = TrafficStats.getTotalTxBytes();
            long rxData = newTotalRxBytes - totalRxBytes;
            long txData = newTotalTxBytes - totalTxBytes;

            if (shouldHide(rxData, txData, timeDelta)) {
                setVisibility(View.INVISIBLE);
                mVisible = false;
            } else if (shouldShowUpload(rxData, txData, timeDelta)) {
                // Show information for uplink if it's called for
                showNetworkTraffic(timeDelta, txData);
            } else {
                // Add information for downlink if it's called for
                showNetworkTraffic(timeDelta, rxData);
            }

            // Post delayed message to refresh in ~1000ms
            totalRxBytes = newTotalRxBytes;
            totalTxBytes = newTotalTxBytes;
            clearHandlerCallbacks();
            mTrafficHandler.postDelayed(mRunnable, INTERVAL);
        }

        private void showNetworkTraffic(long timeDelta, long data) {
            CharSequence[] output = formatOutput(timeDelta, data);
            // Update view if there's anything new to show
            mSpeedView.setText(output[0]);
            mUnitView.setText(output[1]);
            makeVisible();
        }

        private CharSequence[] formatOutput(long timeDelta, long data) {
            return formatDecimal((long) (data / (timeDelta / 1000f)));
        }

        private CharSequence[] formatDecimal(long speed) {
            DecimalFormat decimalFormat;
            String unit;
            String formatSpeed;

            if (speed >= 1000 * MB) {
                unit = "GB";
                decimalFormat = new DecimalFormat("0.00");
                formatSpeed = decimalFormat.format(speed / (float) GB);
            } else if (speed >= 100 * MB) {
                decimalFormat = new DecimalFormat("000");
                unit = "MB";
                formatSpeed = decimalFormat.format(speed / (float) MB);
            } else if (speed >= 10 * MB) {
                decimalFormat = new DecimalFormat("00.0");
                unit = "MB";
                formatSpeed = decimalFormat.format(speed / (float) MB);
            } else if (speed >= 1000 * KB) {
                decimalFormat = new DecimalFormat("0.00");
                unit = "MB";
                formatSpeed = decimalFormat.format(speed / (float) MB);
            } else if (speed >= 100 * KB) {
                decimalFormat = new DecimalFormat("000");
                unit = "KB";
                formatSpeed = decimalFormat.format(speed / (float) KB);
            } else if (speed >= 10 * KB) {
                decimalFormat = new DecimalFormat("00.0");
                unit = "KB";
                formatSpeed = decimalFormat.format(speed / (float) KB);
            } else {
                decimalFormat = new DecimalFormat("0.00");
                unit = "KB";
                formatSpeed = decimalFormat.format(speed / (float) KB);
            }
            return new CharSequence[]{formatSpeed, unit + symbol};
        }

        private boolean shouldHide(long rxData, long txData, long timeDelta) {
            long speedRxKB = (long) (rxData / (timeDelta / 1000f)) / KB;
            long speedTxKB = (long) (txData / (timeDelta / 1000f)) / KB;
            return !getConnectAvailable() ||
                    (speedRxKB < mAutoHideThreshold && speedTxKB < mAutoHideThreshold);
        }

        private boolean shouldShowUpload(long rxData, long txData, long timeDelta) {
            long speedRxKB = (long) (rxData / (timeDelta / 1000f)) / KB;
            long speedTxKB = (long) (txData / (timeDelta / 1000f)) / KB;

            return speedTxKB > speedRxKB;
        }
    };

    protected void makeVisible() {
        setVisibility(View.VISIBLE);
        mVisible = true;
    }

    /*
     *  @hide
     */
    public NetworkTraffic(Context context) {
        this(context, null);
    }

    /*
     *  @hide
     */
    public NetworkTraffic(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /*
     *  @hide
     */
    public NetworkTraffic(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setGravity(Gravity.CENTER);
        final Resources resources = getResources();
        mTintColor = Utils.getColorAttrDefaultColor(context, android.R.attr.textColorPrimary);
        mWidth = resources.getDimensionPixelSize(R.dimen.network_traffic_width);
        mSpeedView = new TextView(context);
        mUnitView = new TextView(context);
        setMode();
        Handler mHandler = new Handler(Looper.getMainLooper());
        mConnectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        SettingsObserver settingsObserver = new SettingsObserver(mHandler);
        settingsObserver.observe();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            mContext.registerReceiver(mIntentReceiver, filter, null, getHandler());
        }
        update();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            mContext.unregisterReceiver(mIntentReceiver);
            mAttached = false;
        }
    }

    private Runnable mRunnable = () -> mTrafficHandler.sendEmptyMessage(0);

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.NETWORK_TRAFFIC_STATE), false,
                    this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD), false,
                    this, UserHandle.USER_ALL);
        }

        /*
         *  @hide
         */
        @Override
        public void onChange(boolean selfChange) {
            setMode();
        }
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            mScreenOn = action.equals(Intent.ACTION_SCREEN_ON);
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION) && mScreenOn || mScreenOn) {
                update();
            } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                mScreenOn = false;
                clearHandlerCallbacks();
            }
        }
    };

    private boolean getConnectAvailable() {
        Network network =
                mConnectivityManager != null ? mConnectivityManager.getActiveNetwork() : null;
        return network != null;
    }

    protected void update() {
        if (mIsEnabled) {
            if (mAttached) {
                totalRxBytes = TrafficStats.getTotalRxBytes();
                totalTxBytes = TrafficStats.getTotalTxBytes();
                mTrafficHandler.sendEmptyMessage(1);
            }
            if (mAutoHideThreshold == 0)
                makeVisible();
            return;
        }
        clearHandlerCallbacks();
        setVisibility(View.GONE);
        mVisible = false;
    }

    protected void setMode() {
        ContentResolver resolver = mContext.getContentResolver();
        mIsEnabled = Settings.System.getIntForUser(resolver,
                Settings.System.NETWORK_TRAFFIC_STATE, 0,
                UserHandle.USER_CURRENT) == 1;
        mAutoHideThreshold = Settings.System.getIntForUser(resolver,
                Settings.System.NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD, 1,
                UserHandle.USER_CURRENT);
        setSpacingAndFonts();
        setTextColor(mTintColor);
        setOrientation(LinearLayout.VERTICAL);
        post(() -> getLayoutParams().width = mWidth);
        if (mIsEnabled) {
            if (getChildCount() == 0) {
                ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                addView(mSpeedView, layoutParams);
                addView(mUnitView, layoutParams);
            }
        }
        update();
    }

    private void clearHandlerCallbacks() {
        mTrafficHandler.removeCallbacks(mRunnable);
        mTrafficHandler.removeMessages(0);
        mTrafficHandler.removeMessages(1);
    }

    protected void setTextColor(int color) {
        mSpeedView.setTextColor(color);
        mUnitView.setTextColor(color);
    }

    protected void setSpacingAndFonts() {
        mSpeedView.setTextAppearance(R.style.TextAppearance_QS_Status);
        mUnitView.setTextAppearance(R.style.TextAppearance_QS_Status);
        mSpeedView.setTextSize(8f);
        mUnitView.setTextSize(6f);
    }

    public void setTintColor(int color) {
        mTintColor = color;
        setTextColor(mTintColor);
    }
}
