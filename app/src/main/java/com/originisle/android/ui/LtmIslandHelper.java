package com.originisle.android.ui;

import android.content.Context;
import android.content.Intent;
import android.app.PendingIntent;
import android.content.IntentFilter;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LtmIslandHelper {
    private static final String TAG = "LtmIslandHelper";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final ConcurrentHashMap<String, List<LyricLine>> LYRICS_CACHE = new ConcurrentHashMap<>();

    // Active Live Lyrics state
    private static Context sContext;
    private static MediaController sController;
    private static Intent sBaseIntent;
    private static volatile String sCurrentTrackKey;
    private static List<LyricLine> sCurrentLyrics;
    private static String sLastLyricText;
    private static boolean sTickerRunning = false;

    private LtmIslandHelper() {}

    public static class LyricLine implements Comparable<LyricLine> {
        public final long timeMs;
        public final String text;

        public LyricLine(long timeMs, String text) {
            this.timeMs = timeMs;
            this.text = text != null ? text.trim() : "";
        }

        @Override
        public int compareTo(LyricLine o) {
            return Long.compare(this.timeMs, o.timeMs);
        }
    }

    public static void promoteCharging(Context context, Intent card) {
        if (android.os.Build.VERSION.SDK_INT < 36 || !card.getBooleanExtra("oi_force_show", false)
                || !context.getSharedPreferences("experimental_prefs", Context.MODE_PRIVATE)
                        .getBoolean("cast_charging_live_updates", false)) return;
        try {
            android.app.NotificationManager manager = context.getSystemService(android.app.NotificationManager.class);
            if (manager == null || !manager.areNotificationsEnabled()
                    || !Boolean.TRUE.equals(android.app.NotificationManager.class
                            .getMethod("canPostPromotedNotifications").invoke(manager))) return;
            String channel = "ltmori_charging_live";
            manager.createNotificationChannel(new android.app.NotificationChannel(channel, "LTMori · Sạc pin",
                    android.app.NotificationManager.IMPORTANCE_LOW));
            android.app.Notification.Builder builder = new android.app.Notification.Builder(context, channel)
                    .setSmallIcon(card.getIntExtra("icon_res", android.R.drawable.ic_lock_idle_charging))
                    .setContentTitle(card.getStringExtra("title")).setContentText(card.getStringExtra("text"))
                    .setOnlyAlertOnce(true).setOngoing(true).setTimeoutAfter(6000);
            android.app.Notification.Builder.class.getMethod("setRequestPromotedOngoing", boolean.class)
                    .invoke(builder, true);
            String status = card.getStringExtra("status_chip_text");
            if (status != null) android.app.Notification.Builder.class.getMethod("setShortCriticalText", CharSequence.class)
                    .invoke(builder, status);
            android.app.Notification notification = builder.build();
            if (Boolean.TRUE.equals(android.app.Notification.class.getMethod("hasPromotableCharacteristics")
                    .invoke(notification))) manager.notify(channel, 35001, notification);
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
            // The regular OriginOS card is posted independently.
        }
    }

    public static String formatCharging(Context context, int level) {
        if (context == null) return level + "%";
        try {
            Intent battery = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery != null) {
                int voltageMv = battery.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
                BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
                if (bm != null && voltageMv > 0) {
                    int currentUa = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
                    if (currentUa != 0 && currentUa != Integer.MIN_VALUE) {
                        double watts = (voltageMv / 1000.0) * (Math.abs((long) currentUa) / 1000000.0);
                        if (watts >= 1.0 && watts <= 250.0) {
                            return String.format(Locale.US, "~%.1f W · %d%%", watts, level);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "formatCharging error: " + t.getMessage());
        }
        return level + "%";
    }

    /**
     * Feature 3: SMS OTP local regex extraction
     */
    public static String extractOtp(String message) {
        if (message == null || message.isEmpty()) return null;
        try {
            Pattern pattern = Pattern.compile("(?i)(?:otp|mã|code|xác thực|verification|của bạn là|is)\\s*[:=-]?\\s*([0-9]{4,8})");
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                return matcher.group(1);
            }
            Pattern fallback = Pattern.compile("\\b([0-9]{6})\\b");
            Matcher fbMatcher = fallback.matcher(message);
            if (fbMatcher.find()) {
                return fbMatcher.group(1);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    /**
     * Feature 4: Live Lyrics Scrolling via LRCLIB
     */
    public static synchronized void startLiveLyrics(Context context, MediaController controller, String track, String artist, Intent baseIntent) {
        if (context == null || controller == null || track == null || track.isEmpty()) {
            stopLiveLyrics();
            return;
        }

        try {
            android.content.SharedPreferences prefs = context.getSharedPreferences("experimental_prefs", Context.MODE_PRIVATE);
            if (!prefs.getBoolean("cast_live_lyrics_opt_in", false)) {
                stopLiveLyrics();
                return;
            }
        } catch (Throwable ignored) {}

        sContext = context.getApplicationContext();
        sController = controller;
        sBaseIntent = new Intent(baseIntent);

        final String trackKey = (track + "|" + (artist != null ? artist : "")).trim();

        if (trackKey.equalsIgnoreCase(sCurrentTrackKey) && sCurrentLyrics != null && !sCurrentLyrics.isEmpty()) {
            if (!sTickerRunning) {
                startTicker();
            }
            return;
        }

        sCurrentTrackKey = trackKey;
        sCurrentLyrics = null;
        sLastLyricText = null;

        List<LyricLine> cached = LYRICS_CACHE.get(trackKey.toLowerCase());
        if (cached != null) {
            sCurrentLyrics = cached;
            startTicker();
            return;
        }

        EXECUTOR.execute(() -> {
            if (!trackKey.equals(sCurrentTrackKey) || !context.getSharedPreferences("experimental_prefs", Context.MODE_PRIVATE)
                    .getBoolean("cast_live_lyrics_opt_in", false)) return;
            List<LyricLine> lyrics = fetchSyncedLyrics(track, artist);
            if (lyrics != null && !lyrics.isEmpty()) {
                LYRICS_CACHE.put(trackKey.toLowerCase(), lyrics);
                MAIN_HANDLER.post(() -> {
                    if (trackKey.equalsIgnoreCase(sCurrentTrackKey)) {
                        sCurrentLyrics = lyrics;
                        startTicker();
                    }
                });
            }
        });
    }

    public static synchronized void stopLiveLyrics() {
        sTickerRunning = false;
        MAIN_HANDLER.removeCallbacks(TICKER_RUNNABLE);
        if (sLastLyricText != null && sContext != null && sBaseIntent != null && hasActiveCard(sBaseIntent)) {
            try {
                Intent original = new Intent(sBaseIntent);
                original.removeExtra("oi_force_show");
                sContext.startService(original);
            } catch (RuntimeException unavailable) {
                // The system may have stopped the card service.
            }
        }
        sCurrentLyrics = null;
        sLastLyricText = null;
        sCurrentTrackKey = null;
    }

    private static void startTicker() {
        sTickerRunning = true;
        MAIN_HANDLER.removeCallbacks(TICKER_RUNNABLE);
        MAIN_HANDLER.post(TICKER_RUNNABLE);
    }

    private static final Runnable TICKER_RUNNABLE = new Runnable() {
        @Override
        public void run() {
            if (!sTickerRunning || sContext == null || sController == null || sCurrentLyrics == null || sCurrentLyrics.isEmpty()) {
                sTickerRunning = false;
                return;
            }

            if (!lyricsAllowed(sContext, sBaseIntent)) {
                stopLiveLyrics();
                return;
            }
            try {
                PlaybackState state = sController.getPlaybackState();
                if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
                    long currentPos = state.getPosition();
                    long lastUpdate = state.getLastPositionUpdateTime();
                    if (lastUpdate > 0) {
                        long elapsed = SystemClock.elapsedRealtime() - lastUpdate;
                        currentPos += (long) (elapsed * state.getPlaybackSpeed());
                    }

                    String activeLine = findLyricForTime(sCurrentLyrics, currentPos);
                    if (activeLine != null && !activeLine.isEmpty() && !activeLine.equals(sLastLyricText)) {
                        sLastLyricText = activeLine;
                        updateIslandWithLyric(activeLine);
                    }
                }
            } catch (Throwable t) {
                Log.w(TAG, "Ticker error: " + t.getMessage());
            }

            MAIN_HANDLER.postDelayed(this, 600);
        }
    };

    private static boolean lyricsAllowed(Context context, Intent card) {
        if (card == null || !context.getSharedPreferences("experimental_prefs", Context.MODE_PRIVATE)
                .getBoolean("cast_live_lyrics_opt_in", false)) return false;
        android.app.KeyguardManager keyguard = context.getSystemService(android.app.KeyguardManager.class);
        return keyguard != null && !keyguard.isKeyguardLocked() && hasActiveCard(card);
    }

    private static boolean hasActiveCard(Intent card) {
        try {
            Class<?> service = Class.forName("com.originisle.android.island.PlaygroundService");
            Object companion = service.getField("Companion").get(null);
            java.util.Set<?> cards = (java.util.Set<?>) companion.getClass().getMethod("getActiveCardIds").invoke(companion);
            return cards.contains(card.getIntExtra("id", -1));
        } catch (ReflectiveOperationException unavailable) {
            return false;
        }
    }

    private static void updateIslandWithLyric(String line) {
        if (sContext == null || sBaseIntent == null) return;
        try {
            Intent update = new Intent(sBaseIntent);
            update.removeExtra("oi_force_show");
            update.putExtra("text", line);
            sContext.startService(update);
        } catch (Throwable t) {
            Log.w(TAG, "Failed to update lyric on Island: " + t.getMessage());
        }
    }

    private static String findLyricForTime(List<LyricLine> lyrics, long timeMs) {
        if (lyrics == null || lyrics.isEmpty()) return null;
        String match = null;
        for (LyricLine line : lyrics) {
            if (line.timeMs <= timeMs) {
                match = line.text;
            } else {
                break;
            }
        }
        return match;
    }

    private static String cleanTrackName(String track) {
        if (track == null) return "";
        return track.replaceAll("(?i)\\s*\\(?(?:official|audio|video|lyric|mv|remix|hd|4k)[^)]*\\)?", "")
                .replaceAll("(?i)\\s*-\\s*(?:official|audio|video|lyric|mv).*", "")
                .trim();
    }

    private static List<LyricLine> fetchSyncedLyrics(String track, String artist) {
        String cleanTrack = cleanTrackName(track);
        String cleanArtist = (artist != null) ? artist.trim() : "";

        // 1. Try exact GET
        try {
            StringBuilder urlBuilder = new StringBuilder("https://lrclib.net/api/get?track_name=");
            urlBuilder.append(URLEncoder.encode(cleanTrack, StandardCharsets.UTF_8.name()));
            if (!cleanArtist.isEmpty()) {
                urlBuilder.append("&artist_name=").append(URLEncoder.encode(cleanArtist, StandardCharsets.UTF_8.name()));
            }
            String json = httpGet(urlBuilder.toString());
            String lrc = extractSyncedLyricsFromJson(json);
            if (lrc != null && !lrc.isEmpty()) {
                return parseLrc(lrc);
            }
        } catch (Throwable ignored) {}

        // 2. Try flexible search with track + artist
        try {
            String query = (cleanTrack + " " + cleanArtist).trim();
            String searchUrl = "https://lrclib.net/api/search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8.name());
            String json = httpGet(searchUrl);
            String lrc = extractSyncedLyricsFromJson(json);
            if (lrc != null && !lrc.isEmpty()) {
                return parseLrc(lrc);
            }
        } catch (Throwable ignored) {}

        // 3. Try flexible search with track only
        try {
            String searchUrl = "https://lrclib.net/api/search?q=" + URLEncoder.encode(cleanTrack, StandardCharsets.UTF_8.name());
            String json = httpGet(searchUrl);
            String lrc = extractSyncedLyricsFromJson(json);
            if (lrc != null && !lrc.isEmpty()) {
                return parseLrc(lrc);
            }
        } catch (Throwable ignored) {}

        return Collections.emptyList();
    }

    private static String httpGet(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.setRequestProperty("User-Agent", "LTMori/3.0.0 (OriginOS; Android)");
            if (conn.getResponseCode() == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append('\n');
                    }
                    return sb.toString();
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static String extractSyncedLyricsFromJson(String json) {
        if (json == null) return null;
        int keyIdx = json.indexOf("\"syncedLyrics\":");
        while (keyIdx != -1) {
            int start = json.indexOf('"', keyIdx + 15);
            if (start != -1) {
                int end = json.indexOf('"', start + 1);
                while (end != -1 && json.charAt(end - 1) == '\\') {
                    end = json.indexOf('"', end + 1);
                }
                if (end != -1) {
                    String candidate = json.substring(start + 1, end).replace("\\n", "\n").replace("\\\"", "\"");
                    if (candidate.length() > 10 && candidate.contains("[")) {
                        return candidate;
                    }
                }
            }
            keyIdx = json.indexOf("\"syncedLyrics\":", keyIdx + 15);
        }
        return null;
    }

    public static List<LyricLine> parseLrc(String lrc) {
        List<LyricLine> list = new ArrayList<>();
        if (lrc == null || lrc.isEmpty()) return list;
        String[] lines = lrc.split("\\r?\\n");
        Pattern p = Pattern.compile("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?\\]");
        for (String line : lines) {
            if (line.isEmpty()) continue;
            Matcher m = p.matcher(line);
            List<Long> times = new ArrayList<>();
            int lastEnd = 0;
            while (m.find()) {
                try {
                    long min = Long.parseLong(m.group(1));
                    long sec = Long.parseLong(m.group(2));
                    long ms = 0;
                    String msStr = m.group(3);
                    if (msStr != null) {
                        if (msStr.length() == 1) ms = Long.parseLong(msStr) * 100;
                        else if (msStr.length() == 2) ms = Long.parseLong(msStr) * 10;
                        else ms = Long.parseLong(msStr);
                    }
                    times.add((min * 60 + sec) * 1000 + ms);
                } catch (Throwable ignored) {}
                lastEnd = m.end();
            }
            String text = (lastEnd < line.length()) ? line.substring(lastEnd).trim() : "";
            for (Long t : times) {
                list.add(new LyricLine(t, text));
            }
        }
        Collections.sort(list);
        return list;
    }

    // Idle Capsule Ticker
    private static boolean sIdleTickerRunning = false;
    private static final Runnable IDLE_TICKER = new Runnable() {
        @Override
        public void run() {
            if (sContext != null && !isIslandEnabled(sContext)) {
                stopIdleTicker();
                return;
            }
            if (!sIdleTickerRunning || sContext == null) return;
            String preset = idlePrefs(sContext).getString("ltm_idle_preset", "dot");
            boolean metrics = isMetricPreset(preset);
            if (hasOtherActiveCards(sContext)) {
                // Another active card (e.g. music, call) is on the island; do not overwrite.
            } else if (!metrics || canRefreshMetrics(sContext)) {
                updateIdlePreset(sContext);
            } else {
                resetNetworkSample();
            }
            if (sIdleTickerRunning && isIslandEnabled(sContext)) {
                long delay = metrics ? 5000L
                        : ("weather".equals(preset) || "weather_wave".equals(preset)) ? 6000L : 30000L;
                MAIN_HANDLER.postDelayed(this, delay);
            }
        }
    };

    static android.content.SharedPreferences idlePrefs(Context context) {
        return context.getSharedPreferences("experimental_prefs", Context.MODE_PRIVATE);
    }

    private static boolean isMetricPreset(String preset) {
        return "ram".equals(preset) || "network".equals(preset);
    }

    private static boolean canRefreshMetrics(Context context) {
        android.os.PowerManager power = context.getSystemService(android.os.PowerManager.class);
        if (power == null || !power.isInteractive()) return false;
        try {
            Class<?> service = Class.forName("com.originisle.android.island.PlaygroundService");
            Object companion = service.getField("Companion").get(null);
            java.util.Set<?> cards = (java.util.Set<?>) companion.getClass()
                    .getMethod("getActiveCardIds").invoke(companion);
            return cards.isEmpty() || (cards.size() == 1 && cards.contains(0xee84));
        } catch (ReflectiveOperationException unavailable) {
            return true;
        }
    }

    public static boolean hasOtherActiveCards(Context context) {
        Context owner = context == null ? sContext : context;
        if (owner == null) return false;
        try {
            Class<?> service = Class.forName("com.originisle.android.island.PlaygroundService");
            Object companion = service.getField("Companion").get(null);
            java.util.Set<?> cards = (java.util.Set<?>) companion.getClass()
                    .getMethod("getActiveCardIds").invoke(companion);
            return !cards.isEmpty() && !(cards.size() == 1 && cards.contains(0xee84));
        } catch (ReflectiveOperationException unavailable) {
            return false;
        }
    }

    private static String ramLabel(Context context) {
        android.app.ActivityManager manager = context.getSystemService(android.app.ActivityManager.class);
        if (manager == null) return "—";
        android.app.ActivityManager.MemoryInfo info = new android.app.ActivityManager.MemoryInfo();
        manager.getMemoryInfo(info);
        if (info.totalMem <= 0 || info.availMem < 0 || info.availMem > info.totalMem) return "—";
        return Math.round(100.0 * (info.totalMem - info.availMem) / info.totalMem) + "%";
    }

    private static long lastRx = -1;
    private static long lastTx = -1;
    private static long lastNetworkTime;

    private static synchronized void resetNetworkSample() {
        lastRx = -1;
        lastTx = -1;
    }

    private static synchronized String networkLabel() {
        long rx = android.net.TrafficStats.getTotalRxBytes();
        long tx = android.net.TrafficStats.getTotalTxBytes();
        long now = android.os.SystemClock.elapsedRealtime();
        long elapsed = now - lastNetworkTime;
        if (rx < 0 || tx < 0) {
            resetNetworkSample();
            return "—";
        }
        if (lastRx < 0 || rx < lastRx || tx < lastTx || elapsed > 15000) {
            lastRx = rx;
            lastTx = tx;
            lastNetworkTime = now;
            return "—";
        }
        if (elapsed <= 0) return "—";
        double down = (rx - lastRx) * 1000.0 / elapsed;
        double up = (tx - lastTx) * 1000.0 / elapsed;
        lastRx = rx;
        lastTx = tx;
        lastNetworkTime = now;
        return "↓" + rate(down) + " ↑" + rate(up);
    }

    private static String rate(double bytesPerSecond) {
        if (bytesPerSecond >= 1_000_000) return String.format(Locale.getDefault(), "%.1fM", bytesPerSecond / 1_000_000);
        if (bytesPerSecond >= 1000) return String.format(Locale.getDefault(), "%.1fK", bytesPerSecond / 1000);
        return Math.round(bytesPerSecond) + "B";
    }

    public static final String MASTER_SWITCH = "ltm_island_master_enabled";
    private static final String ISLAND_SERVICE = "com.originisle.android.island.PlaygroundService";
    private static final String CARDS = "com.originisle.android.cards.";

    public static boolean isIslandEnabled(Context context) {
        Context owner = context == null ? sContext : context;
        if (owner == null) return true;
        android.content.SharedPreferences prefs = idlePrefs(owner);
        return prefs.getBoolean(MASTER_SWITCH, prefs.getBoolean("cast_notifications", true));
    }

    static void setIslandEnabled(Context context, boolean enabled) {
        android.content.SharedPreferences prefs = idlePrefs(context);
        android.content.SharedPreferences.Editor editor = prefs.edit().putBoolean(MASTER_SWITCH, enabled);
        // Older master controls stored their on/off state in cast_notifications as well.
        if (enabled && !prefs.getBoolean("cast_notifications", false)) editor.putBoolean("cast_notifications", true);
        editor.apply();
        if (enabled) {
            dispatch(context, serviceIntent(context, "ACTION_KEEPALIVE"));
            syncIdleTicker(context);
            updateIdlePreset(context);
        } else {
            stopIdleTicker();
            stopLiveLyrics();
            dismissChargingLive(context);
            dispatch(context, serviceIntent(context, "ACTION_STOP"));
        }
    }

    private static Intent serviceIntent(Context context, String action) {
        return new Intent(action).setClassName(context, ISLAND_SERVICE);
    }

    private static void dispatch(Context context, Intent intent) {
        try {
            context.startService(intent);
        } catch (IllegalStateException | SecurityException unavailable) {
            Log.w(TAG, "Island service action unavailable: " + intent.getAction());
        }
    }

    static void dismissChargingLive(Context context) {
        android.app.NotificationManager manager = context.getSystemService(android.app.NotificationManager.class);
        if (manager != null) manager.cancel("ltmori_charging_live", 35001);
    }

    static void refreshCharging(Context context, boolean enabled) {
        if (!enabled) dismissChargingLive(context);
        if (isIslandEnabled(context)) callCard("ChargingCard", "onSettingsChanged", new Class<?>[]{Context.class}, context);
    }

    static void refreshPrivacy(Context context) {
        if (isIslandEnabled(context)) callCard("PrivacyCard", "refresh", new Class<?>[0]);
    }

    public static void onIslandPermissionResult(Context context, int requestCode) {
        if (requestCode != 12012) return;
        if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            idlePrefs(context).edit().putBoolean("cast_earbuds", false).apply();
        }
        refreshEarbuds(context);
    }

    static void refreshEarbuds(Context context) {
        if (!isIslandEnabled(context)) return;
        try {
            Class<?> type = Class.forName("com.originisle.android.service.NotificationCastListener");
            Object companion = type.getField("Companion").get(null);
            boolean connected = companion.getClass().getMethod("getInstance").invoke(companion) != null;
            callCard("EarbudsCard", "onSettingsChanged", new Class<?>[]{Context.class, boolean.class}, context, connected);
        } catch (ReflectiveOperationException unavailable) {
            Log.w(TAG, "Earbuds settings refresh unavailable");
        }
    }

    static void refreshMedia(Context context, boolean enabled) {
        if (enabled || !isIslandEnabled(context)) return;
        stopLiveLyrics();
        try {
            Class<?> cache = Class.forName("com.originisle.android.service.IconCache");
            Object instance = cache.getField("INSTANCE").get(null);
            java.util.Map<?, ?> controllers = (java.util.Map<?, ?>) cache.getMethod("getActiveControllers").invoke(instance);
            for (Object id : new java.util.ArrayList<>(controllers.keySet())) {
                if (id instanceof Integer) {
                    dispatch(context, serviceIntent(context, "ACTION_CANCEL").putExtra("id", (Integer) id)
                            .putExtra("oi_scene", "NAVIGATION").putExtra("ltmori_explicit_dismiss", true));
                }
            }
        } catch (ReflectiveOperationException unavailable) {
            Log.w(TAG, "Media settings refresh unavailable");
        }
    }

    private static void callCard(String name, String method, Class<?>[] signature, Object... arguments) {
        try {
            Class<?> type = Class.forName(CARDS + name);
            type.getMethod(method, signature).invoke(type.getField("INSTANCE").get(null), arguments);
        } catch (ReflectiveOperationException unavailable) {
            Log.w(TAG, "Card settings refresh unavailable: " + name);
        }
    }

    public static void startIdleTicker(Context context) {
        if (context != null) sContext = context.getApplicationContext();
        if (!isIslandEnabled(sContext)) return;
        if (!sIdleTickerRunning) {
            sIdleTickerRunning = true;
            MAIN_HANDLER.removeCallbacks(IDLE_TICKER);
            MAIN_HANDLER.post(IDLE_TICKER);
        }
    }

    public static void stopIdleTicker() {
        sIdleTickerRunning = false;
        resetNetworkSample();
        MAIN_HANDLER.removeCallbacks(IDLE_TICKER);
    }

    /**
     * Starts or stops the idle ticker to match the saved preset. Call this after the
     * island is switched on or off so a stopped island is not revived by the ticker.
     */
    public static void syncIdleTicker(Context context) {
        if (context == null) context = sContext;
        if (context == null) return;
        sContext = context.getApplicationContext();
        if (!isIslandEnabled(sContext)) {
            stopIdleTicker();
            return;
        }
        android.content.SharedPreferences prefs =
                sContext.getSharedPreferences("experimental_prefs", Context.MODE_PRIVATE);
        String preset = prefs.getString("ltm_idle_preset", "dot");
        if (isMetricPreset(preset) || "date_earth".equals(preset) || "weather".equals(preset) || "weather_wave".equals(preset)) {
            startIdleTicker(sContext);
        } else {
            stopIdleTicker();
        }
    }

    public static void updateIdlePreset(Context context) {
        if (context == null) context = sContext;
        if (context == null) return;
        sContext = context.getApplicationContext();
        if (!isIslandEnabled(sContext)) {
            return;
        }
        postIdleCard(sContext);
    }

    /**
     * Builds and posts the idle card (id 0xee84) for the currently selected preset.
     * Replaces the upstream IdleDot.invoke() which no longer exists in origin-isle v2.2.3.
     */
    public static void postIdleCard(Context context) {
        if (context == null) context = sContext;
        if (context == null) return;
        sContext = context.getApplicationContext();
        if (!isIslandEnabled(sContext)) return;
        if (hasOtherActiveCards(sContext)) return;
        try {
            Class<?> ps = Class.forName("com.originisle.android.island.PlaygroundService");
            Intent intent = new Intent(sContext, ps);
            intent.setAction("ACTION_START");
            intent.putExtra("id", 0xee84);
            intent.putExtra("is_ongoing", true);
            intent.putExtra("oi_scene", "NAVIGATION");
            intent.putExtra("oi_force_show", false);
            intent.putExtra("oi_force_show_card", false);
            intent.putExtra("oi_show_notify", false);
            intent.putExtra("oi_is_idle_ticker", true);
            intent.putExtra("oi_dismiss_when_kill", false);
            intent.putExtra("icon_res", sContext.getApplicationInfo().icon);

            android.content.SharedPreferences prefs = idlePrefs(sContext);
            boolean geminiTap = prefs.getBoolean("ltm_idle_gemini_tap", false);
            android.content.SharedPreferences bridgePrefs = sContext.getSharedPreferences("ai_key_bridge_settings", Context.MODE_PRIVATE);
            String voiceAction = bridgePrefs.getString("voice_action", "GEMINI_VOICE");

            Intent click;
            if (geminiTap) {
                click = new Intent(sContext, com.aikeybridge.LaunchActivity.class);
                if ("GOOGLE_CTS".equals(voiceAction)) {
                    click.setAction("com.autonavi.minimap.action.CTS");
                    click.putExtra("action_mode", "GOOGLE_CTS");
                } else {
                    click.setAction("com.autonavi.minimap.action.GEMINI");
                    click.putExtra("action_mode", "GEMINI_VOICE");
                }
                click.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("oi_island_click", 1); // ISLAND_CLICK_LANDING
            } else {
                click = new Intent(sContext, Class.forName("com.originisle.android.MainActivity"));
                click.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("oi_island_click", 0);
            }
            PendingIntent pi = PendingIntent.getActivity(sContext, 0xee84, click,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            intent.putExtra("click_resp", pi);
            applyIdlePreset(sContext, intent);
            sContext.startService(intent);
        } catch (Throwable t) {
            Log.w(TAG, "postIdleCard error: " + t.getMessage());
        }
    }

    // Weather cache
    private static long sLastWeatherFetchTime = 0L;
    private static String sCachedWeatherIcon = "⛅";
    private static String sCachedWeatherTemp = "26°C";
    private static String sCachedWeatherCondition = "Nhiều mây";
    private static String sCachedWeatherHumidity = "75%";
    private static String sCachedWeatherWind = "5km/h";
    private static int sWeatherAnimFrame = 0;

    private static void fetchWeatherAsync(Context context) {
        long now = System.currentTimeMillis();
        // Update weather every 30 minutes
        if (now - sLastWeatherFetchTime < 1800000L) {
            return;
        }
        sLastWeatherFetchTime = now;
        EXECUTOR.execute(() -> {
            try {
                URL url = new URL("https://wttr.in/?lang=vi&format=%c|%t|%C|%h|%w");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);
                conn.setRequestProperty("User-Agent", "curl/7.88.1");
                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    String line = reader.readLine();
                    reader.close();
                    if (line != null && !line.trim().isEmpty()) {
                        String[] parts = line.trim().split("\\|");
                        if (parts.length >= 1) sCachedWeatherIcon = parts[0].trim();
                        if (parts.length >= 2) sCachedWeatherTemp = parts[1].replace("+", "").trim();
                        if (parts.length >= 3) sCachedWeatherCondition = parts[2].trim();
                        if (parts.length >= 4) sCachedWeatherHumidity = parts[3].trim();
                        if (parts.length >= 5) {
                            sCachedWeatherWind = parts[4].replace("←", "").replace("→", "")
                                    .replace("↑", "").replace("↓", "").trim();
                        }
                        MAIN_HANDLER.post(() -> updateIdlePreset(context));
                    }
                }
                conn.disconnect();
            } catch (Throwable t) {
                Log.w(TAG, "fetchWeatherAsync error: " + t.getMessage());
            }
        });
    }

    private static String shortenWeatherCondition(String cond) {
        if (cond == null || cond.trim().isEmpty()) return "";
        String s = cond.trim();
        if (s.length() > 12) {
            String lower = s.toLowerCase(Locale.ROOT);
            if (lower.contains("mưa rào")) return "Mưa rào";
            if (lower.contains("mưa dông")) return "Mưa dông";
            if (lower.contains("mưa")) return "Trời mưa";
            if (lower.contains("nhiều mây")) return "Nhiều mây";
            if (lower.contains("mây rải rác")) return "Mây rải rác";
            if (lower.contains("dông")) return "Có dông";
            if (lower.contains("nắng")) return "Trời nắng";
            return s.substring(0, 11) + "…";
        }
        return s;
    }

    private static ArrayList<String> getWeatherWaveColors(String icon, String condition) {
        ArrayList<String> colors = new ArrayList<>();
        String lowerCond = (condition != null ? condition.toLowerCase(Locale.ROOT) : "");
        String iconStr = (icon != null ? icon : "");
        if (iconStr.contains("⛈") || iconStr.contains("🌩") || lowerCond.contains("dông") || lowerCond.contains("sét")) {
            colors.add("#7C4DFF");
            colors.add("#651FFF");
            colors.add("#FFD600");
        } else if (iconStr.contains("🌧") || iconStr.contains("🌦") || lowerCond.contains("mưa")) {
            colors.add("#00B0FF");
            colors.add("#2979FF");
            colors.add("#00E5FF");
        } else if (iconStr.contains("☀️") || iconStr.contains("🌞") || lowerCond.contains("nắng") || lowerCond.contains("quang đãng")) {
            colors.add("#FFD600");
            colors.add("#FF9100");
            colors.add("#FF3D00");
        } else if (iconStr.contains("❄") || lowerCond.contains("tuyết")) {
            colors.add("#80D8FF");
            colors.add("#B388FF");
            colors.add("#E0F7FA");
        } else {
            colors.add("#00E676");
            colors.add("#00B0FF");
            colors.add("#78909C");
        }
        return colors;
    }

    private static void drawCloudShape(android.graphics.Canvas canvas, float cx, float cy, float width, android.graphics.Paint paint) {
        float rCenter = width * 0.28f;
        float rLeft = width * 0.20f;
        float rRight = width * 0.22f;

        float xLeft = cx - width * 0.22f;
        float yLeft = cy + width * 0.04f;

        float xCenter = cx - width * 0.02f;
        float yCenter = cy - width * 0.10f;

        float xRight = cx + width * 0.22f;
        float yRight = cy + width * 0.02f;

        android.graphics.RectF bottomRect = new android.graphics.RectF(
                xLeft - rLeft, cy - width * 0.05f, xRight + rRight, cy + width * 0.18f);
        canvas.drawRoundRect(bottomRect, rLeft, rLeft, paint);

        canvas.drawCircle(xLeft, yLeft, rLeft, paint);
        canvas.drawCircle(xCenter, yCenter, rCenter, paint);
        canvas.drawCircle(xRight, yRight, rRight, paint);
    }

    public static android.graphics.Bitmap createLiveWeatherBitmap(Context context, String icon, String condition, int frame) {
        try {
            float density = context.getResources().getDisplayMetrics().density;
            int size = Math.round(24f * density);
            if (size <= 0) size = 72;

            android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bmp);

            String lowerCond = (condition != null ? condition.toLowerCase(Locale.ROOT) : "");
            String iconStr = (icon != null ? icon : "");
            boolean isThunder = iconStr.contains("⛈") || iconStr.contains("🌩") || lowerCond.contains("dông") || lowerCond.contains("sét") || lowerCond.contains("bão");
            boolean isRain = !isThunder && (iconStr.contains("🌧") || iconStr.contains("🌦") || lowerCond.contains("mưa") || lowerCond.contains("rain") || lowerCond.contains("drizzle"));
            boolean isSnow = iconStr.contains("❄") || iconStr.contains("🌨") || lowerCond.contains("tuyết") || lowerCond.contains("snow");
            boolean isSun = !isRain && !isThunder && (iconStr.contains("☀️") || iconStr.contains("🌞") || lowerCond.contains("nắng") || lowerCond.contains("quang đãng") || lowerCond.contains("clear") || lowerCond.contains("sunny"));
            boolean isFog = iconStr.contains("🌫") || lowerCond.contains("sương") || lowerCond.contains("mù") || lowerCond.contains("fog") || lowerCond.contains("mist");

            float cx = size / 2f;
            float cy = size / 2f;

            android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);

            if (isSun) {
                // 1. ANIMATED SUN (Mặt trời tỏa nắng & xoay góc)
                float sunR = 4.8f * density;
                paint.setStyle(android.graphics.Paint.Style.FILL);
                paint.setColor(0xffffb300); // Amber core
                canvas.drawCircle(cx, cy, sunR, paint);

                paint.setColor(0xfffff9c4); // Bright center highlight
                canvas.drawCircle(cx, cy, sunR * 0.55f, paint);

                paint.setStyle(android.graphics.Paint.Style.STROKE);
                paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);
                paint.setStrokeWidth(1.4f * density);
                paint.setColor(0xffffca28);

                float rotAngle = (frame % 4) * 11.25f;
                float rayPulse = ((frame % 2) == 0 ? 0f : 1.2f) * density;
                for (int i = 0; i < 8; i++) {
                    double rad = Math.toRadians(i * 45f + rotAngle);
                    float startX = (float) (cx + Math.cos(rad) * (sunR + 1.2f * density));
                    float startY = (float) (cy + Math.sin(rad) * (sunR + 1.2f * density));
                    float stopX = (float) (cx + Math.cos(rad) * (sunR + 3.2f * density + rayPulse));
                    float stopY = (float) (cy + Math.sin(rad) * (sunR + 3.2f * density + rayPulse));
                    canvas.drawLine(startX, startY, stopX, stopY, paint);
                }
            } else if (isThunder) {
                // 2. THUNDERSTORM (Mây bão tối + Tia sét nhấp nháy + Mưa rơi)
                float cloudY = cy - 2.5f * density;
                float cloudW = 15f * density;

                paint.setStyle(android.graphics.Paint.Style.FILL);
                paint.setColor(0xff37474f); // Dark thundercloud
                drawCloudShape(canvas, cx, cloudY, cloudW, paint);

                paint.setStyle(android.graphics.Paint.Style.STROKE);
                paint.setStrokeWidth(0.9f * density);
                paint.setColor((frame % 2 == 0) ? 0xfffff176 : 0xff78909c); // Flash highlight
                drawCloudShape(canvas, cx, cloudY, cloudW, paint);

                // Lightning bolt flashes on even frames
                if ((frame % 2) == 0) {
                    paint.setStyle(android.graphics.Paint.Style.FILL_AND_STROKE);
                    paint.setStrokeWidth(0.8f * density);
                    paint.setColor(0xffffd600); // Electric Yellow
                    android.graphics.Path bolt = new android.graphics.Path();
                    bolt.moveTo(cx - 0.5f * density, cloudY + 2f * density);
                    bolt.lineTo(cx - 3.5f * density, cloudY + 6.5f * density);
                    bolt.lineTo(cx - 0.5f * density, cloudY + 6.5f * density);
                    bolt.lineTo(cx - 2.5f * density, size - 1.5f * density);
                    bolt.lineTo(cx + 1.0f * density, cloudY + 5.0f * density);
                    bolt.lineTo(cx - 1.5f * density, cloudY + 5.0f * density);
                    bolt.close();
                    canvas.drawPath(bolt, paint);
                } else {
                    // Rain drops
                    paint.setStyle(android.graphics.Paint.Style.STROKE);
                    paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);
                    paint.setStrokeWidth(1.2f * density);
                    paint.setColor(0xff40c4ff);
                    float yOff = (frame % 3) * (1.8f * density);
                    canvas.drawLine(cx - 3f * density, cloudY + 5f * density + yOff,
                            cx - 4f * density, cloudY + 7.5f * density + yOff, paint);
                    canvas.drawLine(cx + 3f * density, cloudY + 5.5f * density + yOff,
                            cx + 2f * density, cloudY + 8f * density + yOff, paint);
                }
            } else if (isRain) {
                // 3. RAIN (Mây mềm + Giọt mưa rơi liên tục theo frame)
                float cloudY = cy - 3.0f * density;
                float cloudW = 15f * density;

                // Sun peeking if partly rainy (🌦)
                if (iconStr.contains("🌦")) {
                    paint.setStyle(android.graphics.Paint.Style.FILL);
                    paint.setColor(0xffffb300);
                    canvas.drawCircle(cx - 4.5f * density, cloudY - 2f * density, 3.2f * density, paint);
                }

                paint.setStyle(android.graphics.Paint.Style.FILL);
                paint.setColor(0xff546e7a); // Cool Slate Blue
                drawCloudShape(canvas, cx, cloudY, cloudW, paint);

                paint.setStyle(android.graphics.Paint.Style.STROKE);
                paint.setStrokeWidth(0.9f * density);
                paint.setColor(0xffcfd8dc);
                drawCloudShape(canvas, cx, cloudY, cloudW, paint);

                // Rain drops falling (3 streaks at different offsets)
                paint.setStyle(android.graphics.Paint.Style.STROKE);
                paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);
                paint.setStrokeWidth(1.3f * density);
                paint.setColor(0xff29b6f6); // Vivid Cyan Blue

                float span = 5.0f * density;
                float dropLen = 2.4f * density;
                float baseY = cloudY + 4.8f * density;

                float y1 = baseY + ((frame * 1.8f * density) % span);
                float y2 = baseY + (((frame + 1) * 1.8f * density) % span);
                float y3 = baseY + (((frame + 2) * 1.8f * density) % span);

                // Slanted rain lines
                canvas.drawLine(cx - 4.5f * density, y1, cx - 5.5f * density, y1 + dropLen, paint);
                canvas.drawLine(cx, y2, cx - 1.0f * density, y2 + dropLen, paint);
                canvas.drawLine(cx + 4.5f * density, y3, cx + 3.5f * density, y3 + dropLen, paint);
            } else if (isSnow) {
                // 4. SNOW (Mây tuyết + Hạt tuyết rơi nhẹ)
                float cloudY = cy - 3.0f * density;
                float cloudW = 15f * density;

                paint.setStyle(android.graphics.Paint.Style.FILL);
                paint.setColor(0xff607d8b);
                drawCloudShape(canvas, cx, cloudY, cloudW, paint);

                paint.setStyle(android.graphics.Paint.Style.STROKE);
                paint.setStrokeWidth(0.9f * density);
                paint.setColor(0xffeceff1);
                drawCloudShape(canvas, cx, cloudY, cloudW, paint);

                paint.setStyle(android.graphics.Paint.Style.FILL);
                paint.setColor(0xffe0f7fa);
                float span = 5.5f * density;
                float baseY = cloudY + 4.5f * density;
                float sY1 = baseY + ((frame * 1.6f * density) % span);
                float sY2 = baseY + (((frame + 1) * 1.6f * density) % span);
                float sY3 = baseY + (((frame + 2) * 1.6f * density) % span);

                canvas.drawCircle(cx - 4.5f * density, sY1, 1.0f * density, paint);
                canvas.drawCircle(cx, sY2, 1.2f * density, paint);
                canvas.drawCircle(cx + 4.5f * density, sY3, 1.0f * density, paint);
            } else if (isFog) {
                // 5. FOG / MIST (Các dải sương mù lượn ngang)
                paint.setStyle(android.graphics.Paint.Style.STROKE);
                paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);
                paint.setStrokeWidth(1.8f * density);
                paint.setColor(0xd0cfd8dc);

                float drift1 = (float) Math.sin(frame * 0.8) * 1.8f * density;
                float drift2 = (float) Math.cos(frame * 0.8) * 1.8f * density;

                canvas.drawLine(cx - 7f * density + drift1, cy - 3f * density,
                        cx + 7f * density + drift1, cy - 3f * density, paint);
                canvas.drawLine(cx - 8f * density + drift2, cy,
                        cx + 6f * density + drift2, cy, paint);
                canvas.drawLine(cx - 6f * density - drift1, cy + 3f * density,
                        cx + 7f * density - drift1, cy + 3f * density, paint);
            } else {
                // 6. CLOUDY / OVERCAST (Đám mây bồng bềnh dập dềnh ngang)
                float driftX = (float) Math.sin(frame * (Math.PI / 2.0)) * (1.2f * density);
                float cloudY = cy;
                float cloudW = 16f * density;

                // Sun peeking behind cloud
                boolean hasSun = iconStr.contains("⛅") || lowerCond.contains("rải rác") || lowerCond.contains("hửng");
                if (hasSun) {
                    paint.setStyle(android.graphics.Paint.Style.FILL);
                    paint.setColor(0xffffb300);
                    float sunPulse = ((frame % 2 == 0) ? 0f : 0.6f) * density;
                    canvas.drawCircle(cx - 4.2f * density, cy - 3.8f * density, 3.4f * density + sunPulse, paint);
                }

                // Cloud body with drift
                paint.setStyle(android.graphics.Paint.Style.FILL);
                paint.setColor(0xff90a4ae); // Base cloud shadow
                drawCloudShape(canvas, cx + driftX, cloudY + 0.5f * density, cloudW, paint);

                paint.setColor(0xffeceff1); // Crisp cloud body
                drawCloudShape(canvas, cx + driftX, cloudY - 0.5f * density, cloudW, paint);

                paint.setStyle(android.graphics.Paint.Style.STROKE);
                paint.setStrokeWidth(0.9f * density);
                paint.setColor(0xffffffff);
                drawCloudShape(canvas, cx + driftX, cloudY - 0.5f * density, cloudW, paint);
            }

            return bmp;
        } catch (Throwable t) {
            Log.w(TAG, "createLiveWeatherBitmap error: " + t.getMessage());
            return textToSquareBitmap(context, icon != null ? icon : "⛅", 14, 0xffffffff);
        }
    }

    private static android.graphics.Bitmap createEarthBitmap(Context context) {
        try {
            float density = context.getResources().getDisplayMetrics().density;
            int size = Math.round(24f * density);
            if (size <= 0) size = 72;

            android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bmp);

            float cx = size / 2f;
            float cy = size / 2f;
            float radius = (size / 2f) - (2f * density);

            // 1. Quả địa cầu màu đen tối giản (Deep Black Minimalist Globe)
            android.graphics.Paint fillPaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            fillPaint.setStyle(android.graphics.Paint.Style.FILL);
            fillPaint.setColor(0xff0d1117); // Đen tuyền sang trọng
            canvas.drawCircle(cx, cy, radius, fillPaint);

            // 2. Viền ngoài sáng bạc (Outer Rim / Atmosphere)
            android.graphics.Paint strokePaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            strokePaint.setStyle(android.graphics.Paint.Style.STROKE);
            strokePaint.setStrokeWidth(1.2f * density);
            strokePaint.setColor(0xfff0f6fc); // Trắng sáng bạc
            canvas.drawCircle(cx, cy, radius, strokePaint);

            // 3. Đường xích đạo ngang & Đường kinh tuyến chính dọc
            strokePaint.setStrokeWidth(1.0f * density);
            strokePaint.setColor(0xd0e2e8f0);
            canvas.drawLine(cx - radius, cy, cx + radius, cy, strokePaint);
            canvas.drawLine(cx, cy - radius, cx, cy + radius, strokePaint);

            // 4. Các đường kinh tuyến cong hai bên (Curved Meridians)
            android.graphics.RectF meridianOval = new android.graphics.RectF(
                    cx - (radius * 0.52f), cy - radius, cx + (radius * 0.52f), cy + radius);
            canvas.drawOval(meridianOval, strokePaint);

            // 5. Hai đường vĩ tuyến cong (Tropic lines)
            strokePaint.setColor(0x99e2e8f0);
            android.graphics.RectF latNorth = new android.graphics.RectF(
                    cx - (radius * 0.85f), cy - (radius * 0.85f), cx + (radius * 0.85f), cy - (radius * 0.15f));
            canvas.drawArc(latNorth, 30, 120, false, strokePaint);

            android.graphics.RectF latSouth = new android.graphics.RectF(
                    cx - (radius * 0.85f), cy + (radius * 0.15f), cx + (radius * 0.85f), cy + (radius * 0.85f));
            canvas.drawArc(latSouth, 210, 120, false, strokePaint);

            return bmp;
        } catch (Throwable t) {
            Log.w(TAG, "createEarthBitmap error: " + t.getMessage());
            return null;
        }
    }

    public static void applyIdleCardText(Context context, Intent intent) {
        if (intent.getIntExtra("id", -1) != 0xee84) return;
        android.content.SharedPreferences prefs = idlePrefs(context);
        intent.putExtra("title", prefs.getString("ltm_idle_card_title", "LTMori"));
        intent.putExtra("text", prefs.getString("ltm_idle_card_text", "Đang chờ"));
    }

    public static void limitMessageSurfaces(Context context, Intent intent, Bundle bundle) {
        if (!intent.getBooleanExtra("ltmori_message_card", false)
                || !idlePrefs(context).getBoolean("ltmori_message_island_only", true)) return;
        String displays = "notification.superx.displays";
        if (bundle.containsKey(displays)) {
            // Omit notification and lock-screen surfaces, retaining the status-bar Island.
            bundle.putInt(displays, bundle.getInt(displays) & ~0x11);
        }
        bundle.putBoolean("notification.superx.showNotify", false);
    }

    public static void sanitizeIdleBundle(Bundle bundle) {
        if (bundle == null) return;
        try {
            Bundle island = bundle.getBundle("notification.superx.island");
            if (island != null) {
                Bundle rightInfo = island.getBundle("island.superx.rightInfo");
                if (rightInfo != null) {
                    rightInfo.remove("island.superx.rightInfo.icon");
                }
                island.remove("island.superx.rightInfo.icon");
            }
            Bundle capsule = bundle.getBundle("notification.superx.capsule");
            if (capsule != null && island != null) {
                Bundle leftInfo = island.getBundle("island.superx.leftInfo");
                if (leftInfo != null && leftInfo.containsKey("island.superx.leftInfo.icon")) {
                    android.os.Parcelable icon = leftInfo.getParcelable("island.superx.leftInfo.icon");
                    if (icon != null) {
                        capsule.putParcelable("notification.superx.capsule.icon", icon);
                    }
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "sanitizeIdleBundle: " + t.getMessage());
        }
    }

    private static android.graphics.Bitmap textToSquareBitmap(Context context, String text, int sizeSp, int color) {
        if (text == null || text.isEmpty()) return null;
        try {
            float density = context.getResources().getDisplayMetrics().scaledDensity;
            float px = sizeSp * density;

            android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            paint.setTextSize(px);
            paint.setColor(color);
            paint.setTextAlign(android.graphics.Paint.Align.CENTER);
            paint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));

            // Standard status bar icon size in Vivo: ~24dp
            int sizePx = Math.round(24f * density);
            if (sizePx <= 0) sizePx = 72;

            android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bmp);

            android.graphics.Paint.FontMetrics fm = paint.getFontMetrics();
            float textY = (sizePx / 2f) - ((fm.ascent + fm.descent) / 2f);
            canvas.drawText(text, sizePx / 2f, textY, paint);
            return bmp;
        } catch (Throwable t) {
            Log.w(TAG, "textToSquareBitmap error: " + t.getMessage());
            return null;
        }
    }

    /**
     * Feature 5: Capsule pill customization for Idle state
     */
    public static void applyIdlePreset(Intent intent) {
        if (intent == null) return;
        Context context = sContext;
        applyIdlePreset(context, intent);
    }

    public static void applyIdlePreset(Context context, Intent intent) {
        if (intent == null) return;
        try {
            int id = intent.getIntExtra("id", -1);
            if (id != 0xee84) return;

            if (context == null) context = sContext;
            if (context == null) return;
            sContext = context.getApplicationContext();

            // First apply compact title and text
            intent.putExtra("oi_template", 4);
            android.content.SharedPreferences prefs = idlePrefs(context);
            applyIdleCardText(context, intent);
            intent.removeExtra("oi_extra1");
            intent.removeExtra("oi_extra2");
            intent.removeExtra("oi_extra3");
            intent.removeExtra("oi_extra4");
            intent.removeExtra("oi_right_icon");
            intent.removeExtra("oi_left_icon");
            intent.removeExtra("oi_large_icon");
            intent.removeExtra("oi_show_right_icon");
            intent.removeExtra("oi_show_left_icon");
            intent.removeExtra("oi_right_template");
            intent.removeExtra("oi_right_content");
            intent.removeExtra("status_chip_text");
            intent.removeExtra("oi_left_content");
            intent.removeExtra("oi_wave_state");
            intent.removeExtra("oi_wave_color_list");
            intent.removeExtra("oi_custom_template");
            intent.putExtra("oi_show_right_icon", false);

            if (!isIslandEnabled(context)) {
                stopIdleTicker();
                return;
            }

            String preset = prefs.getString("ltm_idle_preset", "dot"); // dot, date_earth, weather, earth_only, wave, custom
            Log.i(TAG, "applyIdlePreset: preset=" + preset);

            // Only presets with live content need the refresh ticker; any other
            // preset must stop it, otherwise a previously started ticker keeps
            // re-posting the idle island forever.
            boolean tickingPreset = isMetricPreset(preset) || "date_earth".equals(preset) || "weather".equals(preset) || "weather_wave".equals(preset);
            if (!tickingPreset) {
                stopIdleTicker();
            }

            if (isMetricPreset(preset)) {
                boolean ram = "ram".equals(preset);
                String value = ram ? ramLabel(context) : networkLabel();
                intent.putExtra("title", ram ? "Bộ nhớ RAM" : "Tốc độ mạng");
                intent.putExtra("text", ram ? ("Đang sử dụng " + value) : value);
                android.graphics.Bitmap bitmap = textToSquareBitmap(context, ram ? "RAM" : "⇅", ram ? 9 : 15, 0xff9ed8ff);
                if (bitmap != null) {
                    android.graphics.drawable.Icon icon = android.graphics.drawable.Icon.createWithBitmap(bitmap);
                    intent.putExtra("oi_left_icon", icon);
                    intent.putExtra("oi_show_left_icon", true);
                }
                intent.putExtra("oi_left_content", "");
                intent.putExtra("oi_right_content", value);
                intent.putExtra("status_chip_text", value);
                intent.putExtra("oi_right_template", 4);
                startIdleTicker(context);
            } else if ("date_earth".equals(preset)) {
                // Preset 1: Earth icon on left, Date & Month on right
                java.util.Calendar cal = java.util.Calendar.getInstance();
                int dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK);
                String[] days = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};
                String dayStr = (dayOfWeek >= 1 && dayOfWeek <= 7) ? days[dayOfWeek - 1] : "T4";
                int dayOfMonth = cal.get(java.util.Calendar.DAY_OF_MONTH);
                int month = cal.get(java.util.Calendar.MONTH) + 1;
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", Locale.getDefault());

                String right = dayStr + " " + dayOfMonth + "/" + month + " · " + sdf.format(cal.getTime());
                intent.putExtra("title", "Ngày & Giờ");
                intent.putExtra("text", right);

                android.graphics.Bitmap earthBmp = createEarthBitmap(context);
                if (earthBmp != null) {
                    android.graphics.drawable.Icon icon = android.graphics.drawable.Icon.createWithBitmap(earthBmp);
                    intent.putExtra("oi_left_icon", icon);
                    intent.putExtra("oi_large_icon", icon);
                    intent.putExtra("oi_show_left_icon", true);
                }

                intent.putExtra("oi_left_content", "");
                intent.putExtra("oi_right_content", right);
                intent.putExtra("status_chip_text", right);
                intent.putExtra("oi_right_template", 4); // TEMPLATE_RIGHT_ISLAND_TEXT_ICON
                intent.putExtra("oi_show_right_icon", false);

                if (!sIdleTickerRunning) {
                    startIdleTicker(context);
                }
            } else if ("weather".equals(preset)) {
                // Preset 2: LiveWeather widget - Dynamic animated icon on left, alternating weather stats on right
                fetchWeatherAsync(context);
                sWeatherAnimFrame = (sWeatherAnimFrame + 1) % 12;

                android.graphics.Bitmap leftBmp = createLiveWeatherBitmap(
                        context, sCachedWeatherIcon, sCachedWeatherCondition, sWeatherAnimFrame);
                if (leftBmp != null) {
                    android.graphics.drawable.Icon icon = android.graphics.drawable.Icon.createWithBitmap(leftBmp);
                    intent.putExtra("oi_left_icon", icon);
                    intent.putExtra("oi_large_icon", icon);
                    intent.putExtra("oi_show_left_icon", true);
                }

                String right;
                int subMode = sWeatherAnimFrame % 3;
                if (subMode == 0) {
                    String cond = shortenWeatherCondition(sCachedWeatherCondition);
                    right = sCachedWeatherTemp + (cond.isEmpty() ? "" : " · " + cond);
                } else if (subMode == 1) {
                    right = sCachedWeatherTemp + " · Ẩm " + sCachedWeatherHumidity;
                } else {
                    right = sCachedWeatherTemp + " · Gió " + sCachedWeatherWind;
                }
                intent.putExtra("title", "Thời tiết");
                intent.putExtra("text", right);

                intent.putExtra("oi_left_content", "");
                intent.putExtra("oi_right_content", right);
                intent.putExtra("status_chip_text", right);
                intent.putExtra("oi_right_template", 4);
                intent.putExtra("oi_show_right_icon", false);

                if (!sIdleTickerRunning) {
                    startIdleTicker(context);
                }
            } else if ("weather_wave".equals(preset)) {
                // Preset 3: LiveWeather Wave - Dynamic animated icon on left, 60fps native color wave on right
                fetchWeatherAsync(context);
                sWeatherAnimFrame = (sWeatherAnimFrame + 1) % 12;

                android.graphics.Bitmap leftBmp = createLiveWeatherBitmap(
                        context, sCachedWeatherIcon, sCachedWeatherCondition, sWeatherAnimFrame);
                if (leftBmp != null) {
                    android.graphics.drawable.Icon icon = android.graphics.drawable.Icon.createWithBitmap(leftBmp);
                    intent.putExtra("oi_left_icon", icon);
                    intent.putExtra("oi_large_icon", icon);
                    intent.putExtra("oi_show_left_icon", true);
                }
                intent.putExtra("title", "Thời tiết");
                intent.putExtra("text", sCachedWeatherTemp + " · " + sCachedWeatherCondition);

                intent.putExtra("oi_left_content", "");
                intent.putExtra("oi_right_template", 1); // TEMPLATE_RIGHT_ISLAND_WAVE
                intent.putExtra("oi_wave_state", 1);
                intent.putExtra("oi_show_right_icon", false);
                intent.putExtra("oi_right_content", "");
                intent.putExtra("status_chip_text", "");

                ArrayList<String> colors = getWeatherWaveColors(sCachedWeatherIcon, sCachedWeatherCondition);
                intent.putStringArrayListExtra("oi_wave_color_list", colors);

                if (!sIdleTickerRunning) {
                    startIdleTicker(context);
                }
            } else if ("wave".equals(preset)) {
                // Preset 4: Animated music waves on right, Earth icon on left
                android.graphics.Bitmap earthBmp = createEarthBitmap(context);
                if (earthBmp != null) {
                    android.graphics.drawable.Icon icon = android.graphics.drawable.Icon.createWithBitmap(earthBmp);
                    intent.putExtra("oi_left_icon", icon);
                    intent.putExtra("oi_large_icon", icon);
                    intent.putExtra("oi_show_left_icon", true);
                }

                intent.putExtra("oi_left_content", "");
                intent.putExtra("oi_right_template", 1); // TEMPLATE_RIGHT_ISLAND_WAVE
                intent.putExtra("oi_wave_state", 1);
                intent.putExtra("oi_show_right_icon", false);
                // Clear text carried over from a previous preset so the wave keeps
                // the right island for itself.
                intent.putExtra("oi_right_content", "");
                intent.putExtra("status_chip_text", "");

                ArrayList<String> colors = new ArrayList<>();
                colors.add("#00E676");
                colors.add("#00B0FF");
                colors.add("#651FFF");
                intent.putStringArrayListExtra("oi_wave_color_list", colors);
            } else if ("custom".equals(preset)) {
                // Preset 5: user supplied emoji on the left, user text on the right
                String left = prefs.getString("ltm_custom_left", "🚀");
                String right = prefs.getString("ltm_custom_right", "LocTM");
                if (left == null || left.trim().isEmpty()) left = "🚀";
                if (right == null || right.trim().isEmpty()) right = "LocTM";

                android.graphics.Bitmap leftBmp = textToSquareBitmap(context, left, 14, 0xffffffff);
                if (leftBmp != null) {
                    android.graphics.drawable.Icon icon = android.graphics.drawable.Icon.createWithBitmap(leftBmp);
                    intent.putExtra("oi_left_icon", icon);
                    intent.putExtra("oi_large_icon", icon);
                    intent.putExtra("oi_show_left_icon", true);
                }

                intent.putExtra("oi_left_content", "");
                intent.putExtra("oi_right_content", right);
                intent.putExtra("status_chip_text", right);
                intent.putExtra("oi_right_template", 4);
                intent.putExtra("oi_show_right_icon", false);
            } else if ("gemini".equals(preset)) {
                android.content.SharedPreferences bridgePrefs = context.getSharedPreferences("ai_key_bridge_settings", Context.MODE_PRIVATE);
                boolean isCts = "GOOGLE_CTS".equals(bridgePrefs.getString("voice_action", "GEMINI_VOICE"));
                android.graphics.Bitmap geminiBmp = createGeminiBitmap(context);
                if (geminiBmp != null) {
                    android.graphics.drawable.Icon icon = android.graphics.drawable.Icon.createWithBitmap(geminiBmp);
                    intent.putExtra("oi_left_icon", icon);
                    intent.putExtra("oi_large_icon", icon);
                    intent.putExtra("oi_show_left_icon", true);
                }
                String geminiText = isCts ? "Circle to Search" : "Gemini Live";
                intent.putExtra("oi_left_content", "");
                intent.putExtra("oi_right_content", geminiText);
                intent.putExtra("status_chip_text", geminiText);
                intent.putExtra("oi_right_template", 4);
                intent.putExtra("oi_show_right_icon", false);
            } else if ("dot".equals(preset)) {
                android.graphics.Bitmap dotBmp = createDotBitmap(context);
                if (dotBmp != null) {
                    android.graphics.drawable.Icon icon = android.graphics.drawable.Icon.createWithBitmap(dotBmp);
                    intent.putExtra("oi_left_icon", icon);
                    intent.putExtra("oi_large_icon", icon);
                    intent.putExtra("oi_show_left_icon", true);
                }
                String blank = "\u2800";
                intent.putExtra("oi_left_content", "");
                intent.putExtra("oi_right_content", blank);
                intent.putExtra("status_chip_text", blank);
                intent.putExtra("oi_right_template", 4);
                intent.putExtra("oi_show_right_icon", false);
            } else {
                // Use the same text-and-icon template as the other idle presets (Earth only).
                android.graphics.Bitmap earth = createEarthBitmap(context);
                if (earth != null) {
                    android.graphics.drawable.Icon icon = android.graphics.drawable.Icon.createWithBitmap(earth);
                    intent.putExtra("oi_left_icon", icon);
                    intent.putExtra("oi_large_icon", icon);
                    intent.putExtra("oi_show_left_icon", true);
                }
                // A blank glyph keeps the right content field present without a visible label.
                String blank = "\u2800";
                intent.putExtra("oi_left_content", "");
                intent.putExtra("oi_right_content", blank);
                intent.putExtra("status_chip_text", blank);
                intent.putExtra("oi_right_template", 4);
                intent.putExtra("oi_show_right_icon", false);
            }
        } catch (Throwable t) {
            Log.w(TAG, "applyIdlePreset error: " + t.getMessage());
        }
    }

    private static int dp(Context context, int value) {
        if (context == null) return value;
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    public static android.graphics.Bitmap createDotBitmap(Context context) {
        int size = dp(context, 16);
        if (size <= 0) size = 32;
        android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bmp);
        android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(android.graphics.Paint.Style.FILL);
        paint.setColor(0xff3b82f6);
        canvas.drawCircle(size / 2f, size / 2f, size * 0.35f, paint);
        return bmp;
    }

    public static android.graphics.Bitmap createGeminiBitmap(Context context) {
        int size = dp(context, 20);
        if (size <= 0) size = 48;
        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(android.graphics.Paint.Style.FILL);

        android.graphics.Shader shader = new android.graphics.LinearGradient(
                0, 0, size, size,
                new int[]{0xff4285f4, 0xff9b72cb, 0xffd96570},
                null,
                android.graphics.Shader.TileMode.CLAMP
        );
        paint.setShader(shader);

        android.graphics.Path path = new android.graphics.Path();
        float mid = size / 2f;
        float r = size * 0.45f;

        path.moveTo(mid, mid - r);
        path.quadTo(mid, mid, mid + r, mid);
        path.quadTo(mid, mid, mid, mid + r);
        path.quadTo(mid, mid, mid - r, mid);
        path.quadTo(mid, mid, mid, mid - r);
        path.close();

        canvas.drawPath(path, paint);
        return bitmap;
    }

    /**
     * Google Gemini & Circle to Search Assistant Integration (AI Key Bridge).
     * Implements native SearchManager.launchAssist invocation for OriginOS.
     */
    public static boolean launchGemini(Context context) {
        if (context == null) return false;
        // 1. SearchManager.launchAssist (Native OriginOS Assistant overlay without Shizuku)
        try {
            android.app.SearchManager sm = (android.app.SearchManager) context.getSystemService(Context.SEARCH_SERVICE);
            if (sm != null) {
                java.lang.reflect.Method method = android.app.SearchManager.class.getMethod("launchAssist", Bundle.class);
                method.invoke(sm, new Bundle());
                Log.i(TAG, "Gemini launched via SearchManager.launchAssist");
                return true;
            }
        } catch (Throwable t) {
            Log.w(TAG, "SearchManager.launchAssist error: " + t.getMessage());
        }

        // 2. Explicit Intent ACTION_ASSIST to Google
        try {
            Intent assist = new Intent(Intent.ACTION_ASSIST)
                    .setPackage("com.google.android.googlequicksearchbox")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (assist.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(assist);
                return true;
            }
        } catch (Throwable t) {
            Log.w(TAG, "ACTION_ASSIST intent error: " + t.getMessage());
        }

        // 3. Open Gemini app directly
        try {
            Intent geminiApp = context.getPackageManager().getLaunchIntentForPackage("com.google.android.apps.bard");
            if (geminiApp != null) {
                geminiApp.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                context.startActivity(geminiApp);
                return true;
            }
        } catch (Throwable t) {
            Log.w(TAG, "Gemini app launch error: " + t.getMessage());
        }

        // 4. Open Google app directly
        try {
            Intent googleApp = context.getPackageManager().getLaunchIntentForPackage("com.google.android.googlequicksearchbox");
            if (googleApp != null) {
                googleApp.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                context.startActivity(googleApp);
                return true;
            }
        } catch (Throwable t) {
            Log.w(TAG, "Google app launch error: " + t.getMessage());
        }

        return false;
    }

    public static boolean launchCircleToSearch(Context context) {
        if (context == null) return false;
        try {
            com.aikeybridge.service.LaunchOutcome outcome = com.aikeybridge.service.AssistOverlay.INSTANCE.launchCircleToSearch(context);
            if (outcome.getOk()) {
                Log.i(TAG, "Circle to Search launched via AssistOverlay: " + outcome.getDetail());
                return true;
            }
        } catch (Throwable t) {
            Log.w(TAG, "AssistOverlay CTS error: " + t.getMessage());
        }

        // 2. Try LaunchActivity from AI Key Bridge
        try {
            Intent ctsIntent = new Intent(context, com.aikeybridge.LaunchActivity.class);
            ctsIntent.setAction(com.aikeybridge.LaunchActivity.ACTION_CTS);
            ctsIntent.putExtra("action_mode", "GOOGLE_CTS");
            ctsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(ctsIntent);
            return true;
        } catch (Throwable t2) {
            Log.w(TAG, "LaunchActivity CTS error: " + t2.getMessage());
        }

        return launchGemini(context);
    }

    public static boolean launchAssistant(Context context) {
        if (context == null) return false;
        try {
            android.content.SharedPreferences bridgePrefs = context.getSharedPreferences("ai_key_bridge_settings", Context.MODE_PRIVATE);
            String mode = bridgePrefs.getString("voice_action", "GEMINI_VOICE");
            if ("GOOGLE_CTS".equals(mode)) {
                return launchCircleToSearch(context);
            } else {
                return launchGemini(context);
            }
        } catch (Throwable t) {
            return launchGemini(context);
        }
    }

    public static boolean handleRecentActivityLaunch(android.app.Activity activity) {
        if (activity == null) return false;
        try {
            android.content.SharedPreferences prefs = activity.getSharedPreferences("experimental_prefs", Context.MODE_PRIVATE);
            boolean geminiTap = prefs.getBoolean("ltm_idle_gemini_tap", false);
            String preset = prefs.getString("ltm_idle_preset", "earth_only");
            if (geminiTap || "gemini".equals(preset)) {
                launchAssistant(activity);
                activity.finish();
                activity.overridePendingTransition(0, 0);
                return true;
            }
        } catch (Throwable t) {
            Log.w(TAG, "handleRecentActivityLaunch error: " + t.getMessage());
        }
        return false;
    }
}
