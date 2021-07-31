package net.snapdrop;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.net.MalformedURLException;
import java.net.URL;

public class WormholeActivity extends Activity {
    private static final int WORMHOLE_RESULT_CODE = 100;
    private static final String url = "https://wormhole.app";
    private WebView browser;
    private View loading;
    private ValueCallback<Uri[]> filePath;
    private Uri[] results = null;

    @Override
    public void onBackPressed() {
        if (browser.canGoBack()) browser.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().setStatusBarColor(getColor(android.R.color.black));
            getWindow().setNavigationBarColor(getColor(android.R.color.black));
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wormhole);

        loading = findViewById(R.id.loading);
        browser = findViewById(R.id.webView);
        browser.setWebChromeClient(getMyWebChromeClient());
        browser.setWebViewClient(getMyWebViewClient());
        browserSettings();
        browser.loadUrl(url);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void browserSettings() {
        browser.getSettings().setJavaScriptEnabled(true);
        browser.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        browser.getSettings().setDatabaseEnabled(true);
        browser.getSettings().setDomStorageEnabled(true);
        browser.getSettings().setUseWideViewPort(true);
        browser.getSettings().setLoadWithOverviewMode(true);
    }

    private void openWebUrl(String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }

    private void openFileChooserActivity() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("*/*");
        startActivityForResult(Intent.createChooser(intent, "Select files"), WORMHOLE_RESULT_CODE);
    }

    private void getUriFromIntent(Intent intent) {
        Log.d("INTENT", "getUriFrom Intent");
        String dataString = intent.getDataString();
        ClipData clipData = intent.getClipData();
        if (clipData != null) {
            results = new Uri[clipData.getItemCount()];
            for (int i = 0; i < clipData.getItemCount(); i++) {
                ClipData.Item item = clipData.getItemAt(i);
                results[i] = item.getUri();
            }
        }
        if (dataString != null)
            results = new Uri[]{Uri.parse(dataString)};
    }


    private WebViewClient getMyWebViewClient() {
        return new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri url = request.getUrl();
                if (url.toString().startsWith("mailto:")) {
                    startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse(url.toString())));
                    return true;
                }

                if (!url.getHost().contains("wormhole.app")) {
                    openWebUrl(url.toString());
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, request);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                String summary = "<html><body><br/><h1> Connecting...</h1><br/><h2> Please check your Internet and try again.</h2></body></html>";
                browser.loadData(summary, "text/html", null);
            }
        };
    }

    private WebChromeClient getMyWebChromeClient() {
        return new WebChromeClient() {

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress >= 80) {
                    loading.setVisibility(View.GONE);
                    browser.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onReceivedTitle(WebView webView1, String title) {
                String url = webView1.getUrl();
                if (url == null || url.isEmpty()) return;
                try {
                    URL host = new URL(url);
                    if (!host.getHost().contains("wormhole.app")) {
                        openWebUrl(url);
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                browser.loadUrl("javascript:var x = document.getElementsByClassName('chakra-button')[0].style.display='none';");
                new Handler().postDelayed(() ->
                                browser.loadUrl("javascript:var x = document.getElementById('chakra-toast-portal').style.display=\"none\";"),
                        500);
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                filePath = filePathCallback;
                if (results != null) {
                    filePath.onReceiveValue(results);
                    filePath = null;
                    results = null;
                } else
                    openFileChooserActivity();

                return true;
            }
        };
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode != WORMHOLE_RESULT_CODE || filePath == null)
            return;

        if (resultCode == Activity.RESULT_OK) {
            if (intent != null) getUriFromIntent(intent);
        }
        filePath.onReceiveValue(results);
        filePath = null;
        results = null;
    }
}