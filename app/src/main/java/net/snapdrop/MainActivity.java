package net.snapdrop;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends Activity implements View.OnClickListener {

    private final int FILE_CHOOSER_RESULT_CODE = 10;
    private final int PERMISSION_REQUEST_CODE = 11;
    private final String SNAPDROP_URL = "https://snapdrop.net/";

    private WebView browser;
    private View aboutLayout;
    private ValueCallback<Uri[]> filePath;
    private Uri[] results = null;
    private String downloadUrl = null;
    private String downloadMimeType = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().setStatusBarColor(getColor(android.R.color.black));
            getWindow().setNavigationBarColor(getColor(android.R.color.black));
        }

        browser = findViewById(R.id.webView);
        browser.setWebChromeClient(getMyWebChromeClient());
        browser.setWebViewClient(getMyWebViewClient());
        browserSettings();
        handleIntent(getIntent());

        browser.loadUrl(SNAPDROP_URL);

        aboutLayout = findViewById(R.id.about_layout);
        findViewById(R.id.about).setOnClickListener(this);
        findViewById(R.id.close).setOnClickListener(this);
        findViewById(R.id.refresh).setOnClickListener(this);
        findViewById(R.id.robin_github).setOnClickListener(this);
        findViewById(R.id.robin_twitter).setOnClickListener(this);
        findViewById(R.id.tanuj_github).setOnClickListener(this);
        findViewById(R.id.tanuj_twitter).setOnClickListener(this);
        findViewById(R.id.snapdrop_web).setOnClickListener(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
        super.onNewIntent(intent);
    }

    @Override
    @SuppressLint("NonConstantResourceId")
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.refresh:
                browser.loadUrl(SNAPDROP_URL);
                break;
            case R.id.about:
                aboutLayout.setVisibility(View.VISIBLE);
                break;
            case R.id.close:
                aboutLayout.setVisibility(View.GONE);
                break;
            case R.id.robin_github:
                openWebUrl("https://github.com/RobinLinus/snapdrop");
                break;
            case R.id.robin_twitter:
                openWebUrl("https://twitter.com/robin_linus/");
                break;
            case R.id.tanuj_github:
                openWebUrl("https://github.com/tanujnotes/");
                break;
            case R.id.tanuj_twitter:
                openWebUrl("https://twitter.com/tanujnotes/");
                break;
            case R.id.snapdrop_web:
                openWebUrl(SNAPDROP_URL);
                break;
        }
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
            } else
                getUriFromIntent(intent);
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
        aboutLayout.setVisibility(View.GONE);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void browserSettings() {
        browser.getSettings().setJavaScriptEnabled(true);
        browser.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            if (hasStoragePermission()
                    || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                saveFile(url, mimeType);
            } else {
                downloadUrl = url;
                downloadMimeType = mimeType;
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            }
        });
        browser.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        browser.getSettings().setDatabaseEnabled(true);
        browser.getSettings().setDomStorageEnabled(true);
        browser.getSettings().setUseWideViewPort(true);
        browser.getSettings().setLoadWithOverviewMode(true);
        browser.addJavascriptInterface(new JavaScriptInterface(this), "Android");
    }

    private boolean hasStoragePermission() {
        String permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
        int res = checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    private void saveFile(String url, String mimeType) {
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "Please try again", Toast.LENGTH_SHORT).show();
            return;
        }
        String fileExtension = MimeTypes.getDefaultExt(mimeType);
        browser.loadUrl(JavaScriptInterface.getBase64StringFromBlobUrl(url, mimeType, fileExtension));
        downloadUrl = null;
        downloadMimeType = null;
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                saveFile(downloadUrl, downloadMimeType);
            else
                Toast.makeText(this, "Need storage permission to download files", Toast.LENGTH_LONG).show();
        } else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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