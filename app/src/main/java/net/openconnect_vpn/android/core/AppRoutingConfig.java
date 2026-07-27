/*
 * Copyright (c) 2026
 * SPDX-License-Identifier: GPL-2.0
 */

package net.openconnect_vpn.android.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Validates a per-application VPN allowlist without depending on Android APIs.
 */
public final class AppRoutingConfig {
    public interface PackageAvailability {
        boolean isInstalled(String packageName);
    }

    public static final class Result {
        private final List<String> installedPackages;
        private final List<String> missingPackages;

        private Result(List<String> installedPackages, List<String> missingPackages) {
            this.installedPackages = installedPackages;
            this.missingPackages = missingPackages;
        }

        public List<String> getInstalledPackages() {
            return installedPackages;
        }

        public List<String> getMissingPackages() {
            return missingPackages;
        }
    }

    private AppRoutingConfig() {
    }

    public static Result validate(Set<String> configuredPackages,
            PackageAvailability availability) {
        if (configuredPackages == null || configuredPackages.isEmpty()) {
            throw new IllegalArgumentException("Application allowlist is empty");
        }

        TreeSet<String> normalized = new TreeSet<String>();
        for (String packageName : configuredPackages) {
            if (packageName != null && !packageName.trim().isEmpty()) {
                normalized.add(packageName.trim());
            }
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Application allowlist is empty");
        }

        ArrayList<String> installed = new ArrayList<String>();
        ArrayList<String> missing = new ArrayList<String>();
        for (String packageName : normalized) {
            if (availability.isInstalled(packageName)) {
                installed.add(packageName);
            } else {
                missing.add(packageName);
            }
        }

        if (installed.isEmpty()) {
            throw new IllegalArgumentException(
                    "Application allowlist contains no installed packages");
        }
        return new Result(
                Collections.unmodifiableList(installed),
                Collections.unmodifiableList(missing));
    }
}
