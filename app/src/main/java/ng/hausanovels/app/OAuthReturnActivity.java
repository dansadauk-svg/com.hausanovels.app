package ng.hausanovels.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/**
 * Receives the package-restricted OAuth return deep link after Google completes sign-in in Chrome.
 * The WordPress authentication cookie already exists in Chrome's cookie jar, which is shared by TWA.
 */
public final class OAuthReturnActivity extends Activity {
    private static final Uri DEFAULT_ACCOUNT_URL =
            Uri.parse("https://hausanovels.ng/account/?google_login=success&twa=1");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        reopenTwa();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        reopenTwa();
    }

    private void reopenTwa() {
        Uri target = extractSafeTarget(getIntent() != null ? getIntent().getData() : null);
        Intent launch = new Intent(this, HausaNovelsLauncherActivity.class);
        launch.setAction(Intent.ACTION_VIEW);
        launch.setData(target);
        launch.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(launch);
        finish();
    }

    private static Uri extractSafeTarget(Uri callback) {
        if (callback == null) {
            return DEFAULT_ACCOUNT_URL;
        }

        String rawUrl = callback.getQueryParameter("url");
        if (rawUrl == null || rawUrl.isEmpty()) {
            return DEFAULT_ACCOUNT_URL;
        }

        Uri parsed = Uri.parse(rawUrl);
        if (!"https".equalsIgnoreCase(parsed.getScheme())) {
            return DEFAULT_ACCOUNT_URL;
        }

        String host = parsed.getHost();
        if (host == null || !"hausanovels.ng".equalsIgnoreCase(host)) {
            return DEFAULT_ACCOUNT_URL;
        }

        return parsed;
    }
}
