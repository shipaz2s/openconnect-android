/*
 * Copyright (c) 2013, Kevin Cernekee
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * In addition, as a special exception, the copyright holders give
 * permission to link the code of portions of this program with the
 * OpenSSL library.
 */

package net.openconnect_vpn.android.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

public class DeviceStateReceiver extends BroadcastReceiver {

	public static final String TAG = "OpenConnect";

	public static final String PREF_CHANGED = "net.openconnect_vpn.android.PREF_CHANGED";

    private OpenVPNManagement mManagement;

    private SharedPreferences mPrefs;
    private boolean mPauseOnScreenOff;
    private boolean mNetchangeReconnect;

    private boolean mScreenOff;
    private boolean mKeepaliveActive;
    private boolean mPaused;

    private final Handler mHandler = new Handler();
    private final UnderlyingNetworkState<Network> mNetworkState =
            new UnderlyingNetworkState<Network>();
    private ConnectivityManager mConnectivityManager;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private boolean mNetworkMonitoring;
    private boolean mReconnectPending;

    private static final long RECONNECT_DEBOUNCE_MS = 500;
    private final Runnable mReconnectRunnable = new Runnable() {
        @Override
        public void run() {
            mReconnectPending = false;
            if (mNetworkMonitoring && !mPaused
                    && mNetworkState.getCurrentNetwork() != null) {
                Log.i(TAG, "reconnecting due to underlying network change");
                mManagement.reconnect();
            }
        }
    };

    public DeviceStateReceiver(OpenVPNManagement management, SharedPreferences prefs) {
        super();
        mManagement = management;
        mPrefs = prefs;
        readPrefs();
    }

    private void readPrefs() {
        mPauseOnScreenOff = mPrefs.getBoolean("screenoff", false);
        mNetchangeReconnect = mPrefs.getBoolean("netchangereconnect", true);
    }

    private void updatePauseState() {
    	boolean pause = false;
    	if (mPauseOnScreenOff && mScreenOff && !mKeepaliveActive) {
    		pause = true;
    	}
        if (mNetworkState.getCurrentNetwork() == null) {
    		pause = true;
    	}
    	if (pause && !mPaused) {
            Log.i(TAG, "pausing: mScreenOff=" + mScreenOff
                    + " underlyingNetwork=" + mNetworkState.getCurrentNetwork());
    		mManagement.pause();
    	} else if (!pause && mPaused) {
            Log.i(TAG, "resuming: mScreenOff=" + mScreenOff
                    + " underlyingNetwork=" + mNetworkState.getCurrentNetwork());
    		mManagement.resume();
    	}
    	mPaused = pause;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
    	String s = intent.getAction();

    	if (PREF_CHANGED.equals(s)) {
    		mManagement.prefChanged();
    		readPrefs();
        } else if (Intent.ACTION_SCREEN_OFF.equals(s)) {
        	mScreenOff = true;
        } else if (Intent.ACTION_SCREEN_ON.equals(s)) {
        	mScreenOff = false;
        }
        updatePauseState();
    }

    public void startNetworkMonitoring(Context context) {
        mConnectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        Set<Network> usableNetworks = new HashSet<Network>();
        for (Network network : mConnectivityManager.getAllNetworks()) {
            if (isUsableUnderlyingNetwork(
                    mConnectivityManager.getNetworkCapabilities(network))) {
                usableNetworks.add(network);
            }
        }
        Network activeNetwork = mConnectivityManager.getActiveNetwork();
        if (!usableNetworks.contains(activeNetwork)) {
            activeNetwork = null;
        }
        mNetworkState.initialize(usableNetworks, activeNetwork);
        mNetworkMonitoring = true;
        Log.i(TAG, "initial underlying network: " + mNetworkState.getCurrentNetwork());
        updatePauseState();

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                .build();
        mNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onCapabilitiesChanged(final Network network,
                    NetworkCapabilities capabilities) {
                final boolean usable = isUsableUnderlyingNetwork(capabilities);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mNetworkMonitoring) {
                            handleNetworkUsability(network, usable);
                        }
                    }
                });
            }

            @Override
            public void onLost(final Network network) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mNetworkMonitoring) {
                            handleNetworkUsability(network, false);
                        }
                    }
                });
            }

            @Override
            public void onLosing(final Network network, int maxMsToLive) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mNetworkMonitoring) {
                            handleNetworkAction(network, mNetworkState.onLosing(network));
                        }
                    }
                });
            }
        };
        mConnectivityManager.registerNetworkCallback(request, mNetworkCallback);
    }

    public void stopNetworkMonitoring() {
        mNetworkMonitoring = false;
        mHandler.removeCallbacksAndMessages(null);
        mReconnectPending = false;
        if (mConnectivityManager != null && mNetworkCallback != null) {
            try {
                mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "network callback was not registered", e);
            }
        }
        mNetworkCallback = null;
        mConnectivityManager = null;
    }

    private boolean isUsableUnderlyingNetwork(NetworkCapabilities capabilities) {
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN);
    }

    private void handleNetworkUsability(Network network, boolean usable) {
        UnderlyingNetworkState.Action action = mNetworkState.setUsable(network, usable);
        handleNetworkAction(network, action);
    }

    private void handleNetworkAction(Network network,
            UnderlyingNetworkState.Action action) {
        if (action == UnderlyingNetworkState.Action.NONE) {
            return;
        }

        Log.i(TAG, "underlying network " + network + " action=" + action);
        if (action == UnderlyingNetworkState.Action.PAUSE
                || action == UnderlyingNetworkState.Action.RESUME) {
            mHandler.removeCallbacks(mReconnectRunnable);
            mReconnectPending = false;
            updatePauseState();
        } else if (action == UnderlyingNetworkState.Action.RECONNECT
                && mNetchangeReconnect && !mPaused) {
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (mReconnectPending) {
            return;
        }
        mReconnectPending = true;
        mHandler.postDelayed(mReconnectRunnable, RECONNECT_DEBOUNCE_MS);
    }

    public void setKeepalive(boolean active) {
    	mKeepaliveActive = active;
    	updatePauseState();
    }
}
