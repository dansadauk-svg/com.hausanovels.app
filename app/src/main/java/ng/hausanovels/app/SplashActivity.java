package ng.hausanovels.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/**
 * Seamless HausaNovels launcher.
 *
 * This WebView shell keeps the splash overlay and website inside the same Activity, supports
 * publisher image uploads, lets readers pull down to refresh, and handles back navigation with
 * both the Android back button and a left-edge swipe gesture.
 */
public final class SplashActivity extends Activity {
    private static final String TAG = "HausaNovelsApp";
    private static final String HOME_URL = "https://hausanovels.ng/?utm_source=android&app=1";
    private static final String APP_USER_AGENT = "HausaNovelsApp/2.1.8";
    private static final int FILE_CHOOSER_REQUEST_CODE = 7001;
    private static final long MIN_SPLASH_MS = 1600L;
    private static final long HIDE_DELAY_MS = 180L;
    private static final long EXIT_CONFIRM_WINDOW_MS = 2000L;
    private static final long SWIPE_MAX_DURATION_MS = 850L;
    private static final float SWIPE_EDGE_WIDTH_DP = 64f;
    private static final float SWIPE_TRIGGER_DISTANCE_DP = 88f;
    private static final float SWIPE_MAX_VERTICAL_DP = 72f;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private WebView webView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View splashOverlay;
    private View errorPanel;
    private ProgressBar progressBar;
    private TextView progressText;
    private TextView statusText;
    private ValueCallback<Uri[]> filePathCallback;
    private long splashStartedAt;
    private boolean firstPageVisible;
    private boolean errorVisible;
    private float touchStartX;
    private float touchStartY;
    private long touchStartTime;
    private long lastExitAttemptTime;
    private boolean edgeSwipeTracking;
    private boolean edgeSwipeHandled;
    private OnBackInvokedCallback backInvokedCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        splashStartedAt = System.currentTimeMillis();
        applyWindowColors();
        setContentView(R.layout.activity_app);

        swipeRefreshLayout = findViewById(R.id.hn_swipe_refresh);
        webView = findViewById(R.id.hn_webview);
        splashOverlay = findViewById(R.id.hn_splash_overlay);
        errorPanel = findViewById(R.id.hn_error_panel);
        progressBar = findViewById(R.id.hn_splash_progress);
        progressText = findViewById(R.id.hn_splash_percentage);
        statusText = findViewById(R.id.hn_splash_status);
        findViewById(R.id.hn_retry_button).setOnClickListener(view -> reloadHome());

        configureSwipeRefresh();
        configureWebView();
        configureSystemBackNavigation();
        loadInitialIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        loadInitialIntent(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            ValueCallback<Uri[]> callback = filePathCallback;
            filePathCallback = null;
            if (callback != null) {
                Uri[] result = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
                callback.onReceiveValue(result);
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        unregisterSystemBackNavigation();
        handler.removeCallbacksAndMessages(null);
        if (filePathCallback != null) {
            filePathCallback.onReceiveValue(null);
            filePathCallback = null;
        }
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        handleBackNavigation();
    }

    private void configureSystemBackNavigation() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }

        backInvokedCallback = this::handleBackNavigation;
        getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                backInvokedCallback
        );
    }

    private void unregisterSystemBackNavigation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && backInvokedCallback != null) {
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backInvokedCallback);
            backInvokedCallback = null;
        }
    }

    private void handleBackNavigation() {
        if (webView != null) {
            if (errorVisible && errorPanel != null && errorPanel.getVisibility() == View.VISIBLE) {
                errorVisible = false;
                errorPanel.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
                String currentUrl = webView.getUrl();
                if (currentUrl == null || currentUrl.isEmpty()) {
                    webView.loadUrl(HOME_URL);
                } else {
                    webView.reload();
                }
                resetExitConfirmation();
                return;
            }

            if (webView.canGoBack()) {
                webView.goBack();
                resetExitConfirmation();
                return;
            }

            String currentUrl = webView.getUrl();
            if (!isAppHomeUrl(currentUrl)) {
                webView.loadUrl(HOME_URL);
                resetExitConfirmation();
                return;
            }
        }

        long now = SystemClock.elapsedRealtime();
        if (now - lastExitAttemptTime <= EXIT_CONFIRM_WINDOW_MS) {
            finish();
            return;
        }

        lastExitAttemptTime = now;
        Toast.makeText(this, R.string.back_again_to_exit, Toast.LENGTH_SHORT).show();
    }

    private void resetExitConfirmation() {
        lastExitAttemptTime = 0L;
    }

    private void applyWindowColors() {
        Window window = getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FFF8EF")));
        window.setStatusBarColor(Color.parseColor("#FFF8EF"));
        window.setNavigationBarColor(Color.parseColor("#FFF8EF"));
    }

    private void configureSwipeRefresh() {
        if (swipeRefreshLayout == null) {
            return;
        }
        swipeRefreshLayout.setColorSchemeColors(Color.parseColor("#0F7B45"), Color.parseColor("#D79D2A"));
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(Color.parseColor("#FFF8EF"));
        swipeRefreshLayout.setOnChildScrollUpCallback(
                (parent, child) -> webView != null && webView.canScrollVertically(-1)
        );
        swipeRefreshLayout.setDistanceToTriggerSync((int) dpToPx(96f));
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (webView != null) {
                webView.reload();
            } else {
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    private void configureWebView() {
        webView.setBackgroundColor(Color.parseColor("#FFF8EF"));
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setOnTouchListener(this::handleSwipeBackGesture);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setLoadWithOverviewMode(false);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);
        settings.setTextZoom(100);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        String currentUa = settings.getUserAgentString();
        if (currentUa == null || !currentUa.contains(APP_USER_AGENT)) {
            settings.setUserAgentString((currentUa == null ? "" : currentUa) + " " + APP_USER_AGENT);
        }

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);
        cookieManager.setCookie("https://hausanovels.ng", "hn_android_app=1; Path=/; Max-Age=31536000; Secure; SameSite=Lax");
        cookieManager.setCookie("https://www.hausanovels.ng", "hn_android_app=1; Path=/; Max-Age=31536000; Secure; SameSite=Lax");
        cookieManager.flush();

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                updateProgress(newProgress);
                if (newProgress >= 100 && swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                super.onProgressChanged(view, newProgress);
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePath, FileChooserParams fileChooserParams) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = filePath;

                Intent chooserIntent;
                try {
                    chooserIntent = fileChooserParams != null ? fileChooserParams.createIntent() : null;
                } catch (Exception ignored) {
                    chooserIntent = null;
                }

                if (chooserIntent == null) {
                    chooserIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    chooserIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    chooserIntent.setType("image/*");
                    chooserIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
                }

                try {
                    startActivityForResult(chooserIntent, FILE_CHOOSER_REQUEST_CODE);
                    return true;
                } catch (ActivityNotFoundException error) {
                    Intent fallback = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    fallback.setType("image/*");
                    try {
                        startActivityForResult(fallback, FILE_CHOOSER_REQUEST_CODE);
                        return true;
                    } catch (ActivityNotFoundException fallbackError) {
                        if (filePathCallback != null) {
                            filePathCallback.onReceiveValue(null);
                            filePathCallback = null;
                        }
                        return false;
                    }
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(request != null ? request.getUrl() : null);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(url == null ? null : Uri.parse(url));
            }

            @Override
            public void onPageCommitVisible(WebView view, String url) {
                markPageVisible();
                super.onPageCommitVisible(view, url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                markPageVisible();
                super.onPageFinished(view, url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                if (request != null && request.isForMainFrame()) {
                    showError();
                }
                super.onReceivedError(view, request, error);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                if (handler != null) {
                    handler.cancel();
                }
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                showError();
            }
        });
    }

    private boolean handleSwipeBackGesture(View view, MotionEvent event) {
        if (event == null || webView == null) {
            return false;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = event.getX();
                touchStartY = event.getY();
                touchStartTime = SystemClock.elapsedRealtime();
                edgeSwipeTracking = touchStartX <= dpToPx(SWIPE_EDGE_WIDTH_DP);
                edgeSwipeHandled = false;
                return false;

            case MotionEvent.ACTION_MOVE:
                if (!edgeSwipeTracking || edgeSwipeHandled) {
                    return edgeSwipeHandled;
                }

                float moveX = event.getX() - touchStartX;
                float moveY = Math.abs(event.getY() - touchStartY);
                if (moveX < 0f || moveY > dpToPx(SWIPE_MAX_VERTICAL_DP)) {
                    edgeSwipeTracking = false;
                    return false;
                }

                if (moveX >= dpToPx(SWIPE_TRIGGER_DISTANCE_DP) && moveX > moveY * 1.35f) {
                    edgeSwipeHandled = true;
                    handleBackNavigation();
                    return true;
                }
                return false;

            case MotionEvent.ACTION_UP:
                if (edgeSwipeHandled) {
                    edgeSwipeTracking = false;
                    return true;
                }

                float dx = event.getX() - touchStartX;
                float dy = Math.abs(event.getY() - touchStartY);
                long elapsed = SystemClock.elapsedRealtime() - touchStartTime;
                boolean validSwipe = edgeSwipeTracking
                        && dx >= dpToPx(SWIPE_TRIGGER_DISTANCE_DP)
                        && dx > dy * 1.35f
                        && dy <= dpToPx(SWIPE_MAX_VERTICAL_DP)
                        && elapsed <= SWIPE_MAX_DURATION_MS;
                edgeSwipeTracking = false;
                if (validSwipe) {
                    edgeSwipeHandled = true;
                    handleBackNavigation();
                    return true;
                }
                return false;

            case MotionEvent.ACTION_CANCEL:
                edgeSwipeTracking = false;
                edgeSwipeHandled = false;
                return false;

            default:
                return edgeSwipeHandled;
        }
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private void loadInitialIntent(Intent intent) {
        showSplash();
        Uri uri = intent != null ? intent.getData() : null;

        if (uri != null && "hausanovels".equalsIgnoreCase(uri.getScheme())) {
            loadUrl(appLoginUrl(uri));
            return;
        }

        if (isSafeHausaUrl(uri)) {
            loadUrl(ensureAppMode(uri).toString());
            return;
        }

        loadUrl(HOME_URL);
    }

    private void reloadHome() {
        showSplash();
        loadUrl(HOME_URL + "&reload=" + System.currentTimeMillis());
    }

    private void loadUrl(String url) {
        errorVisible = false;
        firstPageVisible = false;
        errorPanel.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
        updateProgress(0);
        if (statusText != null) {
            statusText.setText(R.string.loading_preparing);
        }
        webView.loadUrl(url);
    }

    private boolean handleUrl(Uri uri) {
        if (uri == null) {
            return false;
        }

        String scheme = uri.getScheme();
        if (scheme == null) {
            return false;
        }

        if ("hausanovels".equalsIgnoreCase(scheme)) {
            showSplash();
            loadUrl(appLoginUrl(uri));
            return true;
        }

        if ("intent".equalsIgnoreCase(scheme)) {
            openExternal(uri.toString());
            return true;
        }

        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            openExternal(uri.toString());
            return true;
        }

        if (isGoogleOrExternalAuth(uri)) {
            openExternal(uri.toString());
            return true;
        }

        if (isSafeHausaUrl(uri)) {
            return false;
        }

        openExternal(uri.toString());
        return true;
    }

    private void markPageVisible() {
        if (firstPageVisible || errorVisible) {
            return;
        }
        firstPageVisible = true;
        long elapsed = System.currentTimeMillis() - splashStartedAt;
        long delay = Math.max(HIDE_DELAY_MS, MIN_SPLASH_MS - elapsed);
        handler.postDelayed(this::hideSplash, delay);
    }

    private void showSplash() {
        splashStartedAt = System.currentTimeMillis();
        firstPageVisible = false;
        errorVisible = false;
        if (splashOverlay != null) {
            splashOverlay.setAlpha(1f);
            splashOverlay.setVisibility(View.VISIBLE);
        }
        if (errorPanel != null) {
            errorPanel.setVisibility(View.GONE);
        }
        updateProgress(0);
    }

    private void hideSplash() {
        if (splashOverlay == null || splashOverlay.getVisibility() != View.VISIBLE) {
            return;
        }
        updateProgress(100);
        splashOverlay.animate()
                .alpha(0f)
                .setDuration(180L)
                .withEndAction(() -> splashOverlay.setVisibility(View.GONE))
                .start();
    }

    private void showError() {
        errorVisible = true;
        handler.removeCallbacksAndMessages(null);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
        if (splashOverlay != null) {
            splashOverlay.setVisibility(View.GONE);
        }
        if (webView != null) {
            webView.stopLoading();
        }
        if (errorPanel != null) {
            errorPanel.setVisibility(View.VISIBLE);
        }
    }

    private void updateProgress(int progress) {
        int safeProgress = Math.max(0, Math.min(100, progress));
        if (progressBar != null) {
            progressBar.setProgress(safeProgress);
        }
        if (progressText != null) {
            progressText.setText(safeProgress + "%");
        }
    }

    private static boolean isSafeHausaUrl(Uri uri) {
        if (uri == null || !"https".equalsIgnoreCase(uri.getScheme())) {
            return false;
        }
        String host = uri.getHost();
        return "hausanovels.ng".equalsIgnoreCase(host) || "www.hausanovels.ng".equalsIgnoreCase(host);
    }

    private static boolean isAppHomeUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isEmpty()) {
            return true;
        }

        Uri uri = Uri.parse(rawUrl);
        if (!isSafeHausaUrl(uri)) {
            return false;
        }

        String path = uri.getPath();
        if (path != null && !path.isEmpty() && !"/".equals(path)) {
            return false;
        }

        String fragment = uri.getFragment();
        if (fragment != null && !fragment.isEmpty()) {
            return false;
        }

        for (String parameter : uri.getQueryParameterNames()) {
            if (!"app".equals(parameter)
                    && !"utm_source".equals(parameter)
                    && !"reload".equals(parameter)) {
                return false;
            }
        }
        return true;
    }

    private static Uri ensureAppMode(Uri uri) {
        Uri.Builder builder = uri.buildUpon();
        if (uri.getQueryParameter("app") == null) {
            builder.appendQueryParameter("app", "1");
        }
        if (uri.getQueryParameter("utm_source") == null) {
            builder.appendQueryParameter("utm_source", "android");
        }
        return builder.build();
    }

    private static boolean isGoogleOrExternalAuth(Uri uri) {
        String host = uri.getHost();
        if (host == null) {
            return false;
        }
        String lowerHost = host.toLowerCase();
        return lowerHost.equals("accounts.google.com")
                || lowerHost.endsWith(".accounts.google.com")
                || lowerHost.equals("oauth2.googleapis.com")
                || lowerHost.endsWith("googleusercontent.com")
                || lowerHost.equals("ssl.gstatic.com");
    }

    private static String appLoginUrl(Uri callback) {
        String token = callback.getQueryParameter("token");
        String redirect = callback.getQueryParameter("redirect");
        String url = callback.getQueryParameter("url");

        if (token != null && token.matches("[a-fA-F0-9]{64}")) {
            Uri.Builder builder = Uri.parse("https://hausanovels.ng/").buildUpon()
                    .appendQueryParameter("hn_google_app_login", "1")
                    .appendQueryParameter("token", token);
            if (redirect != null && !redirect.isEmpty()) {
                builder.appendQueryParameter("redirect", redirect);
            }
            builder.appendQueryParameter("app", "1");
            return builder.build().toString();
        }

        Uri parsed = url == null ? null : Uri.parse(url);
        if (isSafeHausaUrl(parsed)) {
            return ensureAppMode(parsed).toString();
        }

        return HOME_URL;
    }

    private void openExternal(String rawUrl) {
        try {
            Intent intent;
            if (rawUrl.startsWith("intent://")) {
                intent = Intent.parseUri(rawUrl, Intent.URI_INTENT_SCHEME);
                if (intent.getPackage() == null) {
                    intent.setPackage("com.android.chrome");
                }
            } else {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(rawUrl));
            }
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            startActivity(intent);
        } catch (Exception error) {
            Log.w(TAG, "Unable to open external URL", error);
            try {
                Intent fallback = new Intent(Intent.ACTION_VIEW, Uri.parse(rawUrl));
                fallback.addCategory(Intent.CATEGORY_BROWSABLE);
                startActivity(fallback);
            } catch (ActivityNotFoundException ignored) {
                showError();
            }
        }
    }
}
