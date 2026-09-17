package net.openconnect_vpn.android.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReconnectStateTest {
    @Test
    public void detectsCstpDeadPeerMessage() {
        assertTrue(OpenConnectManagementThread.isDeadPeerMessage(
                "CSTP Dead Peer Detection detected dead peer!\n"));
    }

    @Test
    public void detectsEspDeadPeerMessage() {
        assertTrue(OpenConnectManagementThread.isDeadPeerMessage(
                "ESP detected dead peer\n"));
    }

    @Test
    public void ignoresGeneralReconnectDocumentationMessage() {
        assertFalse(OpenConnectManagementThread.isDeadPeerMessage(
                "Server reports that reconnect-after-drop is allowed"));
    }

    @Test
    public void ignoresNullMessage() {
        assertFalse(OpenConnectManagementThread.isDeadPeerMessage(null));
    }
}
