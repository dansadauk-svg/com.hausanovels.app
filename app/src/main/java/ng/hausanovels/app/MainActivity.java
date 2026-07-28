package ng.hausanovels.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Insets;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST_CODE = 4107;
    private static final int WEB_CACHE_VERSION = 5;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private WebView webView;
    private FrameLayout appRoot;
    private View splashOverlay;
    private ProgressBar splashProgress;
    private TextView loadingPercent;
    private TextView loadingStatus;
    private ValueCallback<Uri[]> filePathCallback;
    private String startUrl;
    private Set<String> inAppHosts;
    private int displayedProgress = 0;
    private boolean initialPageFinished = false;
    private boolean splashDismissed = false;
    private boolean showingOfflinePage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_HausaNovels);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        bindViews();

        startUrl = getString(R.string.web_app_url);
        inAppHosts = loadInAppHosts();

        String deepLinkUrl = resolveIntentUrl(getIntent());
        if (deepLinkUrl != null) {
            startUrl = deepLinkUrl;
        }

        configureWindow();
        configureWebView();
        registerPredictiveBack();
        startProgressSafetyTimer();

        if (savedInstanceState != null && webView.restoreState(savedInstanceState) != null) {
            updateSplashProgress(35);
        } else if (isNetworkAvailable()) {
            webView.loadUrl(startUrl);
        } else {
            showOfflinePage();
        }
    }

    private void bindViews() {
        appRoot = findViewById(R.id.app_root);
        webView = findViewById(R.id.web_view);
        splashOverlay = findViewById(R.id.splash_overlay);
        splashProgress = findViewById(R.id.splash_progress);
        loadingPercent = findViewById(R.id.loading_percent);
        loadingStatus = findViewById(R.id.loading_status);
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(attributes);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                );
            }
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }

        appRoot.setOnApplyWindowInsetsListener((view, insets) -> {
            int left;
            int top;
            int right;
            int bottom;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Insets safe = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout()
                );
                left = safe.left;
                top = safe.top;
                right = safe.right;
                bottom = safe.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
            }

            view.setPadding(left, top, right, bottom);
            return insets;
        });
        appRoot.requestApplyInsets();
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
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(false);
        settings.setTextZoom(100);
        settings.setDefaultFontSize(16);
        settings.setDefaultFixedFontSize(13);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // Use a Chrome-style mobile user agent without an app-specific suffix.
        // This prevents the website from applying larger app-only typography.
        String chromeStyleUserAgent = settings.getUserAgentString()
                .replace("; wv", "")
                .replace("Version/4.0 ", "");
        settings.setUserAgentString(chromeStyleUserAgent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(true);
        }

        WebView.setWebContentsDebuggingEnabled(false);
        webView.setBackgroundColor(getColorCompat(R.color.app_background));
        webView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        webView.setWebViewClient(new HausaNovelsWebViewClient());
        webView.setWebChromeClient(new HausaNovelsChromeClient());
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> openExternal(Uri.parse(url)));

        clearWebCacheAfterAppUpgrade();
    }

    private void clearWebCacheAfterAppUpgrade() {
        SharedPreferences preferences = getSharedPreferences("hausanovels_app", MODE_PRIVATE);
        int storedVersion = preferences.getInt("web_cache_version", 0);
        if (storedVersion < WEB_CACHE_VERSION) {
            webView.clearCache(true);
            preferences.edit().putInt("web_cache_version", WEB_CACHE_VERSION).apply();
        }
    }

    private void startProgressSafetyTimer() {
        mainHandler.postDelayed(() -> {
            if (!splashDismissed && displayedProgress < 90) {
                updateSplashProgress(90);
                loadingStatus.setText(R.string.loading_finishing);
            }
        }, 12000L);
    }

    private void updateSplashProgress(int requestedProgress) {
        if (splashDismissed) {
            return;
        }

        int nextProgress = Math.max(displayedProgress, Math.min(100, requestedProgress));
        displayedProgress = nextProgress;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            splashProgress.setProgress(nextProgress, true);
        } else {
            splashProgress.setProgress(nextProgress);
        }
        loadingPercent.setText(String.format(Locale.US, "%d%%", nextProgress));

        if (nextProgress < 25) {
            loadingStatus.setText(R.string.loading_preparing);
        } else if (nextProgress < 55) {
            loadingStatus.setText(R.string.loading_connecting);
        } else if (nextProgress < 88) {
            loadingStatus.setText(R.string.loading_content);
        } else if (nextProgress < 100) {
            loadingStatus.setText(R.string.loading_finishing);
        } else {
            loadingStatus.setText(R.string.loading_ready);
        }

        if (nextProgress >= 100 && initialPageFinished) {
            dismissSplashOverlay();
        }
    }

    private void dismissSplashOverlay() {
        if (splashDismissed) {
            return;
        }

        splashDismissed = true;
        splashOverlay.animate()
                .alpha(0f)
                .setDuration(320L)
                .withEndAction(() -> {
                    splashOverlay.setVisibility(View.GONE);
                    splashOverlay.setClickable(false);
                })
                .start();
    }

    private String resolveIntentUrl(Intent intent) {
        if (intent == null || intent.getData() == null) {
            return null;
        }

        Uri uri = intent.getData();
        return shouldLoadInApp(uri) ? uri.toString() : null;
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
        if ("about".equals(lowerScheme) || "data".equals(lowerScheme)
                || "blob".equals(lowerScheme) || "javascript".equals(lowerScheme)) {
            return true;
        }

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
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }

        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) {
            return false;
        }

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void showOfflinePage() {
        if (showingOfflinePage) {
            return;
        }
        showingOfflinePage = true;

        String retryUrl = startUrl.replace("\\", "\\\\").replace("'", "\\'");
        String html = "<!doctype html><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1,viewport-fit=cover\">"
                + "<style>body{box-sizing:border-box;margin:0;background:#050816;color:#f8fafc;font-family:system-ui,-apple-system,Arial,sans-serif;display:flex;align-items:center;justify-content:center;min-height:100vh;padding:28px;text-align:center}"
                + "main{max-width:360px}h1{font-size:22px;margin:0 0 10px}p{color:#a8b3c7;line-height:1.5}button{background:#21c77a;border:0;color:#03110a;padding:13px 18px;border-radius:10px;font-weight:800}</style></head>"
                + "<body><main><h1>No internet connection</h1><p>Please connect to the internet and try again.</p><button onclick=\"window.location.href='" + retryUrl + "'\">Retry</button></main></body></html>";
        webView.loadDataWithBaseURL(startUrl, html, "text/html", "UTF-8", null);
    }

    private int getColorCompat(int colorResource) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getColor(colorResource);
        }
        return getResources().getColor(colorResource);
    }

    private void registerPredictiveBack() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    () -> {
                        if (webView != null && webView.canGoBack()) {
                            webView.goBack();
                        } else {
                            finish();
                        }
                    }
            );
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        String deepLinkUrl = resolveIntentUrl(intent);
        if (deepLinkUrl != null && webView != null) {
            webView.loadUrl(deepLinkUrl);
        }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            super.onBackPressed();
            return;
        }

        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
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
        mainHandler.removeCallbacksAndMessages(null);

        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
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

        @Override
        public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            if (!splashDismissed) {
                updateSplashProgress(8);
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            initialPageFinished = true;
            updateSplashProgress(100);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (request.isForMainFrame() && !showingOfflinePage) {
                showOfflinePage();
            }
        }
    }

    private final class HausaNovelsChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            if (!splashDismissed) {
                int visibleProgress = Math.min(99, Math.max(8, newProgress));
                updateSplashProgress(visibleProgress);
            }
        }

        @Override
        public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams parameters) {
            if (filePathCallback != null) {
                filePathCallback.onReceiveValue(null);
            }

            filePathCallback = callback;

            try {
                startActivityForResult(parameters.createIntent(), FILE_CHOOSER_REQUEST_CODE);
                return true;
            } catch (ActivityNotFoundException ignored) {
                filePathCallback = null;
                Toast.makeText(MainActivity.this, R.string.no_file_picker, Toast.LENGTH_SHORT).show();
                return false;
            }
        }
    }
}
