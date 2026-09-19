package net.openconnect_vpn.android.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReconnectStateTest {
    private static class CancellableDialog extends UserDialog {
        CancellableDialog() {
            super(null);
        }

        @Override
        protected Object getCancelResult() {
            return "cancelled";
        }
    }

    @Test
    public void cancelUnblocksPendingDialog() {
        CancellableDialog dialog = new CancellableDialog();
        dialog.cancel();
        assertEquals("cancelled", dialog.waitForResponse());
    }

    @Test
    public void reconnectTimeoutDefaultsToThirtySeconds() {
        assertEquals(30, OpenConnectManagementThread.parseReconnectTimeout(null));
        assertEquals(30, OpenConnectManagementThread.parseReconnectTimeout("invalid"));
    }

    @Test
    public void reconnectTimeoutSupportsDisabledAndClampsMaximum() {
        assertEquals(0, OpenConnectManagementThread.parseReconnectTimeout("0"));
        assertEquals(10, OpenConnectManagementThread.parseReconnectTimeout("10"));
        assertEquals(0, OpenConnectManagementThread.parseReconnectTimeout("-1"));
        assertEquals(300, OpenConnectManagementThread.parseReconnectTimeout("999"));
    }

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
