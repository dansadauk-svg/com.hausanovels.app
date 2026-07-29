package ng.hausanovels.app;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

/** Opens HausaNovels in an external browser without routing the URL back into this app. */
final class BrowserFallback {
    private static final String TAG = "HausaNovelsFallback";

    private static final List<String> PREFERRED_BROWSERS = Arrays.asList(
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.dev",
            "com.chrome.canary",
            "com.sec.android.app.sbrowser",
            "com.microsoft.emmx",
            "org.mozilla.firefox",
            "org.mozilla.firefox_beta"
    );

    private BrowserFallback() {
    }

    static boolean open(Context context, Uri url) {
        Uri safeUrl = sanitize(url);
        Intent probe = new Intent(Intent.ACTION_VIEW, safeUrl);
        probe.addCategory(Intent.CATEGORY_BROWSABLE);

        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> handlers = packageManager.queryIntentActivities(
                probe,
                PackageManager.MATCH_DEFAULT_ONLY
        );

        String ownPackage = context.getPackageName();

        for (String preferredPackage : PREFERRED_BROWSERS) {
            if (!ownPackage.equals(preferredPackage)
                    && containsPackage(handlers, preferredPackage)
                    && launchPackage(context, safeUrl, preferredPackage)) {
                return true;
            }
        }

        for (ResolveInfo handler : handlers) {
            if (handler.activityInfo == null) {
                continue;
            }
            String candidate = handler.activityInfo.packageName;
            if (candidate != null
                    && !ownPackage.equals(candidate)
                    && launchPackage(context, safeUrl, candidate)) {
                return true;
            }
        }

        Log.e(TAG, "No external browser can open HausaNovels");
        Toast.makeText(context, R.string.no_browser_available, Toast.LENGTH_LONG).show();
        return false;
    }

    private static boolean containsPackage(List<ResolveInfo> handlers, String packageName) {
        for (ResolveInfo handler : handlers) {
            if (handler.activityInfo != null
                    && packageName.equals(handler.activityInfo.packageName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean launchPackage(Context context, Uri url, String packageName) {
        Intent browser = new Intent(Intent.ACTION_VIEW, url);
        browser.addCategory(Intent.CATEGORY_BROWSABLE);
        browser.setPackage(packageName);
        browser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            context.startActivity(browser);
            return true;
        } catch (ActivityNotFoundException | SecurityException error) {
            Log.w(TAG, "Browser package failed: " + packageName, error);
            return false;
        } catch (RuntimeException error) {
            Log.e(TAG, "Unexpected browser launch failure: " + packageName, error);
            return false;
        }
    }

    private static Uri sanitize(Uri candidate) {
        if (candidate != null && "https".equalsIgnoreCase(candidate.getScheme())) {
            String host = candidate.getHost();
            if ("hausanovels.ng".equalsIgnoreCase(host)
                    || "www.hausanovels.ng".equalsIgnoreCase(host)) {
                return candidate;
            }
        }
        return Uri.parse("https://hausanovels.ng/?utm_source=twa&twa=1");
    }
}
