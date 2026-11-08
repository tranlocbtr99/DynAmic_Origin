package com.originisle.android.ui;

import org.json.JSONObject;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.io.IOException;

public class UpgradeChecks {
    public static void main(String[] args) throws Exception {
        if (LtmUpgrade.compareVersions("v1.3.10", "1.3.9") <= 0) throw new AssertionError();
        if (LtmUpgrade.compareVersions("v1.3.5", "1.3.5") != 0) throw new AssertionError();
        if (LtmUpgrade.compareVersions("v1.3.4", "1.3.5") >= 0) throw new AssertionError();
        try {
            LtmUpgrade.compareVersions("v1.3.6-beta", "1.3.5");
            throw new AssertionError();
        } catch (IOException expected) {}
        Method select = LtmUpgrade.class.getDeclaredMethod("selectApk", JSONObject.class, String.class);
        select.setAccessible(true);
        String base = "https://github.com/tranlocbtr99/DynAmic_Origin/releases/download/v1.3.6/";
        JSONObject release = new JSONObject("{\"assets\":["
                + "{\"name\":\"LTMori-1.3.6-PackageTest-signed.apk\",\"browser_download_url\":\"" + base + "LTMori-1.3.6-PackageTest-signed.apk\"},"
                + "{\"name\":\"LTMori-1.3.6-Origin-signed.apk\",\"browser_download_url\":\"" + base + "LTMori-1.3.6-Origin-signed.apk\"}]}");
        JSONObject chosen = (JSONObject) select.invoke(null, release, "v1.3.6");
        if (!"LTMori-1.3.6-Origin-signed.apk".equals(chosen.getString("name"))) throw new AssertionError();
        release.getJSONArray("assets").remove(1);
        try {
            select.invoke(null, release, "v1.3.6");
            throw new AssertionError();
        } catch (InvocationTargetException expected) {
            if (!(expected.getCause() instanceof IOException)) throw expected;
        }
        System.out.println("Version ordering and GitHub asset selection passed");
    }
}
