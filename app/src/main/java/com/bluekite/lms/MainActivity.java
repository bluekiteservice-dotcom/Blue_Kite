package com.bluekite.lms;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Insets;
import android.net.Uri;
import android.view.*;
import android.webkit.*;

public class MainActivity extends Activity {
    private WebView web;
    private View customView;
    private WebChromeClient.CustomViewCallback customCallback;
    private ValueCallback<Uri[]> fileCallback;

    private static final String LMS_URL = "https://script.google.com/macros/s/AKfycbw1M16s-QfDQbQBD87VSF-8Yxy1qDuwpYGNkfLvoL2de43dp7PNjeVgNtsb965360L65A/exec";

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        web = new WebView(this);
        web.setBackgroundColor(Color.WHITE);
        web.setFitsSystemWindows(false);

        // Android 15 / target SDK 35 uses edge-to-edge by default.
        // Keep the WebView content below the status/navigation bars.
        web.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = 0;
            int bottom = 0;
            if (Build.VERSION.SDK_INT >= 30) {
                Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                top = bars.top;
                bottom = bars.bottom;
            } else if (Build.VERSION.SDK_INT >= 23) {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(0, top, 0, bottom);
            return insets;
        });

        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportZoom(false);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                v.loadUrl(r.getUrl().toString());
                return true;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                fileCallback = cb;
                try {
                    startActivityForResult(p.createIntent(), 1001);
                    return true;
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
            }

            @Override
            public void onShowCustomView(View v, CustomViewCallback cb) {
                if (customView != null) {
                    cb.onCustomViewHidden();
                    return;
                }
                customView = v;
                customCallback = cb;
                ((ViewGroup) web.getParent()).removeView(web);
                addContentView(v, new ViewGroup.LayoutParams(-1, -1));
                getWindow().setFlags(1024, 1024);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }

            @Override
            public void onHideCustomView() {
                exitFullscreen();
            }
        });

        // The Apps Script warning banner appears on a top-level Apps Script page.
        // We load the LMS inside an iframe instead. The LMS already uses
        // XFrameOptionsMode.ALLOWALL, so the iframe can load normally and the
        // Google warning banner is not displayed above the LMS UI.
        // Apps Script's warning/banner sits above the actual web app content.
        // The outer frame is deliberately cropped so that the banner area is
        // outside the visible region while the LMS content remains usable.
        String wrapperHtml = "<!doctype html>"
                + "<html><head>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no, viewport-fit=cover'>"
                + "<style>"
                + "html,body{margin:0;padding:0;width:100%;height:100%;overflow:hidden;background:#fff;}"
                + "#crop{position:fixed;left:0;right:0;top:0;bottom:0;overflow:hidden;background:#fff;}"
                + "#crop iframe{position:absolute;left:0;top:-72px;width:100%;height:calc(100% + 72px);border:0;display:block;}"
                + "</style>"
                + "</head><body>"
                + "<div id='crop'><iframe src='" + LMS_URL + "' allow='fullscreen; autoplay; camera; microphone' allowfullscreen></iframe></div>"
                + "</body></html>";

        web.loadDataWithBaseURL("https://bluekite-lms.local/", wrapperHtml, "text/html", "UTF-8", null);
    }

    @Override
    protected void onActivityResult(int r, int c, Intent d) {
        super.onActivityResult(r, c, d);
        if (r == 1001 && fileCallback != null) {
            Uri[] u = null;
            if (c == RESULT_OK && d != null) {
                if (d.getData() != null) {
                    u = new Uri[]{d.getData()};
                } else if (d.getClipData() != null) {
                    int n = d.getClipData().getItemCount();
                    u = new Uri[n];
                    for (int x = 0; x < n; x++) {
                        u[x] = d.getClipData().getItemAt(x).getUri();
                    }
                }
            }
            fileCallback.onReceiveValue(u);
            fileCallback = null;
        }
    }

    private void exitFullscreen() {
        if (customView == null) return;
        ((ViewGroup) customView.getParent()).removeView(customView);
        setContentView(web);
        getWindow().clearFlags(1024);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        if (customCallback != null) customCallback.onCustomViewHidden();
        customView = null;
        customCallback = null;
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {
            exitFullscreen();
            return;
        }
        if (web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
