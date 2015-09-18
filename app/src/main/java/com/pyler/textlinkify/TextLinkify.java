package com.pyler.textlinkify;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.text.util.Linkify;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

public class TextLinkify implements IXposedHookZygoteInit {
    public XSharedPreferences prefs;
    public XC_MethodHook textLinkifyHook;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        prefs = new XSharedPreferences(TextLinkify.class.getPackage().getName());
        prefs.makeWorldReadable();

        textLinkifyHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param)
                    throws Throwable {
                Context context = AndroidAppHelper.currentApplication();
                TextView textView = (TextView) param.thisObject;
                String packageName = AndroidAppHelper.currentPackageName();
                addTextLinks(context, textView, packageName);
            }
        };
        XposedBridge.hookAllConstructors(TextView.class, textLinkifyHook);
    }


    public void addTextLinks(Context context, TextView textView, String packageName) {
        int textLinkFlags = 0;
        prefs.reload();
        boolean enabledModule = prefs.getBoolean("enable_module", true);
        boolean customAppSettings = prefs.getBoolean("custom_app_settings", false);

        if (!enabledModule) {
            return;
        }
        if (packageName == null || packageName.isEmpty()) {
            return;
        }

        ApplicationInfo appInfo = context.getApplicationInfo();
        if (!isAllowedApp(appInfo)) {
            return;
        }

        boolean enabledForAllApps = prefs.getBoolean("enable_for_all_apps",
                false);
        if (enabledForAllApps) {
            Set<String> disabledApps = prefs.getStringSet("disable_for_apps",
                    new HashSet<String>());
            if (disabledApps.contains(packageName)) {
                return;
            }
        } else {
            Set<String> enabledApps = prefs.getStringSet("enable_for_apps",
                    new HashSet<String>());
            if (enabledApps.isEmpty() || !enabledApps.contains(packageName)) {
                return;
            }
        }

        String pref = customAppSettings ? packageName : Common.GLOBAL_TEXT_LINKS;
        Set<String> textLinks = prefs.getStringSet(pref, new HashSet<String>());
        if (textLinks.isEmpty()) {
            return;
        }

        if (textLinks.contains(Common.PHONE_NUMBERS))

        {
            textLinkFlags |= Linkify.PHONE_NUMBERS;
        }
        if (textLinks.contains(Common.WEB_URLS))

        {
            textLinkFlags |= Linkify.WEB_URLS;
        }
        if (textLinks.contains(Common.EMAIL_ADDRESSES))

        {
            textLinkFlags |= Linkify.EMAIL_ADDRESSES;
        }
        if (textLinks.contains(Common.MAP_ADDRESSES)) {


            textLinkFlags |= Linkify.MAP_ADDRESSES;
        }

        if (textLinkFlags != 0)

        {
            textView.setAutoLinkMask(textLinkFlags);
        }

    }

    public boolean isAllowedApp(ApplicationInfo appInfo) {
        prefs.reload();
        boolean includeSystemApps = prefs.getBoolean("include_system_apps",
                false);
        if (appInfo == null) {
            return includeSystemApps;
        } else {
            if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                    && !includeSystemApps) {
                return false;
            }
        }
        return true;
    }
}
