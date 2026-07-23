package ng.hausanovels.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Insets;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST_CODE = 4107;

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private String startUrl;
    private Set<String> inAppHosts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply the real app theme after the launch/splash theme has been used.
        // This avoids relying on postSplashScreenTheme, which belongs to the
        // AndroidX splashscreen compat flow and breaks this no-AndroidX project.
        setTheme(R.style.Theme_HausaNovels);

        super.onCreate(savedInstanceState);

        startUrl = getString(R.string.web_app_url);
        inAppHosts = loadInAppHosts();

        configureWindow();

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(getColorCompat(R.color.app_background));
        applySafeAreaPadding(root);

        webView = new WebView(this);
        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        setContentView(root);
        configureWebView();

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else if (isNetworkAvailable()) {
            webView.loadUrl(startUrl);
        } else {
            showOfflinePage();
        }
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(getColorCompat(R.color.splash_background));
        window.setNavigationBarColor(getColorCompat(R.color.app_background));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams attrs = window.getAttributes();
            attrs.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
            window.setAttributes(attrs);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
        }
    }

    private void applySafeAreaPadding(View root) {
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int left = 0;
            int top = 0;
            int right = 0;
            int bottom = 0;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Insets safeInsets = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout()
                );
                left = safeInsets.left;
                top = safeInsets.top;
                right = safeInsets.right;
                bottom = safeInsets.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
            }

            view.setPadding(left, top, right, bottom);
            return insets;
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setUserAgentString(settings.getUserAgentString() + " HausaNovelsApp/1.0");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

        webView.setBackgroundColor(getColorCompat(R.color.app_background));
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setWebViewClient(new HausaNovelsWebViewClient());
        webView.setWebChromeClient(new HausaNovelsChromeClient());
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> openExternal(Uri.parse(url)));
    }

    private Set<String> loadInAppHosts() {
        String[] hosts = getResources().getStringArray(R.array.in_app_hosts);
        Set<String> normalized = new HashSet<>();

        for (String host : hosts) {
            String cleanHost = normalizeHost(host);
            if (!cleanHost.isEmpty()) {
                normalized.add(cleanHost);
            }
        }

        Uri startUri = Uri.parse(startUrl);
        String startHost = normalizeHost(startUri.getHost());
        if (!startHost.isEmpty()) {
            normalized.add(startHost);
        }

        return normalized;
    }

    private boolean shouldLoadInApp(Uri uri) {
        String scheme = uri.getScheme();
        if (scheme == null) {
            return false;
        }

        String lowerScheme = scheme.toLowerCase(Locale.US);
        if (!"http".equals(lowerScheme) && !"https".equals(lowerScheme)) {
            return false;
        }

        String host = normalizeHost(uri.getHost());
        if (host.isEmpty()) {
            return false;
        }

        for (String allowedHost : inAppHosts) {
            if (host.equals(allowedHost) || host.endsWith("." + allowedHost)) {
                return true;
            }
        }

        return false;
    }

    private void openExternal(Uri uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        } catch (ActivityNotFoundException ignored) {
            Toast.makeText(this, R.string.no_app_for_link, Toast.LENGTH_SHORT).show();
        }
    }

    private String normalizeHost(String host) {
        if (host == null) {
            return "";
        }

        String cleanHost = host.toLowerCase(Locale.US).trim();
        if (cleanHost.startsWith("www.")) {
            cleanHost = cleanHost.substring(4);
        }

        return cleanHost;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivity = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            return false;
        }

        NetworkInfo activeNetwork = connectivity.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    private void showOfflinePage() {
        String retryUrl = startUrl.replace("\\", "\\\\").replace("'", "\\'");
        String html = "<!doctype html><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1,viewport-fit=cover\">"
                + "<style>body{margin:0;background:#050816;color:#f8fafc;font-family:Arial,sans-serif;display:flex;align-items:center;justify-content:center;min-height:100vh;padding:28px;text-align:center}"
                + "main{max-width:360px}h1{font-size:22px;margin:0 0 10px}p{color:#a8b3c7;line-height:1.5}button{background:#21c77a;border:0;color:#03110a;padding:13px 18px;border-radius:8px;font-weight:700}</style></head>"
                + "<body><main><h1>No internet connection</h1><p>Please connect to the internet and try again.</p><button onclick=\"window.location.href='" + retryUrl + "'\">Retry</button></main></body></html>";
        webView.loadDataWithBaseURL(startUrl, html, "text/html", "UTF-8", null);
    }

    private int getColorCompat(int colorRes) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getColor(colorRes);
        }

        return getResources().getColor(colorRes);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) {
            webView.saveState(outState);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }

        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != FILE_CHOOSER_REQUEST_CODE || filePathCallback == null) {
            return;
        }

        Uri[] results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
        filePathCallback.onReceiveValue(results);
        filePathCallback = null;
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }

    private final class HausaNovelsWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            if (shouldLoadInApp(uri)) {
                return false;
            }

            openExternal(uri);
            return true;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Uri uri = Uri.parse(url);
            if (shouldLoadInApp(uri)) {
                return false;
            }

            openExternal(uri);
            return true;
        }
    }

    private final class HausaNovelsChromeClient extends WebChromeClient {
        @Override
        public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
            if (filePathCallback != null) {
                filePathCallback.onReceiveValue(null);
            }

            filePathCallback = callback;

            try {
                startActivityForResult(params.createIntent(), FILE_CHOOSER_REQUEST_CODE);
                return true;
            } catch (ActivityNotFoundException ignored) {
                filePathCallback = null;
                Toast.makeText(MainActivity.this, R.string.no_file_picker, Toast.LENGTH_SHORT).show();
                return false;
            }
        }
    }
}
