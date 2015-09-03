package com.pyler.textlinkify;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Preferences extends Activity {
    public static Context context;
    public static SharedPreferences prefs;
    public static PreferenceCategory appSettings;
    public static String[] entries;
    public static String[] entryValues;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new Settings()).commit();
    }

    @SuppressWarnings("deprecation")
    public static class Settings extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager()
                    .setSharedPreferencesMode(MODE_WORLD_READABLE);
            addPreferencesFromResource(R.xml.preferences);
            prefs = PreferenceManager.getDefaultSharedPreferences(context);
            appSettings = (PreferenceCategory) findPreference("app_settings");
            entries = new String[]{getString(R.string.phone_numbers), getString(R.string.web_urls), getString(R.string.email_addresses), getString(R.string.map_addresses)};
            entryValues = new String[]{Common.PHONE_NUMBERS, Common.WEB_URLS, Common.EMAIL_ADDRESSES, Common.MAP_ADDRESSES};
            CheckBoxPreference includeSystemApps = (CheckBoxPreference) findPreference("include_system_apps");
            CheckBoxPreference showAppIcon = (CheckBoxPreference) findPreference("show_app_icon");
            CheckBoxPreference customAppSettings = (CheckBoxPreference) findPreference("custom_app_settings");
            MultiSelectListPreference globalTextLinks = (MultiSelectListPreference) findPreference("global_text_links");
            Set<String> enabledItems = prefs.getStringSet(Common.GLOBAL_TEXT_LINKS, new HashSet<String>());
            int enabledItemsLength = enabledItems.size();
            String summary = "";
            for (String enabledItem : enabledItems) {
                int pos = Arrays.asList(entryValues).indexOf(enabledItem);
                String enabledValue = entries[pos];
                if (enabledItemsLength > 1) {
                    summary += enabledValue + ", ";
                } else {
                    summary += enabledValue;
                }
                enabledItemsLength--;
            }
            if (!summary.isEmpty()) {
                globalTextLinks.setSummary(summary);
            }

            includeSystemApps
                    .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(
                                Preference preference, Object newValue) {
                            reloadAppsList();
                            return true;
                        }
                    });

            customAppSettings
                    .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(
                                Preference preference, Object newValue) {
                            boolean enabled = (boolean) newValue;
                            MultiSelectListPreference globalTextLinks = (MultiSelectListPreference) findPreference("global_text_links");
                            globalTextLinks.setEnabled(!enabled);
                            if (enabled) {
                                reloadAppsList();
                            } else {
                                appSettings.removeAll();
                            }
                            return true;
                        }
                    });


            showAppIcon
                    .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(
                                Preference preference, Object newValue) {
                            PackageManager packageManager = context
                                    .getPackageManager();
                            int state = (Boolean) newValue ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                                    : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
                            String settings = context.getPackageName() + ".Settings";
                            ComponentName alias = new ComponentName(context,
                                    settings);
                            packageManager.setComponentEnabledSetting(alias,
                                    state, PackageManager.DONT_KILL_APP);
                            return true;
                        }
                    });

            if (customAppSettings.isChecked()) {
                globalTextLinks.setEnabled(false);
            }

            reloadAppsList();
        }

        public void reloadAppsList() {
            new LoadApps().execute();
        }

        public boolean isAllowedApp(ApplicationInfo appInfo) {
            boolean isAllowedApp = true;
            boolean includeSystemApps = prefs.getBoolean("include_system_apps",
                    false);
            if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                    && !includeSystemApps) {
                isAllowedApp = false;
            }
            return isAllowedApp;
        }

        public class LoadApps extends AsyncTask<Void, Void, Void> {
            MultiSelectListPreference enabledApps = (MultiSelectListPreference) findPreference("enable_for_apps");
            MultiSelectListPreference disabledApps = (MultiSelectListPreference) findPreference("disable_for_apps");
            List<CharSequence> appNames = new ArrayList<>();
            List<CharSequence> packageNames = new ArrayList<>();
            PackageManager pm = context.getPackageManager();
            List<ApplicationInfo> packages = pm
                    .getInstalledApplications(PackageManager.GET_META_DATA);
            ProgressDialog loadingApps = new ProgressDialog(getActivity());

            @Override
            protected void onPreExecute() {
                loadingApps.setMessage(getString(R.string.loading_apps));
                loadingApps.show();
                enabledApps.setEnabled(false);
                disabledApps.setEnabled(false);
                appSettings.removeAll();
            }

            @Override
            protected Void doInBackground(Void... arg0) {
                List<String[]> sortedApps = new ArrayList<>();

                for (ApplicationInfo app : packages) {
                    if (isAllowedApp(app)) {
                        sortedApps.add(new String[]{
                                app.packageName,
                                app.loadLabel(context.getPackageManager())
                                        .toString()});
                    }
                }

                Collections.sort(sortedApps, new Comparator<String[]>() {
                    @Override
                    public int compare(String[] entry1, String[] entry2) {
                        return entry1[1].compareToIgnoreCase(entry2[1]);
                    }
                });

                for (int i = 0; i < sortedApps.size(); i++) {
                    String packageName = sortedApps.get(i)[0];
                    String appName = sortedApps.get(i)[1];

                    appNames.add(appName);
                    packageNames.add(packageName);

                    boolean customAppSettings = prefs.getBoolean("custom_app_settings", false);
                    if (customAppSettings) {
                        MultiSelectListPreference preference = new MultiSelectListPreference(getActivity());

                        preference.setKey(packageName);
                        preference.setTitle(appName);
                        String dialogTitle = getString(R.string.text_links);
                        preference.setDialogTitle(dialogTitle);
                        preference.setEntries(entries);
                        preference.setEntryValues(entryValues);
                        Set<String> enabledItems = prefs.getStringSet(packageName, new HashSet<String>());
                        int enabledItemsLength = enabledItems.size();
                        String summary = "";
                        for (String enabledItem : enabledItems) {
                            int pos = Arrays.asList(entryValues).indexOf(enabledItem);
                            String enabledValue = entries[pos];
                            if (enabledItemsLength > 1) {
                                summary += enabledValue + ", ";
                            } else {
                                summary += enabledValue;
                            }
                            enabledItemsLength--;
                        }
                        if (!summary.isEmpty()) {
                            preference.setSummary(summary);
                        }
                        appSettings.addPreference(preference);
                    }
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                CharSequence[] appNamesList = appNames
                        .toArray(new CharSequence[appNames.size()]);
                CharSequence[] packageNamesList = packageNames
                        .toArray(new CharSequence[packageNames.size()]);

                enabledApps.setEntries(appNamesList);
                enabledApps.setEntryValues(packageNamesList);
                enabledApps.setEnabled(true);
                disabledApps.setEntries(appNamesList);
                disabledApps.setEntryValues(packageNamesList);
                disabledApps.setEnabled(true);
                appSettings.setEnabled(true);
                loadingApps.dismiss();
            }
        }
    }


}