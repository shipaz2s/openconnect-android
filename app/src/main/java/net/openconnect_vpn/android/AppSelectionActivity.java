/*
 * Copyright (c) 2026
 * SPDX-License-Identifier: GPL-2.0
 */

package net.openconnect_vpn.android;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.openconnect_vpn.android.core.ProfileManager;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class AppSelectionActivity extends Activity {
    public static final String EXTRA_UUID = "profileUUID";

    private SharedPreferences preferences;
    private final Set<String> selectedPackages = new HashSet<String>();
    private AppAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_selection);

        String uuid = getIntent().getStringExtra(EXTRA_UUID);
        VpnProfile profile = uuid == null ? null : ProfileManager.get(uuid);
        if (profile == null) {
            finish();
            return;
        }

        preferences = profile.mPrefs;
        selectedPackages.addAll(profile.getAppRoutingPackages());
        adapter = new AppAdapter(loadApplications());

        ListView list = (ListView)findViewById(R.id.app_list);
        list.setAdapter(adapter);
        EditText search = (EditText)findViewById(R.id.app_search);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private List<AppEntry> loadApplications() {
        PackageManager pm = getPackageManager();
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolved = pm.queryIntentActivities(launcherIntent, 0);
        Map<String, AppEntry> entries = new HashMap<String, AppEntry>();

        for (ResolveInfo info : resolved) {
            String packageName = info.activityInfo.packageName;
            if (packageName.equals(getPackageName()) || entries.containsKey(packageName)) {
                continue;
            }
            CharSequence label = info.loadLabel(pm);
            entries.put(packageName, new AppEntry(packageName,
                    label == null ? packageName : label.toString(),
                    info.loadIcon(pm), true));
        }

        for (String packageName : selectedPackages) {
            if (entries.containsKey(packageName)) {
                continue;
            }
            try {
                ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
                CharSequence label = pm.getApplicationLabel(info);
                entries.put(packageName, new AppEntry(packageName,
                        label == null ? packageName : label.toString(),
                        pm.getApplicationIcon(info), true));
            } catch (PackageManager.NameNotFoundException e) {
                entries.put(packageName, new AppEntry(packageName,
                        packageName + " — " + getString(R.string.app_routing_missing),
                        pm.getDefaultActivityIcon(), false));
            }
        }

        ArrayList<AppEntry> result = new ArrayList<AppEntry>(entries.values());
        Collections.sort(result, new Comparator<AppEntry>() {
            @Override
            public int compare(AppEntry left, AppEntry right) {
                return left.label.compareToIgnoreCase(right.label);
            }
        });
        return result;
    }

    private void setSelected(String packageName, boolean selected) {
        if (selected) {
            selectedPackages.add(packageName);
        } else {
            selectedPackages.remove(packageName);
        }
        preferences.edit().putStringSet(VpnProfile.PREF_APP_ROUTING_PACKAGES,
                new HashSet<String>(selectedPackages)).apply();
    }

    private static final class AppEntry {
        final String packageName;
        final String label;
        final Drawable icon;
        final boolean installed;

        AppEntry(String packageName, String label, Drawable icon, boolean installed) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
            this.installed = installed;
        }
    }

    private final class AppAdapter extends BaseAdapter {
        private final List<AppEntry> allEntries;
        private final List<AppEntry> visibleEntries = new ArrayList<AppEntry>();
        private final LayoutInflater inflater;

        AppAdapter(List<AppEntry> entries) {
            allEntries = entries;
            visibleEntries.addAll(entries);
            inflater = LayoutInflater.from(AppSelectionActivity.this);
        }

        void filter(String query) {
            String normalized = query.trim().toLowerCase(Locale.getDefault());
            visibleEntries.clear();
            for (AppEntry entry : allEntries) {
                if (normalized.isEmpty()
                        || entry.label.toLowerCase(Locale.getDefault()).contains(normalized)
                        || entry.packageName.toLowerCase(Locale.US).contains(normalized)) {
                    visibleEntries.add(entry);
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return visibleEntries.size();
        }

        @Override
        public AppEntry getItem(int position) {
            return visibleEntries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).packageName.hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.app_selection_row, parent, false);
                holder = new ViewHolder();
                holder.icon = (ImageView)convertView.findViewById(R.id.app_icon);
                holder.name = (TextView)convertView.findViewById(R.id.app_name);
                holder.packageName = (TextView)convertView.findViewById(R.id.app_package);
                holder.selected = (CheckBox)convertView.findViewById(R.id.app_selected);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder)convertView.getTag();
            }

            final AppEntry entry = getItem(position);
            holder.icon.setImageDrawable(entry.icon);
            holder.name.setText(entry.label);
            holder.name.setEnabled(entry.installed);
            holder.packageName.setText(entry.packageName);
            holder.packageName.setEnabled(entry.installed);
            holder.selected.setOnCheckedChangeListener(null);
            holder.selected.setChecked(selectedPackages.contains(entry.packageName));
            holder.selected.setOnCheckedChangeListener(
                    new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton button, boolean checked) {
                            setSelected(entry.packageName, checked);
                        }
                    });
            return convertView;
        }
    }

    private static final class ViewHolder {
        ImageView icon;
        TextView name;
        TextView packageName;
        CheckBox selected;
    }
}
