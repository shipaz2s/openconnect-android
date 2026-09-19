/*
 * Copyright (c) 2019.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openconnect_vpn.android;

import android.annotation.TargetApi;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.os.Build;
import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;
import net.openconnect_vpn.android.core.OpenConnectManagementThread;
import net.openconnect_vpn.android.core.OpenVpnService;
import net.openconnect_vpn.android.core.VPNConnector;
import net.openconnect_vpn.android.core.ProfileManager;
import net.openconnect_vpn.android.api.GrantPermissionsActivity;

/**
 * @author Terry E-mail: yaoxinghuo at qq dot com
 * @date 2019-6-2 18:33
 * @description
 */
@TargetApi(24)
public class QSTileService extends TileService {

    private static final String TAG = QSTileService.class.getName();

    private int mConnectionState;
    private VPNConnector mConn;

    @Override
    public void onStartListening() {
        super.onStartListening();
        mConnectionState = -1;
        mConn = new VPNConnector(this, true) {
            @Override
            public void onUpdate(OpenVpnService service) {
                updateState(service);
            }
        };
    }

    @Override
    public void onStopListening() {
        super.onStopListening();

        mConn.stopActiveDialog();
        mConn.unbind();
    }

    @Override
    public void onClick() {
        super.onClick();

        toggle();
    }

    private void updateState(OpenVpnService service) {
        int newState = service.getConnectionState();

        if (mConnectionState != newState) {
            String tileLabel = null;
            int tileState;
            String profileName;
            switch (newState) {
                case OpenConnectManagementThread.STATE_CONNECTED:
                    tileState = Tile.STATE_ACTIVE;
                    tileLabel = getString(R.string.disconnect);
                    profileName = service.getReconnectName();
                    if (profileName != null) {
                        tileLabel = tileLabel + " " + profileName;
                    }
//                    Toast.makeText(this, getString(R.string.state_connected_to, service.profile.getName()), Toast.LENGTH_SHORT).show();
                    break;
                case OpenConnectManagementThread.STATE_DISCONNECTED:
                    tileState = Tile.STATE_INACTIVE;
                    profileName = service.getReconnectName();
                    if (profileName != null) {
                        tileLabel = getString(R.string.reconnect_to, profileName);
                    }
                    break;
                default:
                    tileLabel = service.getConnectionStateName();
                    // Connecting is not connected: keep the tile inactive and clickable.
                    tileState = Tile.STATE_INACTIVE;
                    break;
            }
            mConnectionState = newState;

            if (tileLabel == null) {
                tileLabel = getString(R.string.app);
            }

            Tile tile = getQsTile();
            tile.setState(tileState);
            tile.setLabel(tileLabel);
            Log.d(TAG, "set tile state: " + tileState + ", label: " + tileLabel);
            tile.updateTile();
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private void toggle() {
        if (mConn != null && mConn.service != null && mConn.service.isStopRequested()) {
            return;
        }
        if (mConn != null && mConn.service != null &&
                mConnectionState != OpenConnectManagementThread.STATE_DISCONNECTED) {
            mConn.service.stopVPN();
            return;
        }

        VpnProfile profile = ProfileManager.getLastUsedVpnProfile();
        if (profile != null) {
            Intent intent = new Intent(this, GrantPermissionsActivity.class);
            intent.putExtra(getPackageName() + GrantPermissionsActivity.EXTRA_UUID,
                    profile.getUUIDString());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                PendingIntent pendingIntent = PendingIntent.getActivity(this,
                        profile.getUUIDString().hashCode(), intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                startActivityAndCollapse(pendingIntent);
            } else {
                startActivityAndCollapse(intent);
            }
        }
    }
}
