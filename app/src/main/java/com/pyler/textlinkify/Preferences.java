package com.pyler.textlinkify;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Preferences extends Activity {
    public static Context context;
    public static SharedPreferences prefs;

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
            PreferenceCategory appSettings = (PreferenceCategory) findPreference("app_settings");
            Preference includeSystemApps = findPreference("include_system_apps");
            includeSystemApps
                    .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(
                                Preference preference, Object newValue) {
                            reloadAppsList();
                            return true;
                        }
                    });

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
            PreferenceCategory appSettings = (PreferenceCategory) findPreference("app_settings");

            PackageManager pm = context.getPackageManager();
            List<ApplicationInfo> packages = pm
                    .getInstalledApplications(PackageManager.GET_META_DATA);

            @Override
            protected void onPreExecute() {

                appSettings.removeAll();
                appSettings.setEnabled(false);
            }

            @Override
            protected Void doInBackground(Void... arg0) {
                List<String[]> sortedApps = new ArrayList<String[]>();

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

                    MultiSelectListPreference preference = new MultiSelectListPreference(getActivity());

                    preference.setKey(packageName);
                    preference.setTitle(appName);
                    String dialogTitle = getString(R.string.add_text_links);
                    preference.setDialogTitle(dialogTitle);
                    CharSequence[] entries = new String[]{getString(R.string.phone_numbers), getString(R.string.web_urls), getString(R.string.email_addresses), getString(R.string.map_addresses)};
                    CharSequence[] entriesValues = new String[]{Common.PHONE_NUMBERS, Common.WEB_URLS, Common.EMAIL_ADDRESSES, Common.MAP_ADDRESSES};
                    preference.setEntries(entries);
                    preference.setEntryValues(entriesValues);
                    appSettings.addPreference(preference);
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                appSettings.setEnabled(true);
            }
        }
    }


}

