package org.alex73.android.bel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.alex73.android.common.FileInfo;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;

public class AndroidBel extends Activity {

    Set<String> translatedPackages = new HashSet<String>();
    List<FileInfo> filesForProcess = new ArrayList<FileInfo>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MyLog.create();
        new Step1(this).doit();
    }

    private void setUILanguage() {
        Locale locale = new Locale("be", "BY");
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());
    }

    protected void setGlobalLanguage() throws Exception {
        Locale locale = new Locale("be", "BY");

        Class amnClass = Class.forName("android.app.ActivityManagerNative");
        Object amn = null;
        Configuration config = null;

        // amn = ActivityManagerNative.getDefault();
        Method methodGetDefault = amnClass.getMethod("getDefault");
        methodGetDefault.setAccessible(true);
        amn = methodGetDefault.invoke(amnClass);

        // config = amn.getConfiguration();
        Method methodGetConfiguration = amnClass.getMethod("getConfiguration");
        methodGetConfiguration.setAccessible(true);
        config = (Configuration) methodGetConfiguration.invoke(amn);

        // config.userSetLocale = true;
        Class configClass = config.getClass();
        Field f = configClass.getField("userSetLocale");
        f.setBoolean(config, true);

        // set the locale to the new value
        config.locale = locale;

        // amn.updateConfiguration(config);
        Method methodUpdateConfiguration = amnClass.getMethod("updateConfiguration", Configuration.class);
        methodUpdateConfiguration.setAccessible(true);
        methodUpdateConfiguration.invoke(amn, config);
    }

    int countVersions(FileInfo.STATUS status) {
        int r = 0;
        for (FileInfo fi : filesForProcess) {
            if (fi.remoteStatus == status) {
                r++;
            }
        }
        return r;
    }
}