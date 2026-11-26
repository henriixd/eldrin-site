package app.mychat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    static final String URL = "https://henriixd.github.io/eldrin-site/mychat/";
    WebView web;
    PermissionRequest pending;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.parseColor("#161a40"));
        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#0f1230"));
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if ("henriixd.github.io".equals(u.getHost())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception e) { }
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest r) {
                runOnUiThread(() -> {
                    String[] need = {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};
                    boolean ok = true;
                    for (String p : need) if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) ok = false;
                    if (ok) r.grant(r.getResources());
                    else { pending = r; requestPermissions(need, 1); }
                });
            }
        });
        web.addJavascriptInterface(new Bridge(), "MyChatNative");
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel("msgs", "Mensagens", NotificationManager.IMPORTANCE_HIGH);
            ch.enableVibration(true);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2);
        }
        if (state == null) web.loadUrl(URL); else web.restoreState(state);
    }

    class Bridge {
        @JavascriptInterface
        public void vibrateLoop() {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v == null) return;
            long[] p = {0, 500, 150, 500, 150, 900, 500};
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createWaveform(p, 0));
            else v.vibrate(p, 0);
        }

        @JavascriptInterface
        public void stop() {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null) v.cancel();
        }

        @JavascriptInterface
        public void notify(String title, String body) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            Intent i = new Intent(MainActivity.this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pi = PendingIntent.getActivity(MainActivity.this, 0, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(MainActivity.this, "msgs") : new Notification.Builder(MainActivity.this);
            b.setSmallIcon(android.R.drawable.stat_notify_chat).setContentTitle(title).setContentText(body)
             .setAutoCancel(true).setContentIntent(pi).setPriority(Notification.PRIORITY_HIGH);
            try { nm.notify((int) (System.currentTimeMillis() % 100000), b.build()); } catch (Exception e) { }
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        if (code != 1 || pending == null) return;
        boolean any = false;
        for (int g : results) if (g == PackageManager.PERMISSION_GRANTED) any = true;
        if (any) pending.grant(pending.getResources()); else pending.deny();
        pending = null;
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
}
