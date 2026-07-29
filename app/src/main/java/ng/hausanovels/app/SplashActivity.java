package ng.hausanovels.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Minimal branded launch screen shown only when the user taps the app icon.
 * The percentage represents app launch preparation, not network page-load progress.
 */
public final class SplashActivity extends Activity {
    private static final String DEFAULT_URL = "https://hausanovels.ng/?utm_source=twa&twa=1";
    private static final long TICK_MS = 22L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private ProgressBar progressBar;
    private TextView progressText;
    private int progress = 0;
    private boolean launched = false;

    private final Runnable progressTask = new Runnable() {
        @Override
        public void run() {
            if (launched) {
                return;
            }

            progress += progress < 70 ? 3 : (progress < 92 ? 2 : 1);
            if (progress >= 100) {
                progress = 100;
                updateProgress();
                handler.postDelayed(SplashActivity.this::launchTwa, 110L);
                return;
            }

            updateProgress();
            handler.postDelayed(this, TICK_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(Color.parseColor("#050816"));
        window.setNavigationBarColor(Color.parseColor("#050816"));

        setContentView(R.layout.activity_splash);
        progressBar = findViewById(R.id.hn_splash_progress);
        progressText = findViewById(R.id.hn_splash_percentage);
        updateProgress();
        handler.postDelayed(progressTask, 80L);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void updateProgress() {
        progressBar.setProgress(progress);
        progressText.setText(progress + "%");
    }

    private void launchTwa() {
        if (launched) {
            return;
        }
        launched = true;

        Uri launchUri = safeWebUri(getIntent() != null ? getIntent().getData() : null);
        Intent intent = new Intent(this, HausaNovelsLauncherActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(launchUri);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private static Uri safeWebUri(Uri candidate) {
        if (candidate != null
                && "https".equalsIgnoreCase(candidate.getScheme())
                && "hausanovels.ng".equalsIgnoreCase(candidate.getHost())) {
            return candidate;
        }
        return Uri.parse(DEFAULT_URL);
    }
}
