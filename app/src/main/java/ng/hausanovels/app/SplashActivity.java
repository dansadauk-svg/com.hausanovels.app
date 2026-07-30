package ng.hausanovels.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Seamless HausaNovels launcher.
 *
 * The older TWA build hands the user from this app to Chrome. That hand-off can show a
 * blank page, a Custom Tab browser bar, or a browser error before the site finishes painting.
 * This build keeps the splash overlay and the WebView in the same Activity, hides the splash
 * only after the first HausaNovels page is visible, and uses the secure system browser only for
 * Google OAuth or other external websites.
 */
public final class SplashActivity extends Activity {
    private static final String TAG = "HausaNovelsApp";
    private static final String HOME_URL = "https://hausanovels.ng/?utm_source=android&app=1";
    private static final String APP_USER_AGENT = "HausaNovelsApp/2.1.4";
    private static final long MIN_SPLASH_MS = 1600L;
    private static final long HIDE_DELAY_MS = 180L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private WebView webView;
    private View splashOverlay;
    private View errorPanel;
    private ProgressBar progressBar;
    private TextView progressText;
    private TextView statusText;
    private long splashStartedAt;
    private boolean firstPageVisible;
    private boolean errorVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        splashStartedAt = System.currentTimeMillis();
        applyWindowColors();
        setContentView(R.layout.activity_app);

        webView = findViewById(R.id.hn_webview);
        splashOverlay = findViewById(R.id.hn_splash_overlay);
        errorPanel = findViewById(R.id.hn_error_panel);
        progressBar = findViewById(R.id.hn_splash_progress);
        progressText = findViewById(R.id.hn_splash_percentage);
        statusText = findViewById(R.id.hn_splash_status);
        findViewById(R.id.hn_retry_button).setOnClickListener(view -> reloadHome());

        configureWebView();
        loadInitialIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        loadInitialIntent(intent);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        super.onBackPressed();
    }

    private void applyWindowColors() {
        Window window = getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#050816")));
        window.setStatusBarColor(Color.parseColor("#050816"));
        window.setNavigationBarColor(Color.parseColor("#050816"));
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void configureWebView() {
        webView.setBackgroundColor(Color.parseColor("#FFF8EF"));
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);

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
                super.onProgressChanged(view, newProgress);
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
                markPageVisible();
                super.onPageFinished(view, url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
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
                showError();
            }
        });
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
