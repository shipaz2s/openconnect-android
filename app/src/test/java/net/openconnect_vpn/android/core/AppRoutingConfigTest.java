package net.openconnect_vpn.android.core;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Test;

public class AppRoutingConfigTest {
    @Test(expected = IllegalArgumentException.class)
    public void emptyAllowlistIsRejected() {
        AppRoutingConfig.validate(Collections.<String>emptySet(), installedPackages());
    }

    @Test
    public void installedPackagesAreNormalizedAndSorted() {
        AppRoutingConfig.Result result = AppRoutingConfig.validate(
                new HashSet<String>(Arrays.asList("org.example.b", " org.example.a ")),
                installedPackages("org.example.a", "org.example.b"));

        assertEquals(Arrays.asList("org.example.a", "org.example.b"),
                result.getInstalledPackages());
        assertEquals(Collections.emptyList(), result.getMissingPackages());
    }

    @Test
    public void missingPackagesAreReported() {
        AppRoutingConfig.Result result = AppRoutingConfig.validate(
                new HashSet<String>(Arrays.asList("org.example.present", "org.example.gone")),
                installedPackages("org.example.present"));

        assertEquals(Collections.singletonList("org.example.present"),
                result.getInstalledPackages());
        assertEquals(Collections.singletonList("org.example.gone"),
                result.getMissingPackages());
    }

    @Test(expected = IllegalArgumentException.class)
    public void fullyMissingAllowlistIsRejected() {
        AppRoutingConfig.validate(
                Collections.singleton("org.example.gone"),
                installedPackages());
    }

    private static AppRoutingConfig.PackageAvailability installedPackages(
            final String... installed) {
        final HashSet<String> packages =
                new HashSet<String>(Arrays.asList(installed));
        return new AppRoutingConfig.PackageAvailability() {
            @Override
            public boolean isInstalled(String packageName) {
                return packages.contains(packageName);
            }
        };
    }
}
