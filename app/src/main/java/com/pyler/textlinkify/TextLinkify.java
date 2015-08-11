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
        boolean includeSystemApps = prefs.getBoolean("include_system_apps",
                false);
        boolean useCustomAppSettings = prefs.getBoolean("use_custom_app_settings", false);

        if (!enabledModule) {
            return;
        }
        if (packageName == null || packageName.isEmpty()) {
            return;
        }

        if (context == null) {
            if (!"android".equals(packageName) || !includeSystemApps) {
                return;
            }
        } else {
            ApplicationInfo appInfo = context.getApplicationInfo();
            if (appInfo == null) {
                return;
            }
            if (!isAllowedApp(appInfo)) {
                return;
            }
        }
        String setting = useCustomAppSettings ? packageName : Common.GLOBAL_TEXT_LINKS;
        Set<String> textLinks = prefs.getStringSet(setting, new HashSet<String>());
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
        boolean isAllowedApp = true;
        prefs.reload();
        boolean includeSystemApps = prefs.getBoolean("include_system_apps",
                false);
        if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                && !includeSystemApps) {
            isAllowedApp = false;
        }
        return isAllowedApp;
    }
}
