/*
 * Copyright (c) 2018, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *  * Neither the name of The Linux Foundation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;

import java.lang.Exception;

import org.codeaurora.qti.qtiNetworkLib.Client;
import org.codeaurora.qti.qtiNetworkLib.DcParam;
import org.codeaurora.qti.qtiNetworkLib.INetworkCallback;
import org.codeaurora.qti.qtiNetworkLib.INetworkInterface;
import org.codeaurora.qti.qtiNetworkLib.ServiceUtil;
import org.codeaurora.qti.qtiNetworkLib.SignalStrength;
import org.codeaurora.qti.qtiNetworkLib.Status;
import org.codeaurora.qti.qtiNetworkLib.Token;

import com.android.systemui.R;

public class FiveGServiceClient {
    private static final String TAG = "FiveGServiceClient";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    private static final int MESSAGE_REBIND = 1024;
    private static final int MESSAGE_REINIT = MESSAGE_REBIND+1;
    private static final int MAX_RETRY = 4;
    private static final int DELAY_MILLISECOND = 3000;
    private static final int DELAY_INCREMENT = 2000;
    private final int NUM_LEVELS;
    private final int mRsrpThresholds[];
    private final int mSnrThresholds[];
    private final SparseArray<IFiveGStateListener> mStatesListeners = new SparseArray<>();
    private final SparseArray<FiveGServiceState> mCurrentServiceStates = new SparseArray<>();
    private final SparseArray<FiveGServiceState> mLastServiceStates = new SparseArray<>();

    private Context mContext;
    private boolean mServiceConnected;
    private INetworkInterface mNetworkService;
    private String mPackageName;
    private Client mClient;
    private int mBindRetryTimes = 0;
    private int mInitRetryTimes = 0;

    public static class FiveGServiceState{
        private int mEndc;
        private int mDcnr;
        private  boolean mEnabled;
        private boolean mDataConnected;
        private int mLevel;

        public FiveGServiceState(){
            mEndc = DcParam.ENDC_UNAVAILABLE;
            mDcnr = DcParam.DCNR_RESTRICTED;
            mEnabled = false;
            mDataConnected = false;
            mLevel = 0;
        }

        public boolean equals(FiveGServiceState state) {
            return  mDataConnected == state.mDataConnected
                    && mLevel == state.mLevel
                    &&isServiceAvailable() == state.isServiceAvailable();
        }

        public boolean isServiceAvailable() {
            boolean available = false;
            if ( mEndc == DcParam.ENDC_AVAILABLE && mDcnr == DcParam.DCNR_UNRESTRICTED ) {
                available = true;
            }
            return available;
        }

        public boolean isDataConnected() {
            return this.mDataConnected;
        }

        public int getLevel() {
            return this.mLevel;
        }

        public void copyFrom(FiveGServiceState state) {
            this.mEndc = state.mEndc;
            this.mDcnr = state.mDcnr;
            this.mEnabled = state.mEnabled;
            this.mDataConnected = state.mDataConnected;
            this.mLevel = state.mLevel;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("mEndc=").append(mEndc).append(", ").
                    append("mDcnr=").append(mDcnr).append(", ").
                    append("mEnabled=").append(mEnabled).append(", ").
                    append("mDataConnected=").append(mDataConnected).append(", ").
                    append("mLevel=" + mLevel);

            return builder.toString();
        }
    }

    public FiveGServiceClient(Context context) {
        mContext = context;
        mPackageName = mContext.getPackageName();
        NUM_LEVELS = getNumLevels(context);

        mRsrpThresholds =
                mContext.getResources().getIntArray(R.array.config_5g_signal_rsrp_thresholds);
        mSnrThresholds =
                mContext.getResources().getIntArray(R.array.config_5g_signal_snr_thresholds);
    }

    public void registerListener(int phoneId, IFiveGStateListener listener) {
        Log.d(TAG, "registerListener phoneId=" + phoneId);

        mStatesListeners.put(phoneId, listener);
        if ( !isServiceConnected() ) {
            binderService();
        }else{
            initFiveGServiceState(phoneId);
        }
    }

    public void unregisterListener(int phoneId) {
        Log.d(TAG, "unregisterListener phoneId=" + phoneId);
        mStatesListeners.remove(phoneId);
        mCurrentServiceStates.remove(phoneId);
        mLastServiceStates.remove(phoneId);
    }

    private void binderService() {
        boolean success = ServiceUtil.bindService(mContext, mServiceConnection);
        Log.d(TAG, " bind service " + success);
        if ( !success && mBindRetryTimes < MAX_RETRY && !mHandler.hasMessages(MESSAGE_REBIND)) {
            mHandler.sendEmptyMessageDelayed(MESSAGE_REBIND,
                    DELAY_MILLISECOND + mBindRetryTimes*DELAY_INCREMENT);
            mBindRetryTimes+=1;
        }
    }

    public static int getNumLevels(Context context) {
        return context.getResources().getInteger(R.integer.config_5g_num_signal_strength_bins);
    }

    public boolean isServiceConnected() {
        return mServiceConnected;
    }

    private FiveGServiceState getCurrentServiceState(int phoneId) {
        return getServiceState(phoneId, mCurrentServiceStates);
    }

    private FiveGServiceState getLastServiceState(int phoneId) {
        return getServiceState(phoneId, mLastServiceStates);
    }

    private static FiveGServiceState getServiceState(int key,
                                                     SparseArray<FiveGServiceState> array) {
        FiveGServiceState state = array.get(key);
        if ( state == null ) {
            state = new FiveGServiceState();
            array.put(key, state);
        }
        return state;
    }

    private int getSnrLevel(int snr) {
        return getLevel(snr, mSnrThresholds);
    }

    private int getRsrpLevel(int rsrp) {
        return getLevel(rsrp, mRsrpThresholds);
    }

    private static int getLevel(int value, int[]thresholds) {
        int level = 0;
        if ( thresholds[thresholds.length-1] < value || value < thresholds[0] ) {
            level = 0;
        } else{
            level = 1;
            for( int i=0; i < thresholds.length-1; ++i ) {
                if (thresholds[i] < value && value <= thresholds[i+1]) {
                    level = i+1;
                    break;
                }
            }
        }
        if ( DEBUG ) {
            Log.d(TAG, "value=" + value + " level=" + level);
        }
        return level;
    }

    private void notifyListenersIfNecessary(int phoneId) {
        FiveGServiceState currentState = getCurrentServiceState(phoneId);
        FiveGServiceState lastState = getLastServiceState(phoneId);

        if ( !currentState.equals(lastState) ) {

            if ( DEBUG ) {
                Log.d(TAG, "Change in state from " + lastState + " \n"+
                        "\tto " + currentState);

            }

            lastState.copyFrom(currentState);
            IFiveGStateListener listener = mStatesListeners.get(phoneId);
            if (listener != null) {
                listener.onStateChanged(currentState);
            }

        }
    }

    private void initFiveGServiceState() {
        Log.d(TAG, "initFiveGServiceState size=" + mStatesListeners.size());
        for( int i=0; i < mStatesListeners.size(); ++i ) {
            int phoneId = mStatesListeners.keyAt(i);
            initFiveGServiceState(phoneId);
        }
    }

    private void initFiveGServiceState(int phoneId) {
        Log.d(TAG, "mNetworkService=" + mNetworkService + " mClient=" + mClient);
        if ( mNetworkService != null && mClient != null) {
            Log.d(TAG, "query 5G service state for phoneId " + phoneId);
            try {
                Token token = mNetworkService.queryNrDcParam(phoneId, mClient);
                Log.d(TAG, "queryNrDcParam result:" + token);
                mNetworkService.queryNrBearerAllocation(phoneId, mClient);
                Log.d(TAG, "queryNrBearerAllocation result:" + token);
                mNetworkService.queryNrSignalStrength(phoneId, mClient);
                Log.d(TAG, "queryNrSignalStrength result:" + token);
            }catch (Exception e) {
                Log.d(TAG, "initFiveGServiceState: Exception = " + e);
                if ( mInitRetryTimes < MAX_RETRY && !mHandler.hasMessages(MESSAGE_REINIT) ) {
                    mHandler.sendEmptyMessageDelayed(MESSAGE_REINIT,
                            DELAY_MILLISECOND + mInitRetryTimes*DELAY_INCREMENT);
                    mInitRetryTimes +=1;
                }
            }
        }
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch ( msg.what ) {
                case MESSAGE_REBIND:
                    binderService();
                    break;

                case MESSAGE_REINIT:
                    initFiveGServiceState();
                    break;
            }

        }
    };

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected:" + service);

            try {
                mNetworkService = INetworkInterface.Stub.asInterface(service);
                mClient = mNetworkService.registerCallback(mPackageName, mCallback);
                mServiceConnected = true;
                initFiveGServiceState();
                Log.d(TAG, "Client = " + mClient);
            } catch (Exception e) {
                Log.d(TAG, "onServiceConnected: Exception = " + e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected:" + name);
            cleanup();
        }

        @Override
        public void onBindingDied(ComponentName name) {
            Log.d(TAG, "onBindingDied:" + name);
            cleanup();
            if ( mBindRetryTimes < MAX_RETRY ) {
                Log.d(TAG, "try to re-bind");
                mHandler.sendEmptyMessageDelayed(MESSAGE_REBIND,
                        DELAY_MILLISECOND+mBindRetryTimes*DELAY_INCREMENT);
            }
        }

        private void cleanup() {
            Log.d(TAG, "cleanup");
            mServiceConnected = false;
            mNetworkService = null;
            mClient = null;
        }
    };


    private INetworkCallback mCallback = new INetworkCallback.Stub() {
        @Override
        public void on5gStatus(int slotId, Token token, Status status, boolean enableStatus) throws
                RemoteException {

            if ( DEBUG ) {
                Log.d(TAG, "on5gStatus: slotId= " + slotId + " token=" + token + " status=" +
                        status + " enableStatus=" + enableStatus);
            }

            if (status.get() == Status.SUCCESS) {
                FiveGServiceState state = getCurrentServiceState(slotId);
                state.mEnabled = enableStatus;

                notifyListenersIfNecessary(slotId);
            }
        }

        @Override
        public void onNrDcParam(int slotId, Token token, Status status, DcParam dcParam) throws
                RemoteException {

            if ( DEBUG ) {
                Log.d(TAG, "onNrDcParam: slotId=" + slotId + " token=" + token + " status=" +
                        status + " dcParam=" + dcParam);
            }

            if (status.get() == Status.SUCCESS) {
                FiveGServiceState state = getCurrentServiceState(slotId);
                state.mDcnr = dcParam.getDcnr();
                state.mEndc = dcParam.getEndc();

                notifyListenersIfNecessary(slotId);
            }
        }

        @Override
        public void onNrBearerAllocation(int slotId, Token token, Status status, boolean
                allocated) throws RemoteException {

            if ( DEBUG ) {
                Log.d(TAG, "onNrBearerAllocationChange: slotId=" + slotId + " token=" + token
                        + "status=" + status + " allocated=" + allocated);
            }

            if (status.get() == Status.SUCCESS) {
                FiveGServiceState state = getCurrentServiceState(slotId);
                state.mDataConnected = allocated;

                notifyListenersIfNecessary(slotId);
            }
        }

        @Override
        public void onSignalStrength(int slotId, Token token, Status status,
                                     org.codeaurora.qti.qtiNetworkLib.SignalStrength
                                             signalStrength) throws RemoteException {
            if ( DEBUG ) {
                Log.d(TAG, "onSignalStrength: slotId=" + slotId + " token=" + token
                        + " status=" + status + " signalStrength=" + signalStrength);
            }

            if (status.get() == Status.SUCCESS && signalStrength != null) {
                FiveGServiceState state = getCurrentServiceState(slotId);
                state.mLevel = getRsrpLevel(signalStrength.getRsrp());

                notifyListenersIfNecessary(slotId);
            }
        }

    };

    public interface IFiveGStateListener {
        public void onStateChanged(FiveGServiceState state);
    }
}
