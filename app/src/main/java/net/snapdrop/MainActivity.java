package net.snapdrop;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends Activity {

    private final int FILE_CHOOSER_RESULT_CODE = 12;

    private WebView browser;
    private ValueCallback<Uri[]> filePath;
    Uri[] results = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        browser = findViewById(R.id.webView);
        browser.setWebChromeClient(getMyWebChromeClient());
        browser.setWebViewClient(getMyWebViewClient());
        browserSettings();
        handleIntent(getIntent());

        browser.loadUrl("https://snapdrop.net/");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
        super.onNewIntent(intent);
    }

    private void handleIntent(Intent intent) {
        Log.d("INTENT", intent.toString());
        String action = intent.getAction();
        String type = intent.getType();
        if (type == null) return;

        if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            if ("text/plain".equals(type)) {
                copyToClipboard(intent.getStringExtra(Intent.EXTRA_TEXT));
                Toast.makeText(this, "Long press and paste the text", Toast.LENGTH_LONG).show();
            } else {
                getUriFromIntent(intent);
            }
        }
    }

    private void copyToClipboard(String sharedText) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(getString(R.string.app_name), sharedText);
        clipboard.setPrimaryClip(clip);
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

    private void openWebUrl(String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void browserSettings() {
        browser.getSettings().setJavaScriptEnabled(true);
        browser.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            String fileExtension = MimeTypes.getDefaultExt(mimeType);
            browser.loadUrl(JavaScriptInterface.getBase64StringFromBlobUrl(url, mimeType, fileExtension));
        });
        browser.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        browser.getSettings().setDatabaseEnabled(true);
        browser.getSettings().setDomStorageEnabled(true);
        browser.getSettings().setUseWideViewPort(true);
        browser.getSettings().setLoadWithOverviewMode(true);
        browser.addJavascriptInterface(new JavaScriptInterface(this), "Android");
    }

    private void openFileChooserActivity() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("*/*");
        startActivityForResult(Intent.createChooser(intent, "Select files"), FILE_CHOOSER_RESULT_CODE);
    }

    private WebViewClient getMyWebViewClient() {
        return new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return super.shouldOverrideUrlLoading(view, request);
            }
        };
    }

    private WebChromeClient getMyWebChromeClient() {
        return new WebChromeClient() {
            @Override
            public void onReceivedTitle(WebView webView1, String title) {
                String url = webView1.getUrl();
                if (url == null || url.isEmpty()) return;
                try {
                    URL host = new URL(url);
                    if (!host.getHost().contains("snapdrop.net")) {
                        webView1.goBack();
                        openWebUrl(url);
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
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
        if (requestCode != FILE_CHOOSER_RESULT_CODE || filePath == null)
            return;

        if (resultCode == Activity.RESULT_OK) {
            if (intent != null) getUriFromIntent(intent);
        }
        filePath.onReceiveValue(results);
        filePath = null;
        results = null;
    }
}