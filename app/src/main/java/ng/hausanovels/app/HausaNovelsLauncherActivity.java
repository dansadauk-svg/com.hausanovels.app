package ng.hausanovels.app;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

/** Trusted Web Activity launcher with a non-recursive external-browser fallback. */
public final class HausaNovelsLauncherActivity
        extends com.google.androidbrowserhelper.trusted.LauncherActivity {

    private static final String TAG = "HausaNovelsTWA";
    private static final Uri DEFAULT_URL =
            Uri.parse("https://hausanovels.ng/?utm_source=twa&twa=1");
    private boolean fallbackStarted;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        applyLightTransitionBackground();
        try {
            super.onCreate(savedInstanceState);
        } catch (RuntimeException error) {
            Log.e(TAG, "Trusted Web Activity launch failed", error);
            openFallback(resolveUrl(getIntent()));
        }
    }

    private void applyLightTransitionBackground() {
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FFF8EF")));
        getWindow().setStatusBarColor(Color.parseColor("#FFF8EF"));
        getWindow().setNavigationBarColor(Color.parseColor("#FFF8EF"));
    }

    @Override
    protected Uri getUrlForIntent(Intent intent) {
        return resolveUrl(intent);
    }

    private Uri resolveUrl(Intent intent) {
        Uri candidate = intent != null ? intent.getData() : null;
        if (candidate != null && "https".equalsIgnoreCase(candidate.getScheme())) {
            String host = candidate.getHost();
            if ("hausanovels.ng".equalsIgnoreCase(host)
                    || "www.hausanovels.ng".equalsIgnoreCase(host)) {
                return candidate;
            }
        }
        return DEFAULT_URL;
    }

    private void openFallback(Uri url) {
        if (fallbackStarted) {
            finish();
            return;
        }
        fallbackStarted = true;
        BrowserFallback.open(this, url);
        finish();
    }
}
