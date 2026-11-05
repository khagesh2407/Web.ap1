package com.zkelite.compucareservice;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Bitmap;
import android.view.View;
import android.view.Gravity;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.content.Intent;
import android.net.Uri;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Button;
import android.graphics.Color;
import android.graphics.Typeface;

public class MainActivity extends Activity {
    private static final String APP_URL = "https://compucare-service-sf3up4.v2.appdeploy.ai/";
    private WebView webView;
    private ProgressBar progressBar;
    private LinearLayout errorPanel;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        FrameLayout root = new FrameLayout(this);
        webView = new WebView(this);
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        errorPanel = new LinearLayout(this);
        errorPanel.setOrientation(LinearLayout.VERTICAL);
        errorPanel.setGravity(Gravity.CENTER);
        errorPanel.setPadding(dp(28), dp(24), dp(28), dp(24));
        errorPanel.setBackgroundColor(Color.WHITE);
        errorPanel.setVisibility(View.GONE);

        TextView title = new TextView(this);
        title.setText("Connection unavailable");
        title.setTextColor(Color.rgb(25,45,75));
        title.setTextSize(21);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        TextView message = new TextView(this);
        message.setText("Check your internet connection and try again.");
        message.setTextColor(Color.DKGRAY);
        message.setTextSize(15);
        message.setGravity(Gravity.CENTER);
        message.setPadding(0, dp(12), 0, dp(20));
        Button retry = new Button(this);
        retry.setText("Try again");
        retry.setOnClickListener(v -> { errorPanel.setVisibility(View.GONE); webView.loadUrl(APP_URL); });
        errorPanel.addView(title); errorPanel.addView(message); errorPanel.addView(retry);

        root.addView(webView, new FrameLayout.LayoutParams(-1,-1));
        root.addView(errorPanel, new FrameLayout.LayoutParams(-1,-1));
        root.addView(progressBar, new FrameLayout.LayoutParams(-1,dp(3),Gravity.TOP));
        setContentView(root);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadsImagesAutomatically(true);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setSupportMultipleWindows(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                return route(r.getUrl());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView v, String u) {
                return route(Uri.parse(u));
            }
            @Override public void onPageStarted(WebView v, String u, Bitmap b) {
                errorPanel.setVisibility(View.GONE); progressBar.setVisibility(View.VISIBLE);
            }
            @Override public void onPageFinished(WebView v, String u) {
                progressBar.setVisibility(View.GONE); CookieManager.getInstance().flush();
            }
            @Override public void onReceivedError(WebView v, WebResourceRequest r, android.webkit.WebResourceError e) {
                if (r.isForMainFrame()) {
                    progressBar.setVisibility(View.GONE); errorPanel.setVisibility(View.VISIBLE);
                }
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView v, int p) {
                progressBar.setProgress(p);
                progressBar.setVisibility(p >= 100 ? View.GONE : View.VISIBLE);
            }
        });
        if (state == null) webView.loadUrl(APP_URL); else webView.restoreState(state);
    }

    private boolean route(Uri uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if ("http".equals(scheme) || "https".equals(scheme)) {
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
            if ("wa.me".equals(host) || host.endsWith(".wa.me")) { external(uri); return true; }
            return false;
        }
        if ("mailto".equals(scheme) || "tel".equals(scheme) || "sms".equals(scheme)
                || "whatsapp".equals(scheme) || "intent".equals(scheme)) { external(uri); return true; }
        return true;
    }
    private void external(Uri uri) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (Exception ignored) { }
    }
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
    @Override protected void onSaveInstanceState(Bundle out) {
        if (webView != null) webView.saveState(out);
        super.onSaveInstanceState(out);
    }
    @Override public void onBackPressed() {
        if (errorPanel != null && errorPanel.getVisibility() == View.VISIBLE) {
            errorPanel.setVisibility(View.GONE); webView.loadUrl(APP_URL);
        } else if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
    @Override protected void onDestroy() {
        if (webView != null) { webView.stopLoading(); webView.setWebChromeClient(null); webView.setWebViewClient(null); webView.destroy(); webView = null; }
        super.onDestroy();
    }
}
