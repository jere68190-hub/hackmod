package com.hackmod.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.awt.Desktop;
import java.net.URI;
import java.util.List;

public class BrowserLauncher {
    private static final Logger LOG = LoggerFactory.getLogger("hackmod-browser");
    private static final String URL = "http://localhost:" + HackWebServer.PORT;

    private static final List<String> CHROME = List.of(
        "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
        "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
        System.getProperty("user.home")+"\\AppData\\Local\\Google\\Chrome\\Application\\chrome.exe",
        "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
        "/Applications/Chromium.app/Contents/MacOS/Chromium",
        "google-chrome","google-chrome-stable","chromium","chromium-browser"
    );
    private static final List<String> EDGE = List.of(
        "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
        "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe","msedge"
    );

    public static void launch() {
        Thread t = new Thread(() -> { try{Thread.sleep(1500);}catch(Exception e){} doLaunch(); }, "hackmod-browser");
        t.setDaemon(true); t.start();
    }

    private static void doLaunch() {
        for (String exe : CHROME) if (tryApp(exe)) return;
        for (String exe : EDGE)   if (tryApp(exe)) return;
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(new URI(URL));
            else Runtime.getRuntime().exec(new String[]{"xdg-open", URL});
        } catch (Exception e) { LOG.warn("[HackMod] Open browser manually: {}", URL); }
    }

    private static boolean tryApp(String exe) {
        try {
            new ProcessBuilder(exe,"--app="+URL,"--window-size=460,780","--window-position=20,30","--no-first-run","--disable-extensions")
                .redirectErrorStream(true).start();
            Thread.sleep(300); return true;
        } catch (Exception e) { return false; }
    }
}
