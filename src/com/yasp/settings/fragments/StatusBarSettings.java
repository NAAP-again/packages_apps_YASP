/*
 * Copyright (C) 2017-2019 The PixelDust Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yasp.settings.fragments;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;

import com.yasp.settings.preferences.SecureSettingSwitchPreference;
import com.yasp.settings.preferences.SystemSettingListPreference;
import com.yasp.settings.preferences.SystemSettingMasterSwitchPreference;
import com.yasp.settings.preferences.SystemSettingSwitchPreference;

@SearchIndexable
public class StatusBarSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String SYSTEMUI_PACKAGE = "com.android.systemui";
    private static final String CONFIG_RESOURCE_NAME = "flag_combined_status_bar_signal_icons";

    private static final String NETWORK_TRAFFIC_STATE = "network_traffic_state";
    private static final String COBINED_STATUSBAR_ICONS = "show_combined_status_bar_signal_icons";
    private static final String BATTERY_STYLE = "status_bar_battery_style";
    private static final String SHOW_BATTERY_PERCENT = "status_bar_show_battery_percent";
    private static final String SHOW_BATTERY_PERCENT_INSIDE = "status_bar_show_battery_percent_inside";

    private SystemSettingMasterSwitchPreference mNetTrafficState;
    private SecureSettingSwitchPreference mCombinedIcons;
    private SystemSettingListPreference mBatteryStyle;
    private SystemSettingSwitchPreference mBatteryPercent;
    private SystemSettingSwitchPreference mBatteryPercentInside;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.yaap_settings_statusbar);
        PreferenceScreen prefSet = getPreferenceScreen();
        final ContentResolver resolver = getActivity().getContentResolver();

        mNetTrafficState = (SystemSettingMasterSwitchPreference)
                findPreference(NETWORK_TRAFFIC_STATE);
        mNetTrafficState.setOnPreferenceChangeListener(this);
        boolean enabled = Settings.System.getInt(resolver,
                Settings.System.NETWORK_TRAFFIC_STATE, 0) == 1;
        mNetTrafficState.setChecked(enabled);

        mCombinedIcons = (SecureSettingSwitchPreference)
                findPreference(COBINED_STATUSBAR_ICONS);
        Resources sysUIRes = null;
        boolean def = false;
        int resId = 0;
        try {
            sysUIRes = getActivity().getPackageManager()
                    .getResourcesForApplication(SYSTEMUI_PACKAGE);
        } catch (Exception ignored) {
            // If you don't have system UI you have bigger issues
        }
        if (sysUIRes != null) {
            resId = sysUIRes.getIdentifier(
                    CONFIG_RESOURCE_NAME, "bool", SYSTEMUI_PACKAGE);
            if (resId != 0) def = sysUIRes.getBoolean(resId);
        }
        enabled = Settings.Secure.getInt(resolver,
                COBINED_STATUSBAR_ICONS, def ? 1 : 0) == 1;
        mCombinedIcons.setChecked(enabled);
        mCombinedIcons.setOnPreferenceChangeListener(this);

        mBatteryPercentInside = (SystemSettingSwitchPreference)
                findPreference(SHOW_BATTERY_PERCENT_INSIDE);
        mBatteryPercent = (SystemSettingSwitchPreference)
                findPreference(SHOW_BATTERY_PERCENT);
        enabled = Settings.System.getIntForUser(resolver,
                SHOW_BATTERY_PERCENT, 0, UserHandle.USER_CURRENT) == 1;
        mBatteryPercent.setChecked(enabled);
        mBatteryPercent.setOnPreferenceChangeListener(this);
        mBatteryPercentInside.setEnabled(enabled);

        mBatteryStyle = (SystemSettingListPreference)
                findPreference(BATTERY_STYLE);
        int value = Settings.System.getIntForUser(resolver,
                BATTERY_STYLE, 0, UserHandle.USER_CURRENT);
        mBatteryStyle.setValue(Integer.toString(value));
        mBatteryStyle.setSummary(mBatteryStyle.getEntry());
        mBatteryStyle.setOnPreferenceChangeListener(this);
        updatePercentEnablement(value != 2);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mNetTrafficState) {
            boolean enabled = (boolean) objValue;
            Settings.System.putInt(resolver,
                    Settings.System.NETWORK_TRAFFIC_STATE, enabled ? 1 : 0);
            return true;
        } else if (preference == mBatteryStyle) {
            int value = Integer.parseInt((String) objValue);
            int index = mBatteryStyle.findIndexOfValue((String) objValue);
            mBatteryStyle.setSummary(mBatteryStyle.getEntries()[index]);
            Settings.System.putIntForUser(resolver,
                    BATTERY_STYLE, value, UserHandle.USER_CURRENT);
            updatePercentEnablement(value != 2);
            return true;
        } else if (preference == mBatteryPercent) {
            boolean enabled = (boolean) objValue;
            Settings.System.putInt(resolver,
                    SHOW_BATTERY_PERCENT, enabled ? 1 : 0);
            mBatteryPercentInside.setEnabled(enabled);
            return true;
        } else if (preference == mCombinedIcons) {
            boolean enabled = (boolean) objValue;
            Settings.Secure.putInt(resolver,
                    COBINED_STATUSBAR_ICONS, enabled ? 1 : 0);
            return true;
        }
        return false;
    }

    private void updatePercentEnablement(boolean enabled) {
        mBatteryPercent.setEnabled(enabled);
        mBatteryPercentInside.setEnabled(enabled && mBatteryPercent.isChecked());
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.YASP;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.yaap_settings_statusbar);
}
