package net.openconnect_vpn.android.core;

import static net.openconnect_vpn.android.core.UnderlyingNetworkState.Action.NONE;
import static net.openconnect_vpn.android.core.UnderlyingNetworkState.Action.PAUSE;
import static net.openconnect_vpn.android.core.UnderlyingNetworkState.Action.RECONNECT;
import static net.openconnect_vpn.android.core.UnderlyingNetworkState.Action.RESUME;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

import org.junit.Test;

public class UnderlyingNetworkStateTest {
    @Test
    public void initialNetworkDoesNotReconnect() {
        UnderlyingNetworkState<String> state = new UnderlyingNetworkState<String>();

        state.initialize(networks("wifi"), "wifi");

        assertEquals("wifi", state.getCurrentNetwork());
        assertEquals(NONE, state.setUsable("wifi", true));
    }

    @Test
    public void standbyNetworkDoesNotReconnectUntilCurrentIsLosing() {
        UnderlyingNetworkState<String> state = initialized("mobile");

        assertEquals(NONE, state.setUsable("wifi", true));
        assertEquals("mobile", state.getCurrentNetwork());

        assertEquals(RECONNECT, state.onLosing("mobile"));
        assertEquals("wifi", state.getCurrentNetwork());
    }

    @Test
    public void losingNonCurrentNetworkIsIgnored() {
        UnderlyingNetworkState<String> state = new UnderlyingNetworkState<String>();
        state.initialize(networks("mobile", "wifi"), "wifi");

        assertEquals(NONE, state.onLosing("mobile"));
        assertEquals("wifi", state.getCurrentNetwork());
    }

    @Test
    public void lossSwitchesToMostRecentlyAvailableReplacement() {
        UnderlyingNetworkState<String> state = initialized("wifi-a");
        state.setUsable("mobile", true);
        state.setUsable("wifi-b", true);

        assertEquals(RECONNECT, state.setUsable("wifi-a", false));
        assertEquals("wifi-b", state.getCurrentNetwork());
    }

    @Test
    public void lossOfLastNetworkPauses() {
        UnderlyingNetworkState<String> state = initialized("wifi");

        assertEquals(PAUSE, state.setUsable("wifi", false));
        assertEquals(null, state.getCurrentNetwork());
    }

    @Test
    public void firstNetworkAfterOfflineResumes() {
        UnderlyingNetworkState<String> state = new UnderlyingNetworkState<String>();
        state.initialize(Collections.<String>emptySet(), null);

        assertEquals(RESUME, state.setUsable("mobile", true));
        assertEquals("mobile", state.getCurrentNetwork());
    }

    @Test
    public void duplicateCallbacksAreIgnored() {
        UnderlyingNetworkState<String> state = initialized("wifi");

        assertEquals(NONE, state.setUsable("wifi", true));
        assertEquals(PAUSE, state.setUsable("wifi", false));
        assertEquals(NONE, state.setUsable("wifi", false));
        assertEquals(RESUME, state.setUsable("wifi", true));
        assertEquals(NONE, state.setUsable("wifi", true));
    }

    private static UnderlyingNetworkState<String> initialized(String network) {
        UnderlyingNetworkState<String> state = new UnderlyingNetworkState<String>();
        state.initialize(networks(network), network);
        return state;
    }

    private static LinkedHashSet<String> networks(String... networks) {
        return new LinkedHashSet<String>(Arrays.asList(networks));
    }
}
