package app.mychat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
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
        if (state == null) web.loadUrl(URL); else web.restoreState(state);
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        if (pending == null) return;
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
