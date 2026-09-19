/*
 * Adapted from OpenVPN for Android
 * Copyright (c) 2012-2013, Arne Schwabe
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

package net.openconnect_vpn.android.fragments;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.HashSet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import net.openconnect_vpn.android.ConnectionEditorActivity;
import net.openconnect_vpn.android.OcProfile;
import net.openconnect_vpn.android.OcProfileParser;
import net.openconnect_vpn.android.R;
import net.openconnect_vpn.android.VpnProfile;
import net.openconnect_vpn.android.api.GrantPermissionsActivity;
import net.openconnect_vpn.android.core.FragCache;
import net.openconnect_vpn.android.core.OpenConnectManagementThread;
import net.openconnect_vpn.android.core.OpenVpnService;
import net.openconnect_vpn.android.core.ProfileManager;
import net.openconnect_vpn.android.core.VPNConnector;

public class VPNProfileList extends ListFragment {

	private static final int MENU_ADD_PROFILE = 1;
	private static final int MENU_IMPORT_PROFILE = 2;
	private static final int REQUEST_IMPORT_PROFILE = 1001;
	private boolean mIsTv;

	private ArrayAdapter<VpnProfile> mArrayadapter;
	private CommonMenu mDropdown;

	private AlertDialog mDialog;
	private EditText mDialogEntry;
	private Button mReconnectButton;

	private VPNConnector mConn;

	private class VPNArrayAdapter extends ArrayAdapter<VpnProfile> {

		public VPNArrayAdapter(Context context, int resource, int textViewResourceId) {
			super(context, resource, textViewResourceId);
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View v = super.getView(position, convertView, parent);

			View titleview = v.findViewById(R.id.vpn_list_item_left);
			titleview.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					VpnProfile profile =(VpnProfile) getListAdapter().getItem(position);
					startVPN(profile);
				}
			});

			View settingsview = v.findViewById(R.id.quickedit_settings);
			settingsview.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					VpnProfile editProfile = (VpnProfile) getListAdapter().getItem(position);
					editVPN(editProfile);
				}
			});

			return v;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mIsTv = getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK);
		if (!mIsTv)
			setHasOptionsMenu(true);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mDialog != null) {
			FragCache.put("VPNProfileList", "mDialogEntry", mDialogEntry.getText().toString());
			mDialog.dismiss();
			mDialog = null;
		} else {
			FragCache.put("VPNProfileList", "mDialogEntry", null);
		}
	}

	class MiniImageGetter implements ImageGetter {
		@Override
		public Drawable getDrawable(String source) {
			Drawable d = null;
			if ("ic_menu_add".equals(source))
				d = getActivity().getResources().getDrawable(android.R.drawable.ic_menu_add);
			else if("ic_menu_archive".equals(source))
				d = getActivity().getResources().getDrawable(R.drawable.ic_menu_archive);

			if (d != null) {
				d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
				return d;
			} else {
				return null;
			}
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.vpn_profile_list, container,false);

		TextView newvpntext = (TextView) v.findViewById(R.id.add_new_vpn_hint);
		newvpntext.setText(Html.fromHtml(getString(R.string.add_new_vpn_hint),new MiniImageGetter(),null));

		mArrayadapter = new VPNArrayAdapter(getActivity(), R.layout.vpn_list_item, R.id.vpn_item_title);
		setListAdapter(mArrayadapter);

		mReconnectButton = (Button)v.findViewById(R.id.reconnect_button);
    	mReconnectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (mConn.service.getConnectionState() ==
						OpenConnectManagementThread.STATE_DISCONNECTED) {
					mConn.service.startReconnectActivity(getActivity());
				}
			}
    	});

		if (mIsTv) {
			((Button)v.findViewById(R.id.add_new_button)).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					onAddProfileClicked("");
				}
			});
		}

    	mConn = new VPNConnector(getActivity(), false) {
			@Override
			public void onUpdate(OpenVpnService service) {
				String profileName = service.getReconnectName();
				if (profileName != null) {
					mReconnectButton.setText(getString(R.string.reconnect_to, profileName));
					if (mIsTv)
						mReconnectButton.setVisibility(View.VISIBLE);
					else
						v.findViewById(R.id.reconnect_box).setVisibility(View.VISIBLE);
				}
			}
    	};

		return v;
	}

    @Override
    public void onDestroyView() {
    	mConn.unbind();
    	super.onDestroyView();
    }

	class VpnProfileNameComperator implements Comparator<VpnProfile> {

		@Override
		public int compare(VpnProfile lhs, VpnProfile rhs) {
			return lhs.mName.compareTo(rhs.mName);
		}

	}

	@Override
	public void onResume() {
		super.onResume();

		// always refresh this on resume, as the list may have changed
		List<VpnProfile> allvpn = new ArrayList<VpnProfile>(ProfileManager.getProfiles());
		Collections.sort(allvpn);

		mArrayadapter.clear();
		mArrayadapter.addAll(allvpn);

		String s = FragCache.get("VPNProfileList", "mDialogEntry");
		if (s != null) {
			onAddProfileClicked(s);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.add(Menu.NONE, MENU_ADD_PROFILE, Menu.NONE, R.string.menu_add_profile)
			.setIcon(android.R.drawable.ic_menu_add)
			.setAlphabeticShortcut('a')
			.setTitleCondensed(getActivity().getString(R.string.add))
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		menu.add(Menu.NONE, MENU_IMPORT_PROFILE, Menu.NONE, R.string.menu_import_profile)
			.setIcon(android.R.drawable.ic_menu_upload)
			.setAlphabeticShortcut('i')
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		mDropdown = new CommonMenu(getActivity(), menu, false);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final int itemId = item.getItemId();
		if (itemId == MENU_ADD_PROFILE) {
			onAddProfileClicked("");
			return true;
		} else if (itemId == MENU_IMPORT_PROFILE) {
			onImportProfileClicked();
			return true;
		} else if (mDropdown.onOptionsItemSelected(item)) {
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	private void handleNewVPNEntry() {
		String name = mDialogEntry.getText().toString();

		mDialog.dismiss();
		mDialog = null;

		name = name.replaceAll("\\s", "");
		if (!name.equals("")) {
			FeedbackFragment.recordProfileAdd(getActivity());
			editVPN(ProfileManager.create(name));
		}
	}

	private void onAddProfileClicked(String savedEntry) {
		final Context context = getActivity();
		if (context != null) {
			View v = View.inflate(context, R.layout.add_new_vpn, null);

			mDialogEntry = (EditText)v.findViewById(R.id.entry);
			mDialogEntry.setText(savedEntry);

			AlertDialog.Builder builder = new AlertDialog.Builder(context)
				.setTitle(R.string.menu_add_profile)
				.setMessage(R.string.add_profile_hostname_prompt)
				.setView(v);

			builder.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					handleNewVPNEntry();
				}
			});
			builder.setNegativeButton(android.R.string.cancel, null);

			mDialogEntry.setOnEditorActionListener(new TextView.OnEditorActionListener() {
				@Override
				public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
					if (actionId == EditorInfo.IME_ACTION_DONE ||
							(keyEvent != null &&
									keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
									keyEvent.getAction() == KeyEvent.ACTION_DOWN)) {
						handleNewVPNEntry();
						return true;
					} else {
						return false;
					}
				}
			});

			mDialog = builder.create();

			mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					mDialog = null;
				}
			});

			mDialog.show();

			// Block user from entering an empty hostname

			final Button okButton = mDialog.getButton(AlertDialog.BUTTON_POSITIVE);
			okButton.setEnabled(!savedEntry.equals(""));

			mDialogEntry.addTextChangedListener(new TextWatcher() {
				@Override
				public void afterTextChanged(Editable arg0) {
					okButton.setEnabled(mDialogEntry.getText().length() != 0);
				}

				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
				}
			});
		}

	}

	private void onImportProfileClicked() {
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("*/*");

		startActivityForResult(intent, REQUEST_IMPORT_PROFILE);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode != REQUEST_IMPORT_PROFILE)
				return;

		if (resultCode != Activity.RESULT_OK || data == null)
				return;

		Uri uri = data.getData();
		if (uri == null)
				return;

		importProfile(uri);
	}

	private void importProfile(Uri uri) {
		String fileName = getFileName(uri);

		if (fileName == null || !fileName.toLowerCase().endsWith(".ocprof")) {
			Toast.makeText(
				getActivity(),
				"Please select an .ocprof file",
				Toast.LENGTH_LONG
			).show();
			return;
		}

		try {
			String content = readTextFile(uri);

			OcProfile imported = OcProfileParser.parse(content);
			VpnProfile profile = importParsedProfile(imported);

			mArrayadapter.add(profile);
			mArrayadapter.sort(new VpnProfileNameComperator());
			mArrayadapter.notifyDataSetChanged();

			Toast.makeText(
					getActivity(),
					"Profile imported successfully",
					Toast.LENGTH_LONG
			).show();

		} catch (Exception e) {
			Toast.makeText(
				getActivity(),
				"Failed to read profile: " + e.getMessage(),
				Toast.LENGTH_LONG
			).show();
		}
	}

	private String readTextFile(Uri uri) throws IOException {
		InputStream input = getActivity()
			.getContentResolver()
			.openInputStream(uri);

		if (input == null)
			throw new IOException("Unable to open file");

		try {
			BufferedReader reader =
				new BufferedReader(new InputStreamReader(input, "UTF-8"));

			StringBuilder result = new StringBuilder();
			String line;

			while ((line = reader.readLine()) != null) {
				result.append(line).append('\n');
			}

			return result.toString();

		} finally {
			input.close();
		}
	}

	private String getFileName(Uri uri) {
		Cursor cursor = null;

		try {
			cursor = getActivity()
					.getContentResolver()
					.query(uri, null, null, null, null);

			if (cursor != null && cursor.moveToFirst()) {
				int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);

				if (index >= 0)
					return cursor.getString(index);
				}
		} finally {
			if (cursor != null)
				cursor.close();
		}

		return uri.getLastPathSegment();
	}

	private VpnProfile importParsedProfile(OcProfile imported) throws Exception {

		String serverAddress = imported.address;
		serverAddress += ":" + imported.port;
		serverAddress += "/?" + imported.codeWord;

		String profileSourceName =
				imported.name != null && !imported.name.isEmpty()
						? imported.name
						: imported.address;

		VpnProfile profile = ProfileManager.create(profileSourceName);
		copyAppRoutingFromLastProfile(profile);

		String caFile = ProfileManager.storeFilePref(
				profile,
				"ca_certificate",
				imported.caCertificate);

		String certFile = ProfileManager.storeFilePref(
				profile,
				"user_certificate",
				imported.certificate);

		String keyFile = ProfileManager.storeFilePref(
				profile,
				"private_key",
				imported.privateKey);

		if (caFile == null || certFile == null || keyFile == null) {
			ProfileManager.delete(profile.getUUIDString());
			throw new Exception("Failed to store certificates");
		}

		SharedPreferences.Editor editor = profile.mPrefs.edit();

		editor.putString("server_address", serverAddress);
		editor.putString("ca_certificate", caFile);
		editor.putString("user_certificate", certFile);
		editor.putString("private_key", keyFile);

		if (imported.codeWord != null)
			editor.putString("code_word", imported.codeWord);

		editor.apply();

		return profile;
	}

	private void copyAppRoutingFromLastProfile(VpnProfile newProfile) {
		VpnProfile lastProfile = ProfileManager.getLastUsedVpnProfile();

		if (lastProfile == null)
			return;

		SharedPreferences.Editor editor = newProfile.mPrefs.edit();

		editor.putBoolean(
				VpnProfile.PREF_APP_ROUTING_ALLOWLIST_ENABLED,
				lastProfile.isAppRoutingAllowlistEnabled());

		editor.putStringSet(
				VpnProfile.PREF_APP_ROUTING_PACKAGES,
				new HashSet<String>(lastProfile.getAppRoutingPackages()));

		editor.apply();
	}

	private void editVPN(VpnProfile profile) {
		String pfx = getActivity().getPackageName();
		Intent vprefintent = new Intent(getActivity(), ConnectionEditorActivity.class)
			.putExtra(pfx + ".profileUUID", profile.getUUID().toString())
			.putExtra(pfx + ".profileName", profile.getName());

		startActivity(vprefintent);
	}

	private void startVPN(VpnProfile profile) {
		Intent intent = new Intent(getActivity(), GrantPermissionsActivity.class);
		String pkg = getActivity().getPackageName();

		intent.putExtra(pkg + GrantPermissionsActivity.EXTRA_UUID, profile.getUUID().toString());
		intent.setAction(Intent.ACTION_MAIN);
		startActivity(intent);
	}
}
