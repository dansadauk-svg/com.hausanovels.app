package ng.hausanovels.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Minimal branded launch screen shown only when the app icon is tapped. */
public final class SplashActivity extends Activity {
    private static final String TAG = "HausaNovelsSplash";
    private static final String DEFAULT_URL = "https://hausanovels.ng/?utm_source=twa&twa=1";

    /**
     * A Trusted Web Activity does not expose a page-finished callback to this wrapper.
     * This keeps the branded splash visible while the website is checked and only launches
     * the TWA after the site responds, with a hard timeout so users are never trapped.
     */
    private static final long TICK_MS = 32L;
    private static final long MIN_SPLASH_MS = 3600L;
    private static final long MAX_WAIT_FOR_SITE_MS = 15000L;
    private static final long LAUNCH_AFTER_100_MS = 160L;
    private static final int HOLD_PROGRESS = 98;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();

    private ProgressBar progressBar;
    private TextView progressText;
    private Uri launchUri;
    private long startedAtMs;
    private int progress;
    private boolean launched;
    private boolean siteReady;
    private boolean siteCheckFinished;

    private final Runnable progressTask = new Runnable() {
        @Override
        public void run() {
            if (launched) {
                return;
            }

            long elapsedMs = System.currentTimeMillis() - startedAtMs;
            boolean minimumTimePassed = elapsedMs >= MIN_SPLASH_MS;
            boolean hardTimeoutReached = elapsedMs >= MAX_WAIT_FOR_SITE_MS;
            boolean canLaunch = (siteReady && minimumTimePassed) || hardTimeoutReached;

            if (canLaunch) {
                progress = 100;
                updateProgress();
                handler.postDelayed(SplashActivity.this::launchTwa, LAUNCH_AFTER_100_MS);
                return;
            }

            int targetProgress;
            if (!minimumTimePassed) {
                targetProgress = Math.min(92, (int) ((elapsedMs * 92L) / MIN_SPLASH_MS));
            } else if (siteCheckFinished && !siteReady) {
                targetProgress = HOLD_PROGRESS;
            } else {
                targetProgress = HOLD_PROGRESS;
            }

            if (progress < targetProgress) {
                progress += Math.max(1, Math.min(3, targetProgress - progress));
            }
            if (progress > HOLD_PROGRESS) {
                progress = HOLD_PROGRESS;
            }

            updateProgress();
            handler.postDelayed(this, TICK_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        launchUri = safeWebUri(getIntent() != null ? getIntent().getData() : null);
        startedAtMs = System.currentTimeMillis();

        Window window = getWindow();
        window.setStatusBarColor(Color.parseColor("#050816"));
        window.setNavigationBarColor(Color.parseColor("#050816"));

        setContentView(R.layout.activity_splash);
        progressBar = findViewById(R.id.hn_splash_progress);
        progressText = findViewById(R.id.hn_splash_percentage);
        updateProgress();

        networkExecutor.execute(this::waitForSiteThenReleaseSplash);
        handler.postDelayed(progressTask, 80L);
    }

    @Override
    protected void onDestroy() {
        launched = true;
        handler.removeCallbacksAndMessages(null);
        networkExecutor.shutdownNow();
        super.onDestroy();
    }

    private void updateProgress() {
        if (progressBar != null) {
            progressBar.setProgress(progress);
        }
        if (progressText != null) {
            progressText.setText(progress + "%");
        }
    }

    private void waitForSiteThenReleaseSplash() {
        boolean ready = false;
        long deadlineMs = System.currentTimeMillis() + MAX_WAIT_FOR_SITE_MS;

        while (!launched && System.currentTimeMillis() < deadlineMs) {
            if (isWebsiteReachable(launchUri)) {
                ready = true;
                break;
            }
            try {
                Thread.sleep(650L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        final boolean finalReady = ready;
        handler.post(() -> {
            if (launched) {
                return;
            }
            siteReady = finalReady;
            siteCheckFinished = true;
            handler.removeCallbacks(progressTask);
            handler.post(progressTask);
        });
    }

    private static boolean isWebsiteReachable(Uri uri) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(uri.toString());
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(2600);
            connection.setReadTimeout(2600);
            connection.setInstanceFollowRedirects(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "HausaNovelsAndroid/2.1.4");
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

            int code = connection.getResponseCode();
            return code >= 200 && code < 400;
        } catch (IOException error) {
            Log.w(TAG, "Website not ready yet", error);
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void launchTwa() {
        if (launched) {
            return;
        }
        launched = true;

        Intent intent = new Intent(this, HausaNovelsLauncherActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(launchUri);
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        try {
            startActivity(intent);
        } catch (RuntimeException error) {
            Log.e(TAG, "Unable to start the Trusted Web Activity", error);
            BrowserFallback.open(this, launchUri);
        }

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private static Uri safeWebUri(Uri candidate) {
        if (candidate != null && "https".equalsIgnoreCase(candidate.getScheme())) {
            String host = candidate.getHost();
            if ("hausanovels.ng".equalsIgnoreCase(host)
                    || "www.hausanovels.ng".equalsIgnoreCase(host)) {
                return candidate;
            }
        }
        return Uri.parse(DEFAULT_URL);
    }
}
