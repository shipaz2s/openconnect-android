/*
 * Copyright (c) 2026 OpenConnect for Android contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package net.openconnect_vpn.android.core;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Tracks usable non-VPN networks without depending on Android framework types.
 *
 * Android can keep more than one physical network available at once. The
 * current network is therefore identified by its Network identity, not merely
 * by its transport (Wi-Fi/mobile).
 */
final class UnderlyingNetworkState<T> {
    enum Action {
        NONE,
        PAUSE,
        RESUME,
        RECONNECT
    }

    private final Set<T> mUsableNetworks = new LinkedHashSet<T>();
    private T mCurrentNetwork;
    private boolean mStarted;
    private boolean mPaused;

    void initialize(Set<T> usableNetworks, T currentNetwork) {
        mUsableNetworks.clear();
        mUsableNetworks.addAll(usableNetworks);
        if (currentNetwork != null && mUsableNetworks.contains(currentNetwork)) {
            mCurrentNetwork = currentNetwork;
        } else {
            mCurrentNetwork = firstUsableNetwork();
        }
        mStarted = true;
        mPaused = mCurrentNetwork == null;
    }

    Action setUsable(T network, boolean usable) {
        if (!mStarted) {
            throw new IllegalStateException("Network state is not initialized");
        }

        if (usable) {
            if (!mUsableNetworks.add(network)) {
                return Action.NONE;
            }
            if (mCurrentNetwork == null) {
                mCurrentNetwork = network;
                if (mPaused) {
                    mPaused = false;
                    return Action.RESUME;
                }
                return Action.NONE;
            }
            return Action.NONE;
        }

        if (!mUsableNetworks.remove(network) || !network.equals(mCurrentNetwork)) {
            return Action.NONE;
        }

        mCurrentNetwork = lastUsableNetwork();
        if (mCurrentNetwork == null) {
            mPaused = true;
            return Action.PAUSE;
        }
        return Action.RECONNECT;
    }

    Action onLosing(T network) {
        if (!network.equals(mCurrentNetwork)) {
            return Action.NONE;
        }
        T replacement = lastUsableNetworkExcept(network);
        if (replacement == null) {
            return Action.NONE;
        }
        mCurrentNetwork = replacement;
        return Action.RECONNECT;
    }

    T getCurrentNetwork() {
        return mCurrentNetwork;
    }

    private T firstUsableNetwork() {
        for (T network : mUsableNetworks) {
            return network;
        }
        return null;
    }

    private T lastUsableNetwork() {
        T last = null;
        for (T network : mUsableNetworks) {
            last = network;
        }
        return last;
    }

    private T lastUsableNetworkExcept(T excluded) {
        T last = null;
        for (T network : mUsableNetworks) {
            if (!network.equals(excluded)) {
                last = network;
            }
        }
        return last;
    }
}
